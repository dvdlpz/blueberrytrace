#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

OUTPUT_DIR="artifacts/pterodactyl"
JAR_OUTPUT="${OUTPUT_DIR}/blueberrytrace.jar"
CONFIG_OUTPUT="${OUTPUT_DIR}/config/application-pterodactyl.properties.example"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

command -v npm >/dev/null 2>&1 || fail "Node.js y npm son obligatorios para compilar React."
command -v java >/dev/null 2>&1 || fail "Java 21 es obligatorio para compilar el backend."

rm -rf frontend/dist "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}/config"

printf '1/4 Instalando dependencias del frontend…\n'
npm ci

printf '2/4 Compilando React/Vite para el mismo origen…\n'
npm run frontend:build
[ -f frontend/dist/index.html ] || fail "Vite no generó frontend/dist/index.html."

printf '3/4 Ejecutando pruebas y empaquetando el JAR Java 21…\n'
bash scripts/maven.sh -pl backend -am -Ppterodactyl-bundle clean verify

mapfile -t BUILT_JARS < <(find backend/target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print | sort)
[ "${#BUILT_JARS[@]}" -eq 1 ] || fail "Se esperaba exactamente un JAR ejecutable en backend/target; se encontraron ${#BUILT_JARS[@]}."
cp "${BUILT_JARS[0]}" "${JAR_OUTPUT}"
cp deploy/pterodactyl/application-pterodactyl.properties.example "${CONFIG_OUTPUT}"

printf '4/4 Verificando el paquete de despliegue…\n'
bash scripts/preflight-pterodactyl.sh "${JAR_OUTPUT}"

printf '\nPaquete Pterodactyl listo:\n'
printf '  - %s\n' "${JAR_OUTPUT}"
printf '  - %s\n' "${CONFIG_OUTPUT}"
printf '\nSube solo esos archivos al panel después de que el servidor use Java 21 y tenga MySQL habilitado.\n'
