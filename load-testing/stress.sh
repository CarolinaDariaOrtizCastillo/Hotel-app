#!/bin/bash
# =============================================================================
# stress.sh - Generador de carga para las 3 Casuísticas de Falla
# Uso: ./stress.sh <MINIKUBE_IP> [casuistica]
# Casuísticas: 1 (servicio caído), 2 (timeout/degradado), 3 (carga negocio)
# =============================================================================

set -uo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

MINIKUBE_IP="${1:-$(minikube ip 2>/dev/null || echo '127.0.0.1')}"
CASUISTICA="${2:-3}"

CATALOG_BASE="http://$MINIKUBE_IP:30081"
BOOKING_BASE="http://$MINIKUBE_IP:30082"

log_info()    { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }
log_test()    { echo -e "${CYAN}[TEST]${NC}  $1"; }

echo "================================================================"
echo "  🔥 Hotel Microservices - Stress Test"
echo "  IP de Minikube: $MINIKUBE_IP"
echo "  Casuística:     $CASUISTICA"
echo "================================================================"

# ---------------------------------------------------------------------------
# CASUÍSTICA 1: Servicio A (Catálogo) Caído
# Propósito: Demostrar que el Servicio B (Reservas) captura el error y responde
#            con HTTP 503 controlado cuando el Catálogo no está disponible.
# Cómo simular: Escalamos el deployment del catálogo a 0 réplicas.
# ---------------------------------------------------------------------------
casuistica_1() {
  echo ""
  echo -e "${RED}========== CASUÍSTICA 1: Servicio de Catálogo CAÍDO ==========${NC}"
  log_warn "Escalando hotel-catalog-service a 0 réplicas para simular caída..."
  kubectl scale deployment hotel-catalog-service -n hotel-app --replicas=0
  sleep 5
  log_info "Enviando 10 solicitudes de reserva mientras el catálogo está caído..."
  for i in $(seq 1 10); do
    response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BOOKING_BASE/api/v1/bookings" \
      -H "Content-Type: application/json" \
      -d '{"clientName":"Test User","clientEmail":"test@hotel.com","type":"ROOM","resourceId":"room-101","quantity":1,"date":"2026-11-15"}')
    log_test "Request $i -> HTTP $response $([ "$response" -eq 503 ] || [ "$response" -eq 504 ] && echo '✅ Fallback correcto' || echo '⚠️ Respuesta inesperada')"
    sleep 1
  done

  log_info "Restaurando catálogo a 1 réplica..."
  kubectl scale deployment hotel-catalog-service -n hotel-app --replicas=1
  kubectl rollout status deployment/hotel-catalog-service -n hotel-app --timeout=60s
  log_success "Casuística 1 completada. Revisa Grafana: alerta HotelCatalogServiceDown."
}

# ---------------------------------------------------------------------------
# CASUÍSTICA 2: Servicio A Degradado por Latencia Artificial (Timeout en B)
# Propósito: Demostrar que el Servicio B tiene timeout de 2 segundos configurado
#            y cuando el catálogo supera ese tiempo, captura la excepción y
#            retorna HTTP 504 Gateway Timeout + métrica de fallo se incrementa.
# Cómo simular: Activar el delay artificial via endpoint de admin.
# ---------------------------------------------------------------------------
casuistica_2() {
  echo ""
  echo -e "${YELLOW}========== CASUÍSTICA 2: Latencia Degradada - TIMEOUT ==========${NC}"
  log_warn "Activando delay artificial de 3000 ms en Catálogo (timeout de B = 2000 ms)..."
  curl -s -X POST "$CATALOG_BASE/api/v1/activities/admin/delay?delayMs=3000" | cat
  echo ""
  sleep 2

  log_info "Enviando 10 solicitudes de reserva con catálogo degradado..."
  for i in $(seq 1 10); do
    response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BOOKING_BASE/api/v1/bookings" \
      -H "Content-Type: application/json" \
      -d '{"clientName":"Timeout Test","clientEmail":"timeout@hotel.com","type":"ACTIVITY","resourceId":"act-001","quantity":1,"date":"2026-11-20"}')
    log_test "Request $i -> HTTP $response $([ "$response" -eq 504 ] && echo '✅ Timeout capturado correctamente' || echo "⚠️ $response")"
    sleep 1
  done

  log_info "Desactivando delay artificial (restaurando a 0 ms)..."
  curl -s -X POST "$CATALOG_BASE/api/v1/activities/admin/delay?delayMs=0" | cat
  echo ""
  log_success "Casuística 2 completada. Revisa Grafana: hotel_booking_failures_total{reason='timeout'}"
}

# ---------------------------------------------------------------------------
# CASUÍSTICA 3: Carga Normal de Negocio (Reservas y Consultas)
# Propósito: Generar tráfico realista para que las métricas de negocio sean
#            visibles en Grafana y se puedan demostrar Counter, Gauge y Timer.
# ---------------------------------------------------------------------------
casuistica_3() {
  echo ""
  echo -e "${GREEN}========== CASUÍSTICA 3: Carga Normal de Negocio ==========${NC}"
  log_info "Iniciando 50 reservas de habitaciones y actividades..."

  ROOMS=("room-101" "room-102" "room-201" "room-301")
  ACTIVITIES=("act-001" "act-002" "act-003" "act-004")
  NAMES=("Ana García" "Luis Martínez" "María López" "Carlos Pérez" "Sofía Ruiz")
  EMAILS=("ana@test.com" "luis@test.com" "maria@test.com" "carlos@test.com" "sofia@test.com")

  for i in $(seq 1 25); do
    IDX=$(( (i - 1) % 4 ))
    EIDX=$(( (i - 1) % 5 ))
    ROOM="${ROOMS[$IDX]}"
    NAME="${NAMES[$EIDX]}"
    EMAIL="${EMAILS[$EIDX]}"

    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BOOKING_BASE/api/v1/bookings" \
      -H "Content-Type: application/json" \
      -d "{\"clientName\":\"$NAME\",\"clientEmail\":\"$EMAIL\",\"type\":\"ROOM\",\"resourceId\":\"$ROOM\",\"quantity\":2,\"date\":\"2026-11-$(printf '%02d' $i)\"}")
    log_test "Reserva habitación $ROOM para $NAME -> HTTP $http_code"
    sleep 0.3
  done

  for i in $(seq 1 25); do
    IDX=$(( (i - 1) % 4 ))
    EIDX=$(( (i - 1) % 5 ))
    ACT="${ACTIVITIES[$IDX]}"
    NAME="${NAMES[$EIDX]}"
    EMAIL="${EMAILS[$EIDX]}"

    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BOOKING_BASE/api/v1/bookings" \
      -H "Content-Type: application/json" \
      -d "{\"clientName\":\"$NAME\",\"clientEmail\":\"$EMAIL\",\"type\":\"ACTIVITY\",\"resourceId\":\"$ACT\",\"quantity\":1,\"date\":\"2026-11-$(printf '%02d' $i)\"}")
    log_test "Reserva actividad $ACT para $NAME -> HTTP $http_code"
    sleep 0.3
  done

  log_info "Consultando catálogo repetidamente para generar métricas de Counter/Timer..."
  for i in $(seq 1 30); do
    curl -s "$CATALOG_BASE/api/v1/rooms" > /dev/null
    curl -s "$CATALOG_BASE/api/v1/activities" > /dev/null
    sleep 0.2
  done

  log_success "Casuística 3 completada."
  log_info "Consulta todas las reservas: GET $BOOKING_BASE/api/v1/bookings"
  log_info "Revisa Grafana: hotel_booking_created_total, hotel_booking_active_total"
}

# ---- Ejecutar casuística seleccionada ----
case "$CASUISTICA" in
  1) casuistica_1 ;;
  2) casuistica_2 ;;
  3) casuistica_3 ;;
  all)
    casuistica_3
    sleep 5
    casuistica_2
    sleep 10
    casuistica_1
    ;;
  *)
    echo "Uso: $0 <MINIKUBE_IP> <1|2|3|all>"
    echo "  1   - Simular Servicio A caído"
    echo "  2   - Simular timeout/latencia degradada en Servicio A"
    echo "  3   - Carga normal de negocio (reservas y consultas)"
    echo "  all - Ejecutar las 3 casuísticas en orden"
    exit 1
    ;;
esac

echo ""
echo "================================================================"
echo -e "  ${GREEN}✅ Script completado.${NC}"
echo "  Métricas disponibles en: http://$MINIKUBE_IP:31000 (Grafana)"
echo "================================================================"
