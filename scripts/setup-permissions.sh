#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

chmod +x mvnw 2>/dev/null || true
chmod +x scripts/*.sh 2>/dev/null || true

if [ -f backend/mvnw ]; then
  chmod +x backend/mvnw 2>/dev/null || true
fi

echo "Permisos restaurados para Maven Wrapper y scripts."
ls -l mvnw scripts/*.sh
