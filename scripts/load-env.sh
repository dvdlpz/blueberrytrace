#!/usr/bin/env bash
# Source this helper from project scripts. It parses KEY=value safely and never executes .env content.
load_env_file() {
  local env_file="$1"
  local raw_line line key value

  if [[ ! -f "$env_file" ]]; then
    echo "No se encontró el archivo de variables: $env_file" >&2
    return 1
  fi

  while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
    line="${raw_line%$'\r'}"
    [[ "$line" =~ ^[[:space:]]*$ ]] && continue
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    if [[ ! "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      echo "Configuración inválida en $env_file: $line" >&2
      return 1
    fi
    key="${BASH_REMATCH[1]}"
    value="${BASH_REMATCH[2]}"
    if [[ ${#value} -ge 2 ]] && { [[ "${value:0:1}" == "\"" && "${value: -1}" == "\"" ]] || [[ "${value:0:1}" == "'" && "${value: -1}" == "'" ]]; }; then
      value="${value:1:${#value}-2}"
    fi
    export "${key}=${value}"
  done < "$env_file"
}
