#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env}"
BACKUP_FILE="${1:-}"

if [[ -z "$BACKUP_FILE" || ! -f "$BACKUP_FILE" ]]; then
  echo "Uso: RESTORE_CONFIRM=YES bash scripts/restore-db.sh /ruta/backup.sql.gz" >&2
  exit 1
fi
if [[ "${RESTORE_CONFIRM:-}" != "YES" ]]; then
  echo "Restauración bloqueada. Revisa el archivo y repite con RESTORE_CONFIRM=YES." >&2
  exit 1
fi
if [[ ! -f "$ENV_FILE" ]]; then
  echo "No se encontró el archivo de variables: $ENV_FILE" >&2
  exit 1
fi

# shellcheck disable=SC1091
source "$PROJECT_DIR/scripts/load-env.sh"
load_env_file "$ENV_FILE"

: "${MYSQL_DATABASE:?MYSQL_DATABASE es obligatorio}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD es obligatorio para la operación administrativa}"

cd "$PROJECT_DIR"
case "$BACKUP_FILE" in
  *.gz) gzip -cd "$BACKUP_FILE" ;;
  *.sql) cat "$BACKUP_FILE" ;;
  *) echo "El respaldo debe terminar en .sql o .sql.gz" >&2; exit 1 ;;
esac | docker compose --env-file "$ENV_FILE" exec -T \
  -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
  --user=root "$MYSQL_DATABASE"

printf 'Restauración finalizada. Verifica /api/v1/health y valida los datos antes de reabrir el servicio.\n'
