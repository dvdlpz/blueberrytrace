#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

PORT="${SERVER_PORT:-8080}"

print_section() {
  echo ""
  echo "== $1 =="
}

print_section "BlueberryTrace workspace"
echo "Ruta: ${ROOT_DIR}"
[ -f pom.xml ] && echo "pom.xml raíz: OK" || echo "pom.xml raíz: NO ENCONTRADO"
[ -f .mvn/wrapper/maven-wrapper.properties ] && echo "Maven Wrapper raíz: OK" || echo "Maven Wrapper raíz: NO ENCONTRADO"
[ -d backend/src/main/java ] && echo "backend/src/main/java: OK" || echo "backend/src/main/java: NO ENCONTRADO"
[ -d frontend/src ] && echo "frontend/src: OK" || echo "frontend/src: NO ENCONTRADO"

print_section "Herramientas"
if command -v java >/dev/null 2>&1; then
  java -version 2>&1 | head -n 1
else
  echo "Java: NO ENCONTRADO"
fi

if command -v node >/dev/null 2>&1; then
  echo "Node: $(node -v)"
else
  echo "Node: NO ENCONTRADO"
fi

if command -v npm >/dev/null 2>&1; then
  echo "npm: $(npm -v)"
else
  echo "npm: NO ENCONTRADO"
fi

print_section "Permisos"
if [ -x mvnw ]; then
  echo "mvnw ejecutable: OK"
else
  echo "mvnw ejecutable: NO. Ejecuta: npm run setup:permissions"
fi

for script in scripts/*.sh; do
  [ -x "${script}" ] && echo "${script}: OK" || echo "${script}: sin permiso de ejecución"
done

print_section "Puerto backend"
if command -v ss >/dev/null 2>&1; then
  if ss -ltn "sport = :${PORT}" | grep -q ":${PORT}"; then
    echo "Puerto ${PORT}: OCUPADO"
    ss -ltnp "sport = :${PORT}" || true
  else
    echo "Puerto ${PORT}: libre"
  fi
else
  echo "No se puede revisar puerto: comando ss no encontrado."
fi

print_section "Configuración"
if [ -f backend/src/main/resources/application.properties ]; then
  grep -E "^(server.port|spring.datasource.url|spring.datasource.username|spring.jpa.database-platform|spring.profiles.active)" backend/src/main/resources/application.properties || true
else
  echo "application.properties no encontrado."
fi

if [ -f frontend/.env ]; then
  echo "frontend/.env: OK"
else
  echo "frontend/.env: no existe. Puedes crear uno con: cp frontend/.env.example frontend/.env"
fi

print_section "Comandos recomendados"
echo "Backend:  npm run backend:run"
echo "Frontend: npm run frontend:dev"
echo "Build FE: npm run frontend:build"
