# Sobe a stack completa de desenvolvimento local:
#   Docker (Postgres + Kafka) -> espera saude -> 3 servicos Spring (java -jar)
# Uso: powershell -ExecutionPolicy Bypass -File scripts\dev-up.ps1 [-SkipBuild]
param([switch]$SkipBuild)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "==> Verificando Docker..." -ForegroundColor Cyan
docker info *> $null
if (-not $?) {
    Write-Host "Docker engine nao esta rodando. Abra o Docker Desktop e rode de novo." -ForegroundColor Red
    exit 1
}

Write-Host "==> Subindo infra (docker compose up -d)..." -ForegroundColor Cyan
Set-Location $root
docker compose up -d

Write-Host "==> Aguardando Kafka ficar saudavel..." -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(3)
while ((Get-Date) -lt $deadline) {
    $status = docker ps --filter "name=championship-kafka" --format "{{.Status}}" | Select-Object -First 1
    if ($status -match "healthy") { break }
    Start-Sleep -Seconds 5
}

if (-not $SkipBuild) {
    Write-Host "==> Empacotando servicos (mvn package -DskipTests)..." -ForegroundColor Cyan
    foreach ($svc in @("inscricoes-service", "partidas-service", "ranking-service")) {
        Set-Location "$root\$svc"
        mvn -q package -DskipTests
        if (-not $?) { Write-Host "build falhou em $svc" -ForegroundColor Red; exit 1 }
    }
    Set-Location $root
}

function Test-PortUp($port) {
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -TimeoutSec 2 -UseBasicParsing
        return $resp.StatusCode -eq 200
    } catch { return $false }
}

Write-Host "==> Iniciando servicos Spring..." -ForegroundColor Cyan
$svcPorts = @{ "inscricoes-service" = 8081; "partidas-service" = 8082; "ranking-service" = 8083 }
foreach ($svc in @("inscricoes-service", "partidas-service", "ranking-service")) {
    if (Test-PortUp $svcPorts[$svc]) {
        Write-Host "   $svc ja esta no ar (porta $($svcPorts[$svc])), pulando"
        continue
    }
    $jar = Get-ChildItem "$root\$svc\target\*-SNAPSHOT.jar" | Select-Object -First 1
    # caminho entre aspas: o repo vive em pasta com espaco/acento (OneDrive\Área de Trabalho)
    Start-Process -WindowStyle Hidden java -ArgumentList "-jar", "`"$($jar.FullName)`"" `
        -RedirectStandardOutput "$env:TEMP\$svc.log" -RedirectStandardError "$env:TEMP\$svc.err.log"
    Write-Host "   $svc iniciado (log: $env:TEMP\$svc.log)"
}

Write-Host "==> Aguardando health checks..." -ForegroundColor Cyan
$ports = @(8081, 8082, 8083)
$deadline = (Get-Date).AddMinutes(3)
while ((Get-Date) -lt $deadline) {
    $up = 0
    foreach ($port in $ports) {
        try {
            $resp = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -TimeoutSec 2 -UseBasicParsing
            if ($resp.StatusCode -eq 200) { $up++ }
        } catch {}
    }
    if ($up -eq 3) { break }
    Start-Sleep -Seconds 5
}

Write-Host "==> Iniciando web-dashboard (Vite)..." -ForegroundColor Cyan
$viteUp = $false
try {
    $viteResp = Invoke-WebRequest -Uri "http://localhost:5173" -TimeoutSec 2 -UseBasicParsing
    $viteUp = $viteResp.StatusCode -eq 200
} catch {}
if ($viteUp) {
    Write-Host "   web-dashboard ja esta no ar (porta 5173), pulando"
} elseif (Test-Path "$root\web-dashboard\node_modules") {
    Start-Process -WindowStyle Hidden cmd -ArgumentList "/c", "cd /d `"$root\web-dashboard`" && npm run dev > `"$env:TEMP\vite.log`" 2>&1"
    Write-Host "   web-dashboard iniciado (log: $env:TEMP\vite.log)"
} else {
    Write-Host "   pulei: rode 'npm install' em web-dashboard\ primeiro" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Stack no ar:" -ForegroundColor Green
Write-Host "  Dashboard           http://localhost:5173"
Write-Host "  inscricoes-service  http://localhost:8081"
Write-Host "  partidas-service    http://localhost:8082"
Write-Host "  ranking-service     http://localhost:8083"
Write-Host "  Kafka UI            http://localhost:8090"
Write-Host "  Postgres            localhost:5432 (admin/admin ou championship/championship)"
Write-Host ""
Write-Host "Servidor MCP: cd mcp-agent-service; npm run dev (ou via .mcp.json no Claude Code)"
