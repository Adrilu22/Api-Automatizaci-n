# UrbanGlow — Sistema de Monitoreo y Observabilidad para API REST

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Prometheus](https://img.shields.io/badge/Prometheus-2.51-red)
![Grafana](https://img.shields.io/badge/Grafana-10.4-yellow)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

Sistema completo de observabilidad implementado sobre una API REST de tienda de moda. Incluye recolección de métricas con **Prometheus**, visualización con **Grafana** y despliegue totalmente contenerizado con **Docker Compose**.

---

## Información del estudiante

| Campo | Detalle |
|---|---|
| **Nombre completo** | Adriana Lucia Carreño Medina |
| **Correo institucional** | alcarrenom@libertadores.edu.co |
| **Repositorio** | https://github.com/Adrilu22/Api-Automatizaci-n |
| **Materia** | Herramientas y Visualización de Datos |

---

## Descripción del proyecto

**UrbanGlow** es una API REST para la gestión de una tienda de moda desarrollada con Spring Boot. El proyecto demuestra la implementación de un stack de monitoreo completo en el que la API expone métricas personalizadas en formato Prometheus (contadores, histogramas y gauges), Prometheus las recolecta cada 15 segundos y Grafana las visualiza en un dashboard con 6 paneles en tiempo real.

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
| Frontend (panel visual) | http://localhost | — |
| API REST | http://localhost:8080 | — |
| Métricas Prometheus raw | http://localhost:8080/metrics | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3001 | admin / admin123 |

---

## Endpoints de la API

La API expone **11 endpoints** organizados en 5 grupos funcionales.

### General

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/` | Health check — estado del servicio y listado de endpoints disponibles |
| `GET` | `/metrics` | Métricas en formato Prometheus (requerido por la actividad) |

### Productos

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/productos` | Lista todos los productos (filtro opcional `?categoria=`) |
| `GET` | `/api/datos` | Alias requerido equivalente a `/api/productos` |
| `GET` | `/api/categorias` | Lista todas las categorías del catálogo |

### Búsqueda

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/buscar?q={término}` | Búsqueda de productos con latencia simulada de 300–1200 ms |
| `GET` | `/api/lento` | Simula procesamiento pesado con latencia de 2–3 segundos |

### Carrito de compras

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/carrito/agregar` | Agrega un producto al carrito de una sesión |
| `GET` | `/api/carrito/total?sesionId=` | Devuelve el resumen y total del carrito |
| `DELETE` | `/api/carrito/vaciar?sesionId=` | Vacía el carrito de una sesión |

### Reportes

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/reporte/inventario` | Estadísticas globales del inventario |
| `GET` | `/api/reporte/categorias` | Productos agrupados por categoría con estadísticas |

---

## Métricas implementadas

### Métricas personalizadas

| Métrica | Tipo | Descripción |
|---|---|---|
| `tienda_requests_total` | Counter | Total de requests por endpoint y estado (ok, not_found, agotado) |
| `tienda_request_duration_seconds` | Histogram | Distribución de latencia con percentiles p50, p95 y p99 |
| `tienda_active_requests` | Gauge | Número de requests siendo procesados simultáneamente |

Todas las métricas incluyen la etiqueta `endpoint` para filtrar por ruta específica en las queries PromQL.

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
```

---

## Dashboard de Grafana

El dashboard se provisiona automáticamente al iniciar los contenedores — no requiere configuración manual. Contiene **6 paneles**:

| # | Panel | Tipo | Descripción |
|---|---|---|---|
| 1 | Requests por segundo | Gráfico de líneas | Throughput en tiempo real por endpoint |
| 2 | Latencia promedio | Gráfico de líneas | Tiempo de respuesta promedio por endpoint |
| 3 | Latencia p95 y p99 | Gráfico de líneas | Percentiles de latencia para detectar cuellos de botella |
| 4 | Tasa de errores | Gráfico de líneas | Porcentaje de requests con status diferente de `ok` |
| 5 | Requests activos | Stat | Gauge en tiempo real de requests en procesamiento |
| 6 | Total por endpoint | Bar gauge | Volumen acumulado de requests por ruta |

---

## Script de tráfico sintético

El script genera requests automáticos con pesos por endpoint para simular un patrón de tráfico realista.

```powershell
# Ejecutar desde la raíz del proyecto
.\scripts\generate_traffic.ps1
```

Distribución de tráfico configurada:

| Endpoint | Descripción | Peso |
|---|---|---|
| `/` | Health check | 15% |
| `/api/datos` | Datos principales | 20% |
| `/api/productos` | Catálogo | 20% |
| `/api/categorias` | Categorías | 10% |
| `/api/lento` | Endpoint lento | 5% |
| `/api/buscar?q=vestido` | Búsqueda 1 | 15% |
| `/api/buscar?q=negro` | Búsqueda 2 | 15% |

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
│       │   ├── CorsConfig.java         # Configuración de CORS
│       │   ├── MetricsConfig.java      # Registro del gauge de requests activos
│       │   └── OpenApiConfig.java      # Metadatos de Swagger UI
│       ├── controller/
│       │   ├── HealthController.java
│       │   ├── ProductoController.java
│       │   ├── BusquedaController.java
│       │   ├── CarritoController.java
│       │   ├── ReporteController.java
│       │   └── MetricsController.java  # Reenvío de /metrics a /actuator/prometheus
│       ├── model/
│       │   ├── Producto.java
│       │   ├── Categoria.java
│       │   ├── CarritoItem.java
│       │   └── ProductoRequest.java
│       └── repository/
│           └── TiendaRepository.java   # Repositorio en memoria con 15 productos
│
├── frontend/                           # Panel visual — Nginx + HTML/JS/Tailwind
│   ├── Dockerfile
│   └── index.html
│
├── prometheus/
│   └── prometheus.yml                  # Configuración de scraping (intervalo: 15s)
│
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/prometheus.yml  # Datasource auto-provisionado
│   │   └── dashboards/dashboards.yml
│   └── dashboards/
│       └── api-dashboard.json          # Dashboard con 6 paneles
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
# 1. Reinicio limpio
docker-compose down -v
docker-compose up -d --build

# 2. Verificar que los 4 servicios están en estado running
docker-compose ps

# 3. Esperar ~60 segundos y confirmar que la API responde
#    Abrir: http://localhost:8080

# 4. Confirmar scraping activo en Prometheus
#    Abrir: http://localhost:9090/targets  →  spring-api debe estar UP

# 5. Generar tráfico sintético
.\scripts\generate_traffic.ps1

# 6. Abrir Grafana y verificar el dashboard
#    Abrir: http://localhost:3001  →  usuario: admin  |  contraseña: admin123
```
