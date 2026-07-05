#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

JAR_PATH="${1:-artifacts/pterodactyl/blueberrytrace.jar}"
CONFIG_TEMPLATE="deploy/pterodactyl/application-pterodactyl.properties.example"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "No se encontró el comando requerido: $1"
}

require_command java
require_command jar

JAVA_VERSION_RAW="$(java -version 2>&1 | awk -F'\"' '/version/ {print $2; exit}')"
JAVA_MAJOR="${JAVA_VERSION_RAW%%.*}"
if [[ "${JAVA_MAJOR}" == "1" ]]; then
  JAVA_MAJOR="$(printf '%s' "${JAVA_VERSION_RAW}" | cut -d. -f2)"
fi
[[ "${JAVA_MAJOR}" =~ ^[0-9]+$ ]] || fail "No se pudo detectar la versión de Java."
(( JAVA_MAJOR >= 21 )) || fail "Java 21 o superior es obligatorio; se detectó Java ${JAVA_VERSION_RAW}."

[ -f "${CONFIG_TEMPLATE}" ] || fail "No se encontró ${CONFIG_TEMPLATE}."
[ -f "${JAR_PATH}" ] || fail "No se encontró el JAR ${JAR_PATH}. Ejecuta npm run package:pterodactyl primero."

jar tf "${JAR_PATH}" | grep -qx 'BOOT-INF/classes/static/index.html' \
  || fail "El JAR no contiene frontend/dist/index.html. No lo subas a Pterodactyl."
jar tf "${JAR_PATH}" | grep -q '^BOOT-INF/classes/static/assets/' \
  || fail "El JAR no contiene los assets compilados de React."
jar tf "${JAR_PATH}" | grep -qx 'BOOT-INF/classes/application-pterodactyl.properties' \
  || fail "El JAR no contiene el perfil application-pterodactyl.properties."

if grep -R --binary-files=without-match -E 'https?://(localhost|127\.0\.0\.1)' frontend/dist >/dev/null 2>&1; then
  fail "El frontend compilado contiene una URL local. Debe usar /api/v1 para el despliegue integrado."
fi

printf 'Preflight Pterodactyl correcto:\n'
printf '  - Java %s\n' "${JAVA_VERSION_RAW}"
printf '  - JAR: %s\n' "${JAR_PATH}"
printf '  - React integrado como recursos estáticos\n'
printf '  - Perfil Pterodactyl incluido\n'
