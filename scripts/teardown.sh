#!/bin/bash
# =============================================================================
# teardown.sh - Elimina todos los recursos del clúster Minikube
# Limpia namespace, Helm releases, y opcionalmente detiene Minikube
# =============================================================================

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $1"; }

NAMESPACE="hotel-app"
HELM_RELEASE="prometheus-stack"
MONITORING_NAMESPACE="monitoring"

echo "================================================================"
echo "  🧹 Hotel Microservices - Teardown"
echo "================================================================"

# ---- Eliminar namespace de la aplicación ----
log_info "Eliminando namespace '$NAMESPACE' y todos sus recursos..."
kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
log_success "Namespace '$NAMESPACE' eliminado."

# ---- Eliminar Helm release de monitoreo ----
log_info "Desinstalando Helm release '$HELM_RELEASE'..."
helm uninstall "$HELM_RELEASE" -n "$MONITORING_NAMESPACE" 2>/dev/null && \
  log_success "Release '$HELM_RELEASE' desinstalado." || \
  log_warn "Release '$HELM_RELEASE' no encontrado, se omite."

# ---- Eliminar CRDs de Prometheus Operator (opcional) ----
log_info "Limpiando CRDs de monitoring.coreos.com..."
kubectl delete crd \
  prometheuses.monitoring.coreos.com \
  prometheusrules.monitoring.coreos.com \
  servicemonitors.monitoring.coreos.com \
  podmonitors.monitoring.coreos.com \
  alertmanagers.monitoring.coreos.com \
  alertmanagerconfigs.monitoring.coreos.com \
  thanosrulers.monitoring.coreos.com \
  probes.monitoring.coreos.com 2>/dev/null || log_warn "Algunos CRDs no existían, se omiten."
log_success "CRDs eliminados."

# ---- Eliminar namespace de monitoreo ----
log_info "Eliminando namespace '$MONITORING_NAMESPACE'..."
kubectl delete namespace "$MONITORING_NAMESPACE" --ignore-not-found=true
log_success "Namespace '$MONITORING_NAMESPACE' eliminado."

# ---- Preguntar si detener Minikube ----
echo ""
read -p "¿Deseas también detener Minikube? (s/N): " stop_minikube
if [[ "$stop_minikube" =~ ^[sS]$ ]]; then
  log_info "Deteniendo Minikube..."
  minikube stop
  log_success "Minikube detenido."
else
  log_warn "Minikube sigue corriendo. Puedes detenerlo manualmente con: minikube stop"
fi

echo ""
echo "================================================================"
echo -e "  ${GREEN}✅ Teardown completado. Entorno limpio.${NC}"
echo "================================================================"
