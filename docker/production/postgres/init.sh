#!/bin/sh
set -eu
test -s /run/secrets/app-password || { echo 'DATABASE_BOOTSTRAP_INVALID' >&2; exit 1; }
SCANNER_APP_PASSWORD=$(cat /run/secrets/app-password)
test "${#SCANNER_APP_PASSWORD}" -ge 24 || exit 1
test "$SCANNER_APP_PASSWORD" != "$POSTGRES_PASSWORD" || exit 1
export SCANNER_APP_PASSWORD
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<'SQL'
\getenv app_password SCANNER_APP_PASSWORD
CREATE ROLE scanner_app LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
CREATE DATABASE llm_scanner_seguridad_pro OWNER scanner_app;
REVOKE ALL ON DATABASE llm_scanner_seguridad_pro FROM PUBLIC;
\connect llm_scanner_seguridad_pro
ALTER SCHEMA public OWNER TO scanner_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SQL
unset SCANNER_APP_PASSWORD
