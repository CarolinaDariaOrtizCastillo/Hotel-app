#!/bin/bash
# =============================================================================
# setup.sh - Script de automatización completa para Hotel Microservices
# Levanta todo desde cero: Minikube, imágenes Docker, K8s, Monitoring
# =============================================================================

set -euo pipefail

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info()    { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# Configuración
NAMESPACE="hotel-app"
CATALOG_IMAGE="hotel-catalog-service:latest"
BOOKING_IMAGE="hotel-booking-service:latest"
HELM_RELEASE="prometheus-stack"
MONITORING_NAMESPACE="monitoring"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "================================================================"
echo "  🏨 Hotel Microservices - Setup Automático"
echo "  Directorio raíz: $ROOT_DIR"
echo "================================================================"

# ---- PASO 1: Verificar dependencias ----
log_info "Verificando herramientas necesarias..."
for tool in docker minikube kubectl helm; do
  command -v "$tool" >/dev/null 2>&1 || log_error "Herramienta '$tool' no encontrada. Por favor instálala."
done
log_success "Todas las herramientas requeridas están disponibles."

# ---- PASO 2: Iniciar Minikube ----
log_info "Verificando estado de Minikube..."
if minikube status | grep -q "Running"; then
  log_success "Minikube ya está en ejecución."
else
  log_info "Iniciando Minikube (driver=docker, cpus=4, memory=6g)..."
  minikube start --driver=docker --cpus=4 --memory=6144 --disk-size=20g
  log_success "Minikube iniciado correctamente."
fi

# ---- PASO 3: Configurar Docker para usar el daemon de Minikube ----
log_info "Configurando entorno Docker de Minikube..."
eval "$(minikube docker-env)"
log_success "Docker apuntando al daemon de Minikube."

# ---- PASO 4: Build de imágenes Docker (multi-stage) ----
log_info "Construyendo imagen Docker: hotel-catalog-service..."
docker build -t "$CATALOG_IMAGE" "$ROOT_DIR/microservices/hotel-catalog-service"
log_success "Imagen $CATALOG_IMAGE construida."

log_info "Construyendo imagen Docker: hotel-booking-service..."
docker build -t "$BOOKING_IMAGE" "$ROOT_DIR/microservices/hotel-booking-service"
log_success "Imagen $BOOKING_IMAGE construida."

# ---- PASO 5: Crear namespace hotel-app ----
log_info "Creando namespace '$NAMESPACE'..."
kubectl apply -f "$ROOT_DIR/k8s/namespace.yaml"
log_success "Namespace '$NAMESPACE' listo."

# ---- PASO 6: Instalar stack de monitoreo con Helm ----
log_info "Verificando si el namespace 'monitoring' existe..."
kubectl get namespace "$MONITORING_NAMESPACE" >/dev/null 2>&1 || kubectl create namespace "$MONITORING_NAMESPACE"

log_info "Agregando repositorio de Helm para Prometheus..."
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts 2>/dev/null || true
helm repo update

log_info "Instalando/actualizando kube-prometheus-stack con Helm..."
helm upgrade --install "$HELM_RELEASE" prometheus-community/kube-prometheus-stack \
  --namespace "$MONITORING_NAMESPACE" \
  --values "$ROOT_DIR/k8s/values-monitoring.yaml" \
  --timeout 15m0s
log_success "Stack Prometheus + Grafana instalado correctamente."

# ---- PASO 7: Desplegar microservicios en Kubernetes ----
log_info "Desplegando manifiestos de Kubernetes..."
kubectl apply -f "$ROOT_DIR/k8s/catalog-deployment.yaml"
kubectl apply -f "$ROOT_DIR/k8s/catalog-service.yaml"
kubectl apply -f "$ROOT_DIR/k8s/booking-deployment.yaml"
kubectl apply -f "$ROOT_DIR/k8s/booking-service.yaml"
log_success "Deployments y Services desplegados."

# ---- PASO 8: Aplicar reglas de Prometheus y ServiceMonitors ----
log_info "Aplicando PrometheusRules y ServiceMonitors..."
kubectl apply -f "$ROOT_DIR/k8s/catalog-servicemonitor.yaml"
kubectl apply -f "$ROOT_DIR/k8s/booking-servicemonitor.yaml"
kubectl apply -f "$ROOT_DIR/k8s/prometheus-rules.yaml"
log_success "Reglas y monitores de Prometheus aplicados."

# ---- PASO 9: Esperar a que los pods estén listos ----
log_info "Esperando a que los pods de los microservicios estén listos..."
kubectl rollout status deployment/hotel-catalog-service -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/hotel-booking-service -n "$NAMESPACE" --timeout=120s
log_success "Todos los pods están corriendo."

# ---- PASO 10: Mostrar URLs de acceso ----
echo ""
echo "================================================================"
echo -e "  ${GREEN}✅ Setup completado exitosamente!${NC}"
echo "================================================================"
MINIKUBE_IP=$(minikube ip)
echo ""
echo "  🏨 URLs de Microservicios:"
echo "     Catálogo:    http://$MINIKUBE_IP:30081"
echo "     Reservas:    http://$MINIKUBE_IP:30082"
echo ""
echo "  📊 Stack de Monitoreo:"
echo "     Prometheus:  http://$MINIKUBE_IP:30090"
echo "     Grafana:     http://$MINIKUBE_IP:31000  (admin / AdminPassword123!)"
echo "     Alertmanager:http://$MINIKUBE_IP:30903"
echo ""
echo "  📖 Ejemplos de uso:"
echo "     curl http://$MINIKUBE_IP:30081/api/v1/rooms"
echo "     curl http://$MINIKUBE_IP:30081/api/v1/activities"
echo ""
echo "  🔥 Para generar carga de prueba:"
echo "     ./load-testing/stress.sh $MINIKUBE_IP"
echo "================================================================"
