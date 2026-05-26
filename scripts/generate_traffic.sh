#!/bin/bash
# Script de generación de tráfico sintético para el sistema de monitoreo

BASE_URL="${API_URL:-http://localhost:8080}"
INTERVAL="${REQUEST_INTERVAL:-1}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-0}"   # 0 = infinito
BURST_SIZE="${BURST_SIZE:-1}"

ENDPOINTS=("/" "/api/datos" "/api/productos" "/api/categorias" "/api/productos/1" "/api/lento" "/api/buscar?q=vestido" "/api/buscar?q=negro")
WEIGHTS=(15 20 20 10 10 5 10 10)  # porcentaje de probabilidad por endpoint

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

pick_endpoint() {
    local rand=$((RANDOM % 100))
    local cumulative=0
    for i in "${!WEIGHTS[@]}"; do
        cumulative=$((cumulative + WEIGHTS[i]))
        if [ $rand -lt $cumulative ]; then
            echo "${ENDPOINTS[$i]}"
            return
        fi
    done
    echo "/"
}

send_request() {
    local endpoint=$1
    local url="${BASE_URL}${endpoint}"
    local start_ms
    local end_ms
    local elapsed_ms

    start_ms=$(date +%s%3N)
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null)
    end_ms=$(date +%s%3N)
    elapsed_ms=$((end_ms - start_ms))

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        log "OK    [${http_code}] ${endpoint} - ${elapsed_ms}ms"
    else
        log "ERROR [${http_code}] ${endpoint} - ${elapsed_ms}ms"
    fi
}

send_burst() {
    for _ in $(seq 1 "$BURST_SIZE"); do
        local endpoint
        endpoint=$(pick_endpoint)
        send_request "$endpoint" &
    done
    wait
}

print_header() {
    echo "=============================================="
    echo "  Generador de Tráfico Sintético"
    echo "=============================================="
    echo "  Base URL   : $BASE_URL"
    echo "  Intervalo  : ${INTERVAL}s entre rafagas"
    echo "  Burst size : $BURST_SIZE requests/rafaga"
    echo "  Total      : $([ "$TOTAL_REQUESTS" -eq 0 ] && echo 'infinito' || echo "$TOTAL_REQUESTS")"
    echo "  Endpoints  : ${ENDPOINTS[*]}"
    echo "=============================================="
    echo "Presiona Ctrl+C para detener"
    echo ""
}

wait_for_api() {
    log "Esperando que la API esté disponible en $BASE_URL ..."
    local retries=30
    while [ $retries -gt 0 ]; do
        if curl -s --max-time 3 "$BASE_URL/" > /dev/null 2>&1; then
            log "API disponible. Iniciando generación de tráfico."
            return 0
        fi
        retries=$((retries - 1))
        sleep 2
    done
    log "ERROR: La API no respondió después de 60 segundos. Verifica que los contenedores estén corriendo."
    exit 1
}

# ---- main ----
print_header
wait_for_api

count=0
while true; do
    send_burst
    count=$((count + BURST_SIZE))

    if [ "$TOTAL_REQUESTS" -gt 0 ] && [ "$count" -ge "$TOTAL_REQUESTS" ]; then
        log "Completados $count requests. Finalizando."
        break
    fi

    sleep "$INTERVAL"
done
