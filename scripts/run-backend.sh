#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${BACKEND_ENV_FILE:-${ROOT_DIR}/backend/.env}"
cd "${ROOT_DIR}"

# Carga un archivo .env simple sin ejecutarlo como Bash. Esto permite URL JDBC con
# caracteres como & y valores con espacios, sin expansión de comandos ni variables.
load_env_file() {
  local env_file="$1"
  local raw_line line key value

  [ -f "${env_file}" ] || return 0

  while IFS= read -r raw_line || [ -n "${raw_line}" ]; do
    line="${raw_line%$'\r'}"

    # Ignora comentarios y líneas vacías.
    [[ "${line}" =~ ^[[:space:]]*$ ]] && continue
    [[ "${line}" =~ ^[[:space:]]*# ]] && continue

    if [[ ! "${line}" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      echo "Configuración inválida en ${env_file}: ${line}" >&2
      exit 1
    fi

    key="${BASH_REMATCH[1]}"
    value="${BASH_REMATCH[2]}"

    # Se permiten comillas simples o dobles para que el archivo siga siendo cómodo
    # de editar; no se evalúan escapes, comandos ni sustituciones de shell.
    if [[ ${#value} -ge 2 ]]; then
      if [[ "${value:0:1}" == '"' && "${value: -1}" == '"' ]] || \
         [[ "${value:0:1}" == "'" && "${value: -1}" == "'" ]]; then
        value="${value:1:${#value}-2}"
      fi
    fi

    # El archivo local es la fuente de verdad en desarrollo. Esto evita conservar
    # valores obsoletos tras un antiguo `source backend/.env` en la misma terminal.
    # Para una ejecución puntual con variables externas, define BACKEND_ENV_FILE=/ruta/.env.
    export "${key}=${value}"
  done < "${env_file}"
}

load_env_file "${ENV_FILE}"
PORT="${SERVER_PORT:-8080}"

if command -v ss >/dev/null 2>&1 && ss -ltn "sport = :${PORT}" | grep -q ":${PORT}"; then
  echo "El puerto ${PORT} ya está en uso."
  echo "Opciones:"
  echo "  1) Liberar puerto: npm run backend:kill"
  echo "  2) Usar puerto alternativo: SERVER_PORT=8081 npm run backend:run"
  echo "  3) Ver proceso actual: npm run backend:port"
  exit 1
fi

if [ ! -f "${ROOT_DIR}/mvnw" ]; then
  echo "No se encontró mvnw en la raíz del proyecto."
  echo "Abre la carpeta blueberrytrace completa o restaura el Maven Wrapper."
  exit 1
fi

if [ ! -x "${ROOT_DIR}/mvnw" ]; then
  chmod +x "${ROOT_DIR}/mvnw" 2>/dev/null || true
fi

for helper in "${ROOT_DIR}"/scripts/*.sh; do
  [ -f "${helper}" ] && chmod +x "${helper}" 2>/dev/null || true
done

echo "Iniciando BlueberryTrace backend"
echo "  Puerto: ${PORT}"
echo "  Proyecto: ${ROOT_DIR}"
if [ -f "${ENV_FILE}" ]; then
  echo "  Variables locales: backend/.env"
fi

# Se usa un wrapper seguro que prefiere Maven del sistema y cae a Maven Wrapper.
bash "${ROOT_DIR}/scripts/maven.sh" -pl backend spring-boot:run
