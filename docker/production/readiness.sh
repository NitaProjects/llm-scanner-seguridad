#!/bin/sh
set -eu
headers=$(mktemp /tmp/readiness.XXXXXX)
trap 'rm -f "$headers"' EXIT
wget -S -T 5 -t 1 -O /dev/null http://backend:8080/api/projects 2>"$headers" || exit 1
grep -Eq 'HTTP/1\.[01] 200' "$headers" && grep -Eiq 'Content-Type: application/json' "$headers"
