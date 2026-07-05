#!/usr/bin/env bash
set -euo pipefail

PORT="${SERVER_PORT:-8080}"

if command -v fuser >/dev/null 2>&1; then
  fuser -k "${PORT}/tcp" || true
  echo "Puerto ${PORT} liberado."
  exit 0
fi

echo "No se encontró fuser. En Arch Linux instala psmisc: sudo pacman -S psmisc"
exit 1
