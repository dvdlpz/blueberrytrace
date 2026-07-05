#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [ ! -f "${ROOT_DIR}/.mvn/wrapper/maven-wrapper.properties" ] && [ -f "${ROOT_DIR}/backend/.mvn/wrapper/maven-wrapper.properties" ]; then
  mkdir -p "${ROOT_DIR}/.mvn/wrapper"
  cp "${ROOT_DIR}/backend/.mvn/wrapper/maven-wrapper.properties" "${ROOT_DIR}/.mvn/wrapper/maven-wrapper.properties"
fi

chmod +x "${ROOT_DIR}/mvnw" 2>/dev/null || true

if command -v mvn >/dev/null 2>&1; then
  echo "Usando Maven instalado en el sistema: $(mvn -v | head -n 1)"
  exec mvn "$@"
fi

if [ -f "${ROOT_DIR}/mvnw" ]; then
  echo "Maven del sistema no encontrado. Usando Maven Wrapper."
  exec bash "${ROOT_DIR}/mvnw" "$@"
fi

echo "No se encontró Maven ni Maven Wrapper."
echo "Instala Maven o restaura ./mvnw y .mvn/wrapper/maven-wrapper.properties."
exit 1
