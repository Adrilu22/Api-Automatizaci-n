# UrbanGlow — API de Tienda de Moda con Monitoreo y Observabilidad

Stack completo de monitoreo usando **Spring Boot**, **Prometheus** y **Grafana**, desplegado con Docker Compose.

---

## Información del estudiante

| Campo | Valor |
|---|---|
| **Nombre completo** | Adriana Lucia Carreño Medina |
| **Correo** | alcarrenom@libertadores.edu.co |
| **Repositorio** | https://github.com/Adrilu22/Api-Automatizaci-n |

---

## Descripción de la API

**UrbanGlow** es una API REST para gestión de una tienda de moda. Permite consultar productos, buscar por categorías, gestionar un carrito de compras y obtener reportes de inventario. Está completamente instrumentada con métricas en formato Prometheus.

---

## Arquitectura del sistema

```
┌──────────────────┐     scrape /actuator/prometheus      ┌─────────────────┐
│  UrbanGlow API   │ ──────────────────────────────────►  │   Prometheus    │
│  Spring Boot     │                                       │   :9090         │
│  :8080           │                                       └────────┬────────┘
└──────────────────┘                                                │ datasource
        ▲                                                   ┌───────▼────────┐
        │ fetch                                             │    Grafana     │
┌───────┴──────────┐                                       │    :3001       │
│    Frontend      │                                       └────────────────┘
│    Nginx :80     │
└──────────────────┘
```

---

## Requisitos previos

- Docker Desktop instalado y corriendo
- Puertos libres: `80`, `8080`, `9090`, `3001`

---

## Instalación y ejecución paso a paso

### Paso 1 — Clonar el repositorio

```powershell
https://github.com/Adrilu22/Api-Automatizaci-n
cd Api-Automatizaci-n
```

### Paso 2 — Compilar el JAR de la API

El JAR debe compilarse antes de construir la imagen Docker. Ejecutar desde la carpeta `api/`:

```powershell
docker run --rm `
  -v "C:/ruta/al/proyecto/api:/app" `
  -v "C:/Users/TU_USUARIO/.m2:/root/.m2" `
  -w /app --dns 8.8.8.8 `
  maven:3.9-eclipse-temurin-17-alpine `
  mvn clean package -DskipTests -q
```

> Reemplazar `C:/ruta/al/proyecto/api` con la ruta absoluta real a la carpeta `api/`.

### Paso 3 — Levantar todos los servicios

```powershell
docker-compose up -d --build
```

### Paso 4 — Verificar que todo está corriendo

```powershell
docker-compose ps
```

Todos los servicios deben aparecer en estado `running`. La API tarda ~60 segundos en arrancar completamente.

### Paso 5 — Verificar scraping en Prometheus

Ir a http://localhost:9090/targets — el target `spring-api` debe mostrar estado **UP**.

---

## URLs de acceso

| Servicio | URL | Credenciales |
|---|---|---|
| Frontend (panel visual) | http://localhost | — |
| API REST | http://localhost:8080 | — |
| Métricas Prometheus raw | http://localhost:8080/ | — |
| Prometheus | http://localhost:9090 | — |
## Usuario Ingreso grafana
| **Grafana** | **http://localhost:3001** | **admin / admin123** |

---

## Endpoints de la API

### General

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/` | Health check — estado del servicio y listado de endpoints |
| GET | `/metrics` | Métricas en formato Prometheus (requerido) |

### Productos

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/productos` | Lista todos los productos (filtro opcional `?categoria=`) |
| GET | `/api/datos` | Alias requerido de `/api/productos` |
| GET | `/api/categorias` | Listar todas las categorías disponibles |

### Búsqueda

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/buscar?q={término}` | Búsqueda de productos (latencia simulada 300–1200ms) |
| GET | `/api/lento` | Simulación de procesamiento pesado (2–3 segundos) |

### Carrito

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/carrito/agregar` | Agregar producto al carrito de una sesión |
| GET | `/api/carrito/total?sesionId=` | Ver resumen y total del carrito |
| DELETE | `/api/carrito/vaciar?sesionId=` | Vaciar carrito de una sesión |

### Reporte

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/reporte/inventario` | Estadísticas globales del inventario |
| GET | `/api/reporte/categorias` | Productos agrupados por categoría con stats |

---

## Métricas implementadas

### Métricas personalizadas

| Métrica | Tipo | Descripción |
|---|---|---|
| `tienda_requests_total` | Counter | Total de requests por endpoint y status |
| `tienda_request_duration_seconds` | Histogram | Latencia con percentiles p50/p95/p99 |
| `tienda_active_requests` | Gauge | Requests siendo procesados en este momento |


---

## Queries PromQL útiles

```promql
# Requests por segundo por endpoint
sum(rate(tienda_requests_total[1m])) by (endpoint)

# Latencia promedio por endpoint
rate(tienda_request_duration_seconds_sum[1m]) / rate(tienda_request_duration_seconds_count[1m])

# Percentil 95 de latencia
histogram_quantile(0.95, sum(rate(tienda_request_duration_seconds_bucket[2m])) by (le, endpoint))

# Percentil 99 de latencia
histogram_quantile(0.99, sum(rate(tienda_request_duration_seconds_bucket[2m])) by (le, endpoint))

# Requests activos en este momento
tienda_active_requests

# Total de requests acumulados
sum(tienda_requests_total) by (endpoint)
```

---

## Dashboard de Grafana

El dashboard se provisiona automáticamente al iniciar los contenedores. Incluye **6 paneles**:

| Panel | Tipo | Métrica |
|---|---|---|
| Requests por segundo (throughput) | Gráfico de líneas | `rate(tienda_requests_total[1m])` |
| Latencia promedio por endpoint | Gráfico de líneas | `rate(..._sum) / rate(..._count)` |
| Latencia p95 y p99 | Gráfico de líneas | `histogram_quantile(0.95/0.99, ...)` |
| Tasa de errores HTTP | Gráfico de líneas | Requests con status != ok |
| Requests activos ahora | Stat | `tienda_active_requests` |
| Total requests por endpoint | Bar gauge | `sum(tienda_requests_total)` |

---

## Script de tráfico sintético

Genera requests automáticos con distribución de peso por endpoint para simular tráfico real.

```powershell
# Ejecutar desde la raíz del proyecto
.\scripts\generate_traffic.ps1
```

El script golpea los siguientes endpoints en loop:

| Endpoint | Peso (%) |
|---|---|
| `/` | 15% |
| `/api/datos` | 20% |
| `/api/productos` | 20% |
| `/api/categorias` | 10% |
| `/api/lento` | 5% |
| `/api/buscar?q=vestido` | 10% |
| `/api/buscar?q=negro` | 10% |

---

## Estructura del proyecto

```
Api-Automatizaci-n/
├── docker-compose.yml              # Orquestación de los 4 servicios
├── README.md                       # Este archivo
├── .gitignore
│
├── api/                            # API Spring Boot (Java 17)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/tiendamoda/
│       ├── config/
│       │   ├── CorsConfig.java
│       │   ├── MetricsConfig.java
│       │   └── OpenApiConfig.java
│       ├── controller/
│       │   ├── HealthController.java
│       │   ├── ProductoController.java
│       │   ├── BusquedaController.java
│       │   ├── CarritoController.java
│       │   ├── ReporteController.java
│       │   └── MetricsController.java
│       ├── model/
│       │   ├── Producto.java
│       │   ├── Categoria.java
│       │   ├── CarritoItem.java
│       │   └── ProductoRequest.java
│       └── repository/
│           └── TiendaRepository.java
│
├── frontend/                       # Panel visual (Nginx + HTML/JS)
│   ├── Dockerfile
│   └── index.html
│
├── prometheus/
│   └── prometheus.yml              # Configuración de scraping
│
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/prometheus.yml
│   │   └── dashboards/dashboards.yml
│   └── dashboards/
│       └── api-dashboard.json      # Dashboard auto-provisionado
│
└── scripts/
    └── generate_traffic.ps1        # Script de tráfico sintético (PowerShell)
```

---

## Gestión de contenedores

```powershell
# Ver estado de todos los servicios
docker-compose ps

# Ver logs en tiempo real
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f api

# Detener todos los servicios
docker-compose down

# Reinicio limpio (elimina volúmenes)
docker-compose down -v
docker-compose up -d --build
```

---

## Prueba final antes de la demostración

```powershell
# 1. Reinicio limpio desde cero
docker-compose down -v
docker-compose up -d --build

# 2. Verificar servicios
docker-compose ps

# 3. Confirmar que la API responde (esperar ~60s)
# Abrir: http://localhost:8080

# 4. Confirmar scraping en Prometheus
# Abrir: http://localhost:9090/targets  →  spring-api debe estar UP

# 5. Generar tráfico
.\scripts\generate_traffic.ps1

# 6. Ver dashboard en Grafana
# Abrir: http://localhost:3001  →  admin / admin123
```
