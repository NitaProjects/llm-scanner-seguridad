#!/bin/sh
set -eu
fail() { echo 'PRODUCTION_BACKEND_CONFIGURATION_INVALID' >&2; exit 1; }
test "${SPRING_PROFILES_ACTIVE:-}" = pro || fail
test "${DB_URL:-}" = jdbc:postgresql://postgres:5432/llm_scanner_seguridad_pro || fail
test "${DB_USERNAME:-}" = scanner_app || fail
test "${EVALUATOR_EXPORT_ENABLED:-false}" = false || fail
test "${TARGET_LOCAL_DEMO_ENABLED:-false}" = false || fail
test -z "${TARGET_ALLOWED_ORIGINS:-}${TARGET_LOCAL_DEMO_ORIGINS:-}${EVALUATOR_ALLOWED_SCENARIOS:-}${EVALUATION_SCENARIO_FILE:-}" || fail
test "${EVALUATOR_API_KEY+x}" != x || fail
for file in /run/secrets/db-password /run/secrets/target-aes; do test -s "$file" && test -r "$file" || fail; done
DB_PASSWORD=$(cat /run/secrets/db-password)
TARGET_CREDENTIAL_ENCRYPTION_KEY=$(cat /run/secrets/target-aes)
test "${#DB_PASSWORD}" -ge 24 || fail
test "${#TARGET_CREDENTIAL_ENCRYPTION_KEY}" -eq 44 || fail
test "$(printf '%s' "$TARGET_CREDENTIAL_ENCRYPTION_KEY" | base64 -d 2>/dev/null | wc -c)" -eq 32 || fail
export DB_PASSWORD TARGET_CREDENTIAL_ENCRYPTION_KEY
exec java -jar /app/scanner.jar
