# Desliga o ambiente local: servicos Java, dashboard (Vite/node) e containers.
# Uso: npm run down  (ou powershell -ExecutionPolicy Bypass -File scripts\dev-down.ps1)

$ErrorActionPreference = "SilentlyContinue"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "==> Parando servicos Java..." -ForegroundColor Cyan
Get-Process java | Stop-Process -Force
Write-Host "==> Parando dashboard (node/vite)..." -ForegroundColor Cyan
Get-Process node | Where-Object { $_.Path -notlike "*Docker*" } | Stop-Process -Force

Write-Host "==> Parando containers (docker compose stop)..." -ForegroundColor Cyan
Set-Location $root
docker compose stop

Write-Host ""
Write-Host "Ambiente desligado. Para subir de novo: npm run dev" -ForegroundColor Green
