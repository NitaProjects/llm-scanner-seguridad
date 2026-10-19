#!/bin/sh
set -eu
fail() { echo 'PRODUCTION_INGRESS_CONFIGURATION_INVALID' >&2; exit 1; }
printf '%s' "${PUBLIC_HOSTNAME:-}" | grep -Eq '^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$' || fail
case "${PUBLIC_ORIGIN:-}" in
    "https://${PUBLIC_HOSTNAME}") ;;
    "https://${PUBLIC_HOSTNAME}":*)
        port=${PUBLIC_ORIGIN##*:}
        printf '%s' "$port" | grep -Eq '^[0-9]{1,5}$' && test "$port" -ge 1 && test "$port" -le 65535 || fail ;;
    *) fail ;;
esac
for file in /run/auth/htpasswd /run/tls/active/fullchain.pem /run/tls/active/privkey.pem; do
    test -f "$file" && test -s "$file" && test -r "$file" || fail
done
envsubst '${PUBLIC_HOSTNAME} ${PUBLIC_ORIGIN}' < /opt/nginx.conf.template > /tmp/nginx.conf
envsubst '${PUBLIC_HOSTNAME}' < /opt/proxy.conf.template > /tmp/proxy.conf
sed -i 's|include /opt/proxy.conf;|include /tmp/proxy.conf;|g' /tmp/nginx.conf
nginx -t -c /tmp/nginx.conf >/dev/null 2>&1 || fail
attempt=0
until /opt/readiness.sh; do
    attempt=$((attempt + 1)); test "$attempt" -lt 24 || fail
    sleep 5
done
exec nginx -c /tmp/nginx.conf -g 'daemon off;'
