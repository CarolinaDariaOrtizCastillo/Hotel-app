# Informe Técnico — Hotel Microservices
## Proyecto: Sistema de Reservas Hoteleras con Observabilidad en Kubernetes

---

## 1. Arquitectura del Sistema

El sistema está compuesto por **dos microservicios** desplegados en Kubernetes bajo el namespace `hotel-app`, más un stack de monitoreo instalado via Helm.

```
┌─────────────────────────────────────────────────────────┐
│                  KUBERNETES CLUSTER (Minikube)           │
│                                                          │
│  namespace: hotel-app                                    │
│  ┌──────────────────────┐    WebClient     ┌──────────────────────┐  │
│  │  hotel-catalog-service│◄────────────────│hotel-booking-service │  │
│  │  Puerto: 8081          │   (con timeout) │  Puerto: 8082        │  │
│  │  /api/v1/rooms         │                 │  /api/v1/bookings    │  │
│  │  /api/v1/activities    │                 │                      │  │
│  │  /metrics (Prometheus) │                 │  /metrics (Prometheus│  │
│  └──────────────────────┘                  └──────────────────────┘  │
│                                                          │
│  namespace: monitoring                                   │
│  ┌───────────────┐  ┌────────────┐  ┌─────────────────┐ │
│  │  Prometheus   │  │  Grafana   │  │  Alertmanager   │ │
│  │  :30090       │  │  :31000    │  │  :30903         │ │
│  └───────────────┘  └────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Almacenamiento
Ambos servicios utilizan **almacenamiento en memoria** (estructuras `ConcurrentHashMap` y `List` de Java), sin ninguna base de datos. Los datos se inicializan en el arranque con el método `@PostConstruct`.

---

## 2. Descripción de Métricas Implementadas (Micrometer)

### Servicio A — hotel-catalog-service

| Métrica | Tipo | Descripción |
|---|---|---|
| `hotel_catalog_room_queries_total` | Counter | Total de consultas al catálogo de habitaciones |
| `hotel_catalog_activity_queries_total` | Counter | Total de consultas al catálogo de actividades |
| `hotel_catalog_request_duration_seconds` | Timer | Latencia de procesamiento de peticiones (p50, p95, p99) |
| `hotel_catalog_artificial_delay_ms` | Gauge | Delay artificial configurado (Casuística 2) |
| `hotel_catalog_total_rooms_count` | Gauge | Número de habitaciones en memoria |

### Servicio B — hotel-booking-service

| Métrica | Tipo | Descripción |
|---|---|---|
| `hotel_booking_created_total` | Counter | Total de reservas/citas creadas exitosamente |
| `hotel_booking_failures_total{reason="timeout"}` | Counter | Fallos por timeout al consultar al catálogo |
| `hotel_booking_failures_total{reason="catalog_error"}` | Counter | Fallos por error general de comunicación |
| `hotel_booking_catalog_call_duration_seconds` | Timer | Tiempo de llamadas WebClient hacia Servicio A |
| `hotel_booking_active_total` | Gauge | Reservas activas (no canceladas) en memoria |

---

## 3. Casuísticas de Falla y Degradación

### Casuística 1 — Servicio A Completamente Caído

**Descripción:** Se escala el `Deployment` del `hotel-catalog-service` a 0 réplicas con `kubectl scale deployment hotel-catalog-service -n hotel-app --replicas=0`.

**Comportamiento observado:**
- El `hotel-booking-service` intenta llamar al catálogo via `WebClient`.
- La conexión es rechazada (`Connection refused`).
- El `CatalogClient` captura la excepción y lanza `CatalogCommunicationException`.
- El `BookingController` responde con **HTTP 503 Service Unavailable** y un body JSON descriptivo.
- La métrica `hotel_booking_failures_total{reason="catalog_error"}` se incrementa.
- Prometheus detecta `up == 0` para el target del catálogo y dispara la alerta `HotelCatalogServiceDown`.

**Query PromQL para verificar:**
```promql
up{job=~".*hotel-catalog.*"}
increase(hotel_booking_failures_total{reason="catalog_error"}[2m])
```

---

### Casuística 2 — Servicio A Degradado por Alta Latencia (Timeout)

**Descripción:** Se activa el delay artificial del catálogo vía el endpoint de admin:
```bash
POST /api/v1/activities/admin/delay?delayMs=3000
```
El timeout configurado en el `hotel-booking-service` es de **2000 ms**.

**Comportamiento observado:**
- El catálogo responde correctamente pero tarda 3 segundos.
- El `WebClient` en el `hotel-booking-service` expira el timeout de 2 segundos.
- El `CatalogClient` captura `TimeoutException` y lanza `CatalogTimeoutException`.
- El `BookingController` responde con **HTTP 504 Gateway Timeout**.
- La métrica `hotel_booking_failures_total{reason="timeout"}` se incrementa.
- El Gauge `hotel_catalog_artificial_delay_ms` sube a 3000, disparando la alerta `HotelCatalogHighLatencyDegraded`.

**Query PromQL para verificar:**
```promql
hotel_catalog_artificial_delay_ms
rate(hotel_booking_failures_total{reason="timeout"}[2m])
```

---

### Casuística 3 — Carga Normal de Negocio

**Descripción:** Se generan 50 reservas de habitaciones y actividades y 30 consultas al catálogo usando el script `stress.sh 3`.

**Comportamiento observado:**
- El Counter `hotel_booking_created_total` se incrementa con cada reserva exitosa.
- El Gauge `hotel_booking_active_total` refleja en tiempo real el número de reservas activas.
- El Timer `hotel_catalog_request_duration_seconds` registra la latencia real de cada petición.
- El Counter `hotel_catalog_room_queries_total` y `hotel_catalog_activity_queries_total` acumulan las consultas.

**Query PromQL para verificar:**
```promql
hotel_booking_created_total
hotel_booking_active_total
rate(hotel_catalog_room_queries_total[1m])
```

---

## 4. Alertas de Prometheus (PromQL)

| Nombre | Expresión PromQL | Severidad | Categoría |
|---|---|---|---|
| `HotelCatalogServiceDown` | `up{job=~".*hotel-catalog.*"} == 0` | critical | Infraestructura |
| `HotelBookingServiceDown` | `up{job=~".*hotel-booking.*"} == 0` | critical | Infraestructura |
| `HotelHighBookingFailureRate` | `rate(hotel_booking_failures_total[2m]) > 0.05` | warning | Negocio |
| `HotelCatalogHighLatencyDegraded` | `hotel_catalog_artificial_delay_ms > 1500` | warning | Rendimiento |

---

## 5. Preguntas Teóricas

### ¿Por qué separar en dos microservicios?

La separación por **responsabilidad de dominio** permite escalar, actualizar y desplegar cada servicio de forma independiente. El catálogo (Servicio A) es de lectura intensiva y podría escalar a múltiples réplicas, mientras que el servicio de reservas (Servicio B) maneja lógica transaccional y validaciones de negocio. Si ambos estuvieran juntos, un fallo en la lógica de reservas caería el catálogo y viceversa.

### ¿Qué rol juega el timeout en la resiliencia?

El timeout es una barrera de aislamiento de fallos. Sin timeout, si el Servicio A se vuelve lento, el Servicio B acumula threads bloqueados esperando respuesta, agotando su pool de conexiones y colapsando también. Con timeout de 2 segundos, el Servicio B falla rápido, libera el recurso, registra la falla en métricas, y responde un error controlado al cliente, en lugar de congelar toda la cadena.

### ¿Por qué usar Micrometer en vez de instrumentar con la librería de Prometheus directo?

Micrometer es una **fachada de métricas** (como SLF4J para logging). Permite escribir código de instrumentación una sola vez (`Counter`, `Gauge`, `Timer`) y cambiar el backend de telemetría sin modificar el código de negocio. Hoy es Prometheus, mañana puede ser Datadog, InfluxDB o CloudWatch con solo cambiar la dependencia Maven.

---

## 6. Cómo correr el proyecto

```bash
# 1. Levantar todo desde cero
./scripts/setup.sh

# 2. Probar Casuística 1 (Catálogo caído)
./load-testing/stress.sh $(minikube ip) 1

# 3. Probar Casuística 2 (Timeout)
./load-testing/stress.sh $(minikube ip) 2

# 4. Generar carga normal (Casuística 3)
./load-testing/stress.sh $(minikube ip) 3

# 5. Limpiar todo
./scripts/teardown.sh
```
---

## 7. Evidencias de actividad implementada

### Pods, deployments, réplicas y services de hotel-ap

<img width="1193" height="780" alt="Captura de pantalla 2026-10-25 200655" src="https://github.com/user-attachments/assets/14359ccf-7fe2-4b37-a25b-07e77a2193de" />

<img width="1317" height="437" alt="Captura de pantalla 2026-10-25 201038" src="https://github.com/user-attachments/assets/39be0e7f-07a2-420f-bee4-b42646306b24" />


### Casuísticas y sus resultaods

<img width="1438" height="827" alt="Captura de pantalla 2026-10-25 201253" src="https://github.com/user-attachments/assets/76dc8073-d5eb-4f71-bd78-0198fccad4c4" />

<img width="1907" height="1011" alt="Captura de pantalla 2026-10-25 201507" src="https://github.com/user-attachments/assets/01cf0c3d-35cd-436e-b004-aeea8d13fcc1" />

<img width="1072" height="743" alt="Captura de pantalla 2026-10-25 215451" src="https://github.com/user-attachments/assets/c4c1fd00-1cbf-4717-a3fc-341e16e0bef7" />

<img width="1917" height="948" alt="Captura de pantalla 2026-10-25 215402" src="https://github.com/user-attachments/assets/a7168061-344f-4ebf-9564-adab756aa29f" />

<img width="1097" height="671" alt="Captura de pantalla 2026-10-25 220419" src="https://github.com/user-attachments/assets/684bbd95-63e6-446f-8453-58aeae10a923" />

<img width="1915" height="950" alt="Captura de pantalla 2026-10-25 220459" src="https://github.com/user-attachments/assets/51a5fd61-6be5-460b-b461-90c36a5300f0" />

<img width="355" height="676" alt="Captura de pantalla 2026-10-25 223523" src="https://github.com/user-attachments/assets/eeee525a-31f0-460c-8671-98ed89f92398" />

<img width="1337" height="631" alt="Captura de pantalla 2026-10-25 223644" src="https://github.com/user-attachments/assets/3b7397be-58a3-4e5a-b702-8f376e5177af" />
