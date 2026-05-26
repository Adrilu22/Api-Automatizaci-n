# UrbanGlow — Sistema de Monitoreo y Observabilidad para API REST

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Prometheus](https://img.shields.io/badge/Prometheus-2.51-red)
![Grafana](https://img.shields.io/badge/Grafana-10.4-yellow)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

Sistema completo de observabilidad implementado sobre una API REST de tienda de moda. Incluye recolección de métricas con **Prometheus**, visualización con **Grafana**, despliegue contenerizado con **Docker Compose** y un dashboard interactivo con operaciones CRUD completas.

---

## Información del estudiante

| Campo | Detalle |
|---|---|
| **Nombre completo** | Adriana Lucia Carreño Medina |
| **Codigo** | 202310009601 |
| **Repositorio** | https://github.com/Adrilu22/Api-Automatizaci-n |
| **Video URL** | https://drive.google.com/file/d/1425kUUkzC0oJBld_VOZMXTKf9h7AvrUK/view?usp=sharing |

---

## Descripción del proyecto

**UrbanGlow** es una API REST de una tienda de moda desarrollada con Spring Boot. El proyecto demuestra la implementación de un stack de monitoreo completo en el que la API expone métricas personalizadas en formato Prometheus (contadores, histogramas y gauges), Prometheus las recolecta cada 15 segundos y Grafana las visualiza en un dashboard con 9 paneles en tiempo real.

Las métricas de negocio son: ventas por mes, clientes premium y descuentos aplicados por mes. Adicionalmente, la API implementa operaciones CRUD completas (POST, PUT, DELETE) sobre ventas y clientes, accesibles desde el dashboard interactivo del frontend.

### Tecnologías utilizadas

| Tecnología | Versión | Rol |
|---|---|---|
| Java + Spring Boot | 17 / 3.2.5 | API REST |
| Micrometer + Prometheus Registry | — | Instrumentación de métricas |
| Prometheus | 2.51.2 | Recolección y almacenamiento |
| Grafana | 10.4.2 | Visualización y dashboards |
| Nginx | Alpine | Servidor del frontend |
| Docker Compose | — | Orquestación de servicios |

---

## Arquitectura del sistema

```
┌──────────────────┐     scrape /actuator/prometheus      ┌─────────────────┐
│  UrbanGlow API   │ ──────────────────────────────────►  │   Prometheus    │
│  Spring Boot     │                                       │   :9090         │
│  :8080           │                                       └────────┬────────┘
└──────────────────┘                                                │ datasource
        ▲                                                   ┌───────▼────────┐
        │ requests                                          │    Grafana     │
┌───────┴──────────┐                                       │    :3001       │
│    Frontend      │                                       └────────────────┘
│    Nginx :80     │
└──────────────────┘
```

Prometheus hace scraping del endpoint `/actuator/prometheus` de la API cada 15 segundos. Grafana consume Prometheus como datasource y renderiza los paneles del dashboard, que se provisiona automáticamente al iniciar los contenedores.

---

## Requisitos previos

- **Docker Desktop** instalado y en ejecución
- Puertos disponibles: `80`, `8080`, `9090`, `3001`
- Conexión a internet para la primera descarga de imágenes

---

## Instalación y ejecución

### Paso 1 — Clonar el repositorio

```powershell
git clone https://github.com/Adrilu22/Api-Automatizaci-n.git
cd Api-Automatizaci-n
```

### Paso 2 — Compilar el JAR de la API

La API está escrita en Java, por lo que es necesario compilar el código fuente antes de construir la imagen Docker. Se utiliza un contenedor Maven para no requerir instalación local:

```powershell
docker run --rm `
  -v "C:/ruta/absoluta/al/proyecto/api:/app" `
  -v "C:/Users/TU_USUARIO/.m2:/root/.m2" `
  -w /app --dns 8.8.8.8 `
  maven:3.9-eclipse-temurin-17-alpine `
  mvn clean package -DskipTests -q
```

> Reemplazar `C:/ruta/absoluta/al/proyecto/api` con la ruta real a la carpeta `api/` del proyecto.

### Paso 3 — Levantar todos los servicios

```powershell
docker-compose up -d --build
```

Esto inicia 4 contenedores: `api`, `frontend`, `prometheus` y `grafana`.

### Paso 4 — Verificar el estado de los servicios

```powershell
docker-compose ps
```

Todos deben aparecer en estado `running`. La API tarda aproximadamente 60 segundos en iniciar completamente.

### Paso 5 — Verificar el scraping en Prometheus

Abrir http://localhost:9090/targets — el target `spring-api` debe mostrar estado **UP**.

---

## URLs de acceso

| Servicio | URL | Credenciales |
|---|---|---|
| Dashboard de negocio | http://localhost | — |
| API REST | http://localhost:8080 | — |
| Métricas Prometheus raw | http://localhost:8080/metrics | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3001 | admin / admin123 |

---

## Endpoints de la API

Base URL: `http://localhost:8080`

### Endpoints de consulta

| Método | URL completa | Descripción |
|---|---|---|
| `GET` | `http://localhost:8080/` | Health check — estado del servicio y listado de endpoints |
| `GET` | `http://localhost:8080/api/datos` | Resumen de las 3 métricas de negocio (ventas, clientes, descuentos) |
| `GET` | `http://localhost:8080/api/ventas` | Lista completa de todas las ventas registradas |
| `GET` | `http://localhost:8080/api/clientes` | Lista completa de todos los clientes registrados |
| `GET` | `http://localhost:8080/api/lento` | Simula procesamiento pesado con latencia de 2–3 segundos |
| `GET` | `http://localhost:8080/metrics` | Expone métricas en formato Prometheus |

### Endpoints CRUD

| Método | URL completa | Descripción |
|---|---|---|
| `POST` | `http://localhost:8080/api/ventas` | Registra una nueva venta |
| `PUT` | `http://localhost:8080/api/ventas/{id}` | Actualiza el total y/o descuento de una venta existente |
| `PUT` | `http://localhost:8080/api/clientes/{id}/premium?premium=true\|false` | Activa o desactiva la membresía premium de un cliente |
| `DELETE` | `http://localhost:8080/api/ventas/{id}` | Cancela (elimina) una venta por ID |

---

### `POST /api/ventas` — Registrar venta

**Request body:**
```json
{
  "mes": "2026-05",
  "clienteId": 3,
  "total": 99.99,
  "descuentoAplicado": false
}
```

**Response 201:**
```json
{
  "mensaje": "Venta registrada exitosamente",
  "venta": { "id": 29, "mes": "2026-05", "clienteId": 3, "total": 99.99, "descuentoAplicado": false },
  "timestamp": "2026-05-26T10:00:00"
}
```

---

### `PUT /api/ventas/{id}` — Actualizar venta

**Request body:**
```json
{
  "total": 119.99,
  "descuentoAplicado": true
}
```

**Response 200:**
```json
{
  "mensaje": "Venta 5 actualizada",
  "venta": { "id": 5, "mes": "2026-02", "clienteId": 9, "total": 119.99, "descuentoAplicado": true },
  "timestamp": "2026-05-26T10:00:00"
}
```

**Response 404:** `{ "error": "Venta con id 99 no encontrada" }`

---

### `PUT /api/clientes/{id}/premium?premium=true` — Actualizar membresía

**Response 200:**
```json
{
  "mensaje": "Membresía premium activada",
  "cliente": { "id": 3, "nombre": "Isabella García", "premium": true },
  "timestamp": "2026-05-26T10:00:00"
}
```

**Response 404:** `{ "error": "Cliente con id 99 no encontrado" }`

---

### `DELETE /api/ventas/{id}` — Cancelar venta

**Response 200:** `{ "mensaje": "Venta 5 cancelada exitosamente" }`

**Response 404:** `{ "error": "Venta con id 99 no encontrada" }`

---

### Respuesta de `GET /api/datos`

Devuelve en un solo JSON las tres métricas de negocio:

```json
{
  "ventas_por_mes": {
    "2026-02": { "cantidad": 5, "ingresos": 524.96 },
    "2026-03": { "cantidad": 8, "ingresos": 554.95 },
    "2026-04": { "cantidad": 9, "ingresos": 799.94 },
    "2026-05": { "cantidad": 6, "ingresos": 649.95 }
  },
  "clientes_premium": {
    "total": 12,
    "premium": 4,
    "regulares": 8,
    "ratio_pct": 33.33
  },
  "descuentos_por_mes": {
    "2026-02": 2,
    "2026-03": 3,
    "2026-04": 4,
    "2026-05": 2
  },
  "timestamp": "2026-05-26T10:00:00"
}
```

---

## Métricas implementadas

### Métricas de rendimiento

| Métrica | Tipo | Descripción |
|---|---|---|
| `tienda_requests_total` | Counter | Total de requests por endpoint y estado (ok) |
| `tienda_request_duration_seconds` | Histogram | Distribución de latencia con percentiles p50, p95 y p99 |
| `tienda_active_requests` | Gauge | Número de requests siendo procesados simultáneamente |

Las métricas de rendimiento incluyen la etiqueta `endpoint` para filtrar por ruta en las queries PromQL.

### Métricas de negocio

| Métrica | Tipo | Etiqueta | Descripción |
|---|---|---|---|
| `tienda_ventas_mensuales` | Gauge | `mes` | Número de ventas registradas por mes |
| `tienda_ingresos_mensuales` | Gauge | `mes` | Ingresos totales en USD por mes |
| `tienda_descuentos_mensuales` | Gauge | `mes` | Descuentos aplicados por mes |
| `tienda_clientes_premium_total` | Gauge | — | Clientes con membresía premium activa |

> Las métricas de negocio se actualizan automáticamente en Prometheus cada vez que se registra, edita o cancela una venta, o se cambia la membresía de un cliente.

---

## Queries PromQL

Estas queries son las utilizadas en el dashboard de Grafana y pueden ejecutarse directamente en http://localhost:9090.

```promql
# Requests por segundo por endpoint (throughput)
sum(rate(tienda_requests_total[1m])) by (endpoint)

# Latencia promedio por endpoint
rate(tienda_request_duration_seconds_sum[1m]) / rate(tienda_request_duration_seconds_count[1m])

# Percentil 95 de latencia (p95)
histogram_quantile(0.95, sum(rate(tienda_request_duration_seconds_bucket[2m])) by (le, endpoint))

# Percentil 99 de latencia (p99)
histogram_quantile(0.99, sum(rate(tienda_request_duration_seconds_bucket[2m])) by (le, endpoint))

# Requests activos en tiempo real
tienda_active_requests

# Total acumulado de requests por endpoint
sum(tienda_requests_total) by (endpoint)

# Ventas por mes
tienda_ventas_mensuales

# Ingresos por mes
tienda_ingresos_mensuales

# Clientes premium activos
tienda_clientes_premium_total

# Descuentos aplicados por mes
tienda_descuentos_mensuales
```

---

## Dashboard de Grafana

El dashboard se provisiona automáticamente al iniciar los contenedores — no requiere configuración manual. Contiene **9 paneles** organizados en dos filas:

**Fila 1 — Rendimiento técnico**

| # | Panel | Tipo | Descripción |
|---|---|---|---|
| 1 | Requests por segundo | Gráfico de líneas | Throughput en tiempo real por endpoint |
| 2 | Latencia promedio | Gráfico de líneas | Tiempo de respuesta promedio por endpoint |
| 3 | Latencia p95 y p99 | Gráfico de líneas | Percentiles de latencia para detectar cuellos de botella |
| 4 | Tasa de errores | Gráfico de líneas | Porcentaje de requests con status diferente de `ok` |
| 5 | Requests activos | Stat | Gauge en tiempo real de requests en procesamiento |
| 6 | Total por endpoint | Bar gauge | Volumen acumulado de requests por ruta |

**Fila 2 — Métricas de negocio**

| # | Panel | Tipo | Descripción |
|---|---|---|---|
| 7 | Ventas por mes | Bar gauge | Número de ventas registradas por mes (Feb–May 2026) |
| 8 | Clientes premium | Stat | Total de clientes con membresía premium activa |
| 9 | Descuentos por mes | Bar gauge | Descuentos aplicados en cada mes |

---

## Dashboard de negocio (frontend)

El frontend en `http://localhost` incluye un dashboard interactivo con las siguientes funcionalidades:

- **Filtro por período** — pill buttons para ver datos de Febrero, Marzo, Abril o Mayo 2026
- **KPIs en tiempo real** — Total ventas, Clientes premium, Ventas con descuento
- **Tabla de resumen mensual** — ventas e ingresos por mes con barra de volumen
- **Registro individual de ventas** — tabla con todas las ventas y 3 acciones CRUD:
  - **+ Nueva Venta** → formulario modal para registrar una venta (`POST /api/ventas`)
  - **Editar** → modal pre-cargado para modificar total y descuento (`PUT /api/ventas/{id}`)
  - **Cancelar** → elimina la venta con confirmación (`DELETE /api/ventas/{id}`)
- **Gestión de membresías** — lista de los 12 clientes con botón para dar o quitar premium (`PUT /api/clientes/{id}/premium`)
- **Descuentos por mes** — barras de progreso con conteo de ventas con descuento
- **Panel de observabilidad** — accesos directos a Prometheus, Grafana y métricas raw

---

## Script de tráfico sintético

El script genera requests automáticos a los endpoints de consulta para simular un patrón de tráfico realista y poblar las métricas en Grafana.

```powershell
# Ejecutar desde la raíz del proyecto
.\scripts\generate_traffic.ps1
```

Distribución de tráfico configurada:

| Endpoint | Descripción | Peso |
|---|---|---|
| `/` | Health check | 25% |
| `/api/datos` | Métricas de negocio | 50% |
| `/api/lento` | Procesamiento lento | 25% |

---

## Estructura del proyecto

```
Api-Automatizaci-n/
├── docker-compose.yml                  # Orquestación de los 4 servicios
├── README.md
├── .gitignore
│
├── api/                                # API REST — Spring Boot (Java 17)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/tiendamoda/
│       ├── config/
│       │   ├── CorsConfig.java         # CORS: GET, POST, PUT, DELETE, OPTIONS
│       │   ├── MetricsConfig.java      # Gauges: requests activos + métricas de negocio
│       │   └── OpenApiConfig.java      # Metadatos de la API
│       ├── controller/
│       │   ├── HealthController.java   # GET /
│       │   ├── DataController.java     # Todos los endpoints /api/*
│       │   └── MetricsController.java  # GET /metrics
│       ├── model/
│       │   ├── Venta.java              # id, mes, clienteId, total, descuentoAplicado
│       │   ├── VentaRequest.java       # Body para POST /api/ventas
│       │   ├── VentaUpdateRequest.java # Body para PUT /api/ventas/{id}
│       │   ├── Cliente.java            # id, nombre, premium
│       │   ├── DescuentoCampana.java   # id, mes, descripcion, fechaExpiracion
│       │   └── DescuentoCampanaRequest.java
│       └── repository/
│           └── TiendaRepository.java   # 28 ventas y 12 clientes en memoria + CRUD
│
├── frontend/                           # Dashboard interactivo — Nginx + HTML/JS/Tailwind
│   ├── Dockerfile
│   └── index.html                      # KPIs, filtro por mes, CRUD de ventas y membresías
│
├── prometheus/
│   └── prometheus.yml                  # Scraping cada 15s
│
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/prometheus.yml  # Datasource auto-provisionado
│   │   └── dashboards/dashboards.yml
│   └── dashboards/
│       └── api-dashboard.json          # 9 paneles: 6 de rendimiento + 3 de negocio
│
└── scripts/
    └── generate_traffic.ps1            # Generador de tráfico sintético (PowerShell)
```

---

## Gestión de contenedores

```powershell
# Estado de todos los servicios
docker-compose ps

# Logs en tiempo real (todos los servicios)
docker-compose logs -f

# Logs de un servicio específico
docker-compose logs -f api

# Detener todos los servicios (conserva volúmenes)
docker-compose down

# Reinicio limpio — elimina volúmenes y reconstruye desde cero
docker-compose down -v
docker-compose up -d --build
```

---

## Validación antes de la demostración

```powershell
# 1. Recompilar el JAR con los últimos cambios del código Java
docker run --rm `
  -v "C:/Users/adria/Desktop/Automatizacion/Api-Automatizaci-n/api:/app" `
  -v "C:/Users/adria/.m2:/root/.m2" `
  -w /app --dns 8.8.8.8 `
  maven:3.9-eclipse-temurin-17-alpine `
  mvn clean package -DskipTests -q

# 2. Reinicio limpio y reconstrucción de imágenes
docker-compose down -v
docker-compose up -d --build

# 3. Verificar que los 4 servicios están en estado running
docker-compose ps

# 4. Esperar ~60 segundos y confirmar que la API responde
#    Abrir: http://localhost:8080

# 5. Confirmar scraping activo en Prometheus
#    Abrir: http://localhost:9090/targets  →  spring-api debe estar UP

# 6. Generar tráfico sintético
.\scripts\generate_traffic.ps1

# 7. Probar las operaciones CRUD desde el dashboard de negocio
#    Abrir: http://localhost
#    - Registrar una nueva venta con el botón "+ Nueva Venta"
#    - Editar el total de una venta con el botón "Editar"
#    - Cancelar una venta con el botón "Cancelar"
#    - Cambiar la membresía de un cliente en "Gestión de Membresías"

# 8. Verificar las métricas en Grafana
#    Abrir: http://localhost:3001  →  usuario: admin  |  contraseña: admin123
```
