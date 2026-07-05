#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/docs/apf3/evidencias"
mkdir -p "${OUT_DIR}"
cd "${ROOT_DIR}"

now="$(date '+%Y-%m-%d %H:%M:%S')"

write_header() {
  local file="$1"
  local title="$2"
  {
    echo "BlueberryTrace — Evidencia APF3"
    echo "${title}"
    echo "Generado: ${now}"
    echo "Directorio: ${ROOT_DIR}"
    echo ""
  } > "${file}"
}

run_or_note() {
  local file="$1"
  shift
  if "$@" >> "${file}" 2>&1; then
    return 0
  fi
  echo "" >> "${file}"
  echo "[Aviso] El comando no pudo completarse: $*" >> "${file}"
}

write_header "${OUT_DIR}/01-git-status.txt" "Estado del repositorio"
run_or_note "${OUT_DIR}/01-git-status.txt" git status --short --branch

write_header "${OUT_DIR}/02-git-log.txt" "Historial de commits"
run_or_note "${OUT_DIR}/02-git-log.txt" git log --oneline --decorate --graph --all -n 25

write_header "${OUT_DIR}/03-git-branches.txt" "Ramas locales y remotas"
run_or_note "${OUT_DIR}/03-git-branches.txt" git branch -a

write_header "${OUT_DIR}/04-git-remote.txt" "Repositorio remoto"
run_or_note "${OUT_DIR}/04-git-remote.txt" git remote -v

write_header "${OUT_DIR}/05-project-tree.txt" "Estructura principal del proyecto"
if command -v find >/dev/null 2>&1; then
  find . -maxdepth 3 \
    -path './.git' -prune -o \
    -path './node_modules' -prune -o \
    -path './frontend/node_modules' -prune -o \
    -path './backend/target' -prune -o \
    -print | sort >> "${OUT_DIR}/05-project-tree.txt"
else
  echo "find no disponible" >> "${OUT_DIR}/05-project-tree.txt"
fi

write_header "${OUT_DIR}/06-key-files.txt" "Archivos clave para APF3"
for file in \
  README.md \
  package.json \
  backend/pom.xml \
  backend/src/main/java/com/keraune/vlvblueberrysystem/VlvBlueberrySystemApplication.java \
  backend/src/main/java/com/keraune/vlvblueberrysystem/config/SecurityConfig.java \
  frontend/package.json \
  frontend/src/App.tsx \
  docs/apf3/01-informe-apf3.md; do
  if [ -f "${file}" ]; then
    echo "OK  ${file}" >> "${OUT_DIR}/06-key-files.txt"
  else
    echo "FALTA ${file}" >> "${OUT_DIR}/06-key-files.txt"
  fi
done

write_header "${OUT_DIR}/07-build-commands.txt" "Comandos sugeridos de validación"
cat >> "${OUT_DIR}/07-build-commands.txt" <<'COMMANDS'
Ejecutar manualmente:

npm run setup:permissions
npm run frontend:build
npm run backend:run
npm run frontend:dev

Validar en navegador:
http://localhost:5173
http://localhost:8080/api/v1/health
COMMANDS

echo "Evidencias APF3 generadas en: ${OUT_DIR}"
