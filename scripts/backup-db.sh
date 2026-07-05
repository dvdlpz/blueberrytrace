#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/blueberrytrace-backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "No se encontró el archivo de variables: $ENV_FILE" >&2
  exit 1
fi

# shellcheck disable=SC1091
source "$PROJECT_DIR/scripts/load-env.sh"
load_env_file "$ENV_FILE"

: "${MYSQL_DATABASE:?MYSQL_DATABASE es obligatorio}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD es obligatorio para la operación administrativa}"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUTPUT="$BACKUP_DIR/blueberrytrace_${MYSQL_DATABASE}_${TIMESTAMP}.sql.gz"
TEMP_OUTPUT="${OUTPUT}.partial"

trap 'rm -f "$TEMP_OUTPUT"' EXIT
cd "$PROJECT_DIR"
docker compose --env-file "$ENV_FILE" exec -T \
  -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
  mysqldump --user=root --single-transaction --routines --events --triggers \
  --set-gtid-purged=OFF "$MYSQL_DATABASE" | gzip -9 > "$TEMP_OUTPUT"

mv "$TEMP_OUTPUT" "$OUTPUT"
chmod 600 "$OUTPUT"
find "$BACKUP_DIR" -type f -name 'blueberrytrace_*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
printf 'Backup creado: %s\n' "$OUTPUT"
