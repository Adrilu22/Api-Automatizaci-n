# Script de generación de tráfico sintético (PowerShell) para Windows
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$IntervalSeconds = 1,
    [int]$TotalRequests = 0,
    [int]$BurstSize = 1
)

$Endpoints = @("/", "/api/datos", "/api/lento")
$Weights   = @(25, 50, 25)

function Write-Log($Message) {
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
}

function Get-RandomEndpoint {
    $rand = Get-Random -Minimum 0 -Maximum 100
    $cumulative = 0
    for ($i = 0; $i -lt $Weights.Length; $i++) {
        $cumulative += $Weights[$i]
        if ($rand -lt $cumulative) { return $Endpoints[$i] }
    }
    return "/"
}

function Send-Request($Endpoint) {
    $url = "$BaseUrl$Endpoint"
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        $sw.Stop()
        Write-Log "OK    [$($response.StatusCode)] $Endpoint - $($sw.ElapsedMilliseconds)ms"
    } catch {
        Write-Log "ERROR [---] $Endpoint - $_"
    }
}

function Wait-ForApi {
    Write-Log "Esperando que la API esté disponible en $BaseUrl ..."
    $retries = 30
    while ($retries -gt 0) {
        try {
            Invoke-WebRequest -Uri "$BaseUrl/" -Method GET -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop | Out-Null
            Write-Log "API disponible. Iniciando generación de tráfico."
            return
        } catch {
            $retries--
            Start-Sleep -Seconds 2
        }
    }
    Write-Log "ERROR: La API no respondió. Verifica que los contenedores estén corriendo."
    exit 1
}

Write-Host "=============================================="
Write-Host "  Generador de Tráfico Sintético (PowerShell)"
Write-Host "=============================================="
Write-Host "  Base URL   : $BaseUrl"
Write-Host "  Intervalo  : ${IntervalSeconds}s"
Write-Host "  Burst size : $BurstSize requests/rafaga"
Write-Host "  Total      : $(if ($TotalRequests -eq 0) { 'infinito' } else { $TotalRequests })"
Write-Host "=============================================="
Write-Host "Presiona Ctrl+C para detener"
Write-Host ""

Wait-ForApi

$count = 0
while ($true) {
    for ($b = 0; $b -lt $BurstSize; $b++) {
        $endpoint = Get-RandomEndpoint
        Send-Request $endpoint
        $count++
    }

    if ($TotalRequests -gt 0 -and $count -ge $TotalRequests) {
        Write-Log "Completados $count requests. Finalizando."
        break
    }

    Start-Sleep -Seconds $IntervalSeconds
}
