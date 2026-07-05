#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

ENV_FILE="${1:-.env}"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

warn() {
  printf 'WARN: %s\n' "$*" >&2
}

[ -f "${ENV_FILE}" ] || fail "No existe ${ENV_FILE}. Copia .env.example a .env únicamente dentro del VPS."
[ -f docker-compose.yml ] || fail "No se encontró docker-compose.yml."
[ -f backend/Dockerfile ] || fail "No se encontró backend/Dockerfile."
[ -f frontend/Dockerfile ] || fail "No se encontró frontend/Dockerfile."
[ -f deploy/nginx/templates/blueberrytrace-http.conf.template ] || fail "No se encontró la plantilla Nginx HTTP."
[ -f .mvn/wrapper/maven-wrapper.properties ] || fail "No se encontró la configuración del Maven Wrapper."

read_env_value() {
  local key="$1"
  local line
  line="$(grep -E "^${key}=" "${ENV_FILE}" | tail -n 1 || true)"
  [ -n "${line}" ] || return 1
  line="${line#*=}"
  line="${line%$'\r'}"
  line="${line#\"}"
  line="${line%\"}"
  line="${line#\'}"
  line="${line%\'}"
  printf '%s' "${line}"
}

require_value() {
  local key="$1"
  local value
  value="$(read_env_value "${key}" || true)"
  [ -n "${value}" ] || fail "Falta ${key} en ${ENV_FILE}."
  case "${value}" in
    *CHANGE_ME*|*example.com*|*YOUR_*|*REEMPLAZAR*)
      fail "${key} todavía tiene un valor de ejemplo en ${ENV_FILE}."
      ;;
  esac
}

for key in APP_DOMAIN MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD MYSQL_ROOT_PASSWORD DB_USERNAME DB_PASSWORD CORS_ALLOWED_ORIGINS; do
  require_value "${key}"
done

APP_DOMAIN="$(read_env_value APP_DOMAIN)"
CORS_ALLOWED_ORIGINS="$(read_env_value CORS_ALLOWED_ORIGINS)"
MYSQL_USER="$(read_env_value MYSQL_USER)"
DB_USERNAME="$(read_env_value DB_USERNAME)"
MYSQL_PASSWORD="$(read_env_value MYSQL_PASSWORD)"
DB_PASSWORD="$(read_env_value DB_PASSWORD)"
BOOTSTRAP_ADMIN_ENABLED="$(read_env_value BOOTSTRAP_ADMIN_ENABLED || true)"

case "${APP_DOMAIN}" in
  http://*|https://*|*/*|*' '*) fail "APP_DOMAIN debe contener solo el dominio, sin protocolo ni rutas." ;;
esac

case ",${CORS_ALLOWED_ORIGINS}," in
  *",https://${APP_DOMAIN},"*) ;;
  *) fail "CORS_ALLOWED_ORIGINS debe incluir https://${APP_DOMAIN}." ;;
esac

[ "${MYSQL_USER}" = "${DB_USERNAME}" ] || fail "DB_USERNAME debe coincidir con MYSQL_USER."
[ "${MYSQL_PASSWORD}" = "${DB_PASSWORD}" ] || fail "DB_PASSWORD debe coincidir con MYSQL_PASSWORD."

if [ "${BOOTSTRAP_ADMIN_ENABLED}" = "true" ]; then
  for key in BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_ADMIN_PASSWORD BOOTSTRAP_ADMIN_FULL_NAME; do
    require_value "${key}"
  done
  warn "El bootstrap administrativo está habilitado. Desactívalo y elimina sus secretos después del primer inicio."
fi

if ! command -v docker >/dev/null 2>&1; then
  fail "Docker no está instalado o no está disponible para el usuario actual."
fi

docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 no está disponible."
docker compose --env-file "${ENV_FILE}" config -q

printf 'Preflight VPS correcto: Docker Compose puede resolver la configuración de BlueberryTrace.\n'
