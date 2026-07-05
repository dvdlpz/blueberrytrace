#!/usr/bin/env bash
set -euo pipefail

PORT="${SERVER_PORT:-8080}"

if command -v ss >/dev/null 2>&1; then
  echo "Procesos escuchando en el puerto ${PORT}:"
  ss -ltnp "sport = :${PORT}" || true
else
  echo "No se encontró ss. En Arch Linux viene con iproute2."
fi
