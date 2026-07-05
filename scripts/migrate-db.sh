#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env}"

if [[ "${MIGRATION_CONFIRM:-}" != "YES" ]]; then
  echo "Migración bloqueada. Crea y verifica un backup antes de repetir con MIGRATION_CONFIRM=YES." >&2
  exit 1
fi

# shellcheck disable=SC1091
source "$PROJECT_DIR/scripts/load-env.sh"
load_env_file "$ENV_FILE"
: "${MYSQL_DATABASE:?MYSQL_DATABASE es obligatorio}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker no está disponible. Ejecuta la migración en el VPS donde opera Docker Compose." >&2
  exit 1
fi

cd "$PROJECT_DIR"
echo "Flyway se ejecuta al iniciar el backend. Reconstruyendo e iniciando el servicio…"
docker compose --env-file "$ENV_FILE" up -d --build backend
printf 'Solicitud de migración enviada. Revisa: docker compose --env-file %s logs --tail=200 backend\n' "$ENV_FILE"
printf 'Verifica que Flyway haya aplicado las versiones pendientes y que /api/v1/health responda correctamente.\n'
