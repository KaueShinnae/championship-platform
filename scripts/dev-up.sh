#!/usr/bin/env bash
# Wrapper para Git Bash / WSL: delega ao dev-up.ps1 (fonte unica da
# orquestracao, que precisa rodar no lado Windows — java, docker, vite).
# Uso: bash scripts/dev-up.sh            (equivale a -SkipBuild)
#      bash scripts/dev-up.sh --build    (recompila os jars antes)

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# converte o caminho para o formato Windows quando rodando no WSL
if command -v wslpath >/dev/null 2>&1; then
  PS_SCRIPT="$(wslpath -w "$SCRIPT_DIR/dev-up.ps1")"
else
  PS_SCRIPT="$SCRIPT_DIR/dev-up.ps1"
fi

if [ "$1" = "--build" ]; then
  powershell.exe -ExecutionPolicy Bypass -File "$PS_SCRIPT"
else
  powershell.exe -ExecutionPolicy Bypass -File "$PS_SCRIPT" -SkipBuild
fi
