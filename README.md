# Hotel Microservices — Sistema de Reservas Hoteleras 🏨

Sistema de microservicios con observabilidad completa basado en Spring Boot 3, Kubernetes y Prometheus/Grafana.

## Estructura del Proyecto

```
hotel-microservices/
├── microservices/
│   ├── hotel-catalog-service/     # Servicio A: Catálogo (Puerto 8081)
│   └── hotel-booking-service/     # Servicio B: Reservas (Puerto 8082)
├── k8s/
│   ├── namespace.yaml
│   ├── catalog-deployment.yaml
│   ├── catalog-service.yaml
│   ├── catalog-servicemonitor.yaml
│   ├── booking-deployment.yaml
│   ├── booking-service.yaml
│   ├── booking-servicemonitor.yaml
│   ├── prometheus-rules.yaml
│   ├── values-monitoring.yaml
│   └── grafana-dashboard.json
├── load-testing/
│   └── stress.sh
├── scripts/
│   ├── setup.sh
│   └── teardown.sh
└── docs/
    └── informe.md
```

## Inicio Rápido

```bash
# Levantar todo automáticamente
./scripts/setup.sh

# Ver URLs de acceso
# Catálogo:   http://<MINIKUBE_IP>:30081
# Reservas:   http://<MINIKUBE_IP>:30082
# Grafana:    http://<MINIKUBE_IP>:31000  (admin/AdminPassword123!)
# Prometheus: http://<MINIKUBE_IP>:30090
```

## Endpoints Principales

### Servicio A — Catálogo (`hotel-catalog-service:8081`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/rooms` | Listar habitaciones |
| GET | `/api/v1/rooms/{id}` | Detalle de habitación |
| GET | `/api/v1/activities` | Listar actividades |
| GET | `/api/v1/activities/{id}` | Detalle de actividad |
| POST | `/api/v1/activities/admin/delay?delayMs=3000` | Activar delay artificial |
| GET | `/health` | Estado del servicio |
| GET | `/metrics` | Métricas Prometheus |

### Servicio B — Reservas (`hotel-booking-service:8082`)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/bookings` | Crear reserva |
| GET | `/api/v1/bookings` | Listar reservas |
| GET | `/api/v1/bookings/{id}` | Consultar por ID |
| PATCH | `/api/v1/bookings/{id}/cancel` | Cancelar reserva |
| GET | `/api/v1/bookings/client/{email}` | Reservas por cliente |
| GET | `/health` | Estado del servicio |
| GET | `/metrics` | Métricas Prometheus |

## Ejemplo de Uso — Crear una Reserva

```bash
curl -X POST http://<MINIKUBE_IP>:30082/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "María García",
    "clientEmail": "maria@hotel.com",
    "type": "ROOM",
    "resourceId": "room-201",
    "quantity": 2,
    "date": "2026-11-15"
  }'
```

## Tecnologías

- **Java 17 + Spring Boot 3.2.5**
- **Spring WebClient** (comunicación reactiva entre servicios)
- **Micrometer + Prometheus** (métricas: Counter, Gauge, Timer)
- **Docker** (imágenes multi-stage)
- **Kubernetes / Minikube** (orquestación)
- **Helm** (kube-prometheus-stack)
- **Grafana** (dashboards y alertas)
