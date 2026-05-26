package com.tiendamoda.controller;

import com.tiendamoda.model.DescuentoCampanaRequest;
import com.tiendamoda.model.VentaRequest;
import com.tiendamoda.model.VentaUpdateRequest;
import com.tiendamoda.repository.TiendaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(name = "Datos", description = "Métricas de negocio de la tienda UrbanGlow")
@RestController
@RequestMapping("/api")
public class DataController {

    private static final List<String> MESES = List.of("2026-02", "2026-03", "2026-04", "2026-05");

    private final TiendaRepository repository;
    private final MeterRegistry registry;
    private final AtomicInteger activeRequests;
    private final Random random = new Random();

    public DataController(TiendaRepository repository, MeterRegistry registry,
                          AtomicInteger activeRequestsGauge) {
        this.repository = repository;
        this.registry = registry;
        this.activeRequests = activeRequestsGauge;
    }

    @Operation(
        summary = "Datos de negocio",
        description = "Devuelve las 3 métricas de negocio: ventas por mes (Feb–May 2026), " +
                      "clientes premium y descuentos aplicados por mes."
    )
    @GetMapping("/datos")
    public Map<String, Object> datos() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            Map<String, Map<String, Object>> ventasPorMes = new LinkedHashMap<>();
            Map<String, Long> descuentosPorMes = new LinkedHashMap<>();
            for (String mes : MESES) {
                ventasPorMes.put(mes, Map.of(
                        "cantidad", repository.countVentasByMes(mes),
                        "ingresos", Math.round(repository.sumIngresosByMes(mes) * 100.0) / 100.0
                ));
                descuentosPorMes.put(mes, repository.countDescuentosByMes(mes));
            }

            long totalClientes = repository.findAllClientes().size();
            long premium       = repository.countClientesPremium();

            registry.counter("tienda_requests", "endpoint", "/api/datos", "status", "ok").increment();
            return Map.of(
                    "ventas_por_mes",     ventasPorMes,
                    "clientes_premium",   Map.of(
                            "total",      totalClientes,
                            "premium",    premium,
                            "regulares",  totalClientes - premium,
                            "ratio_pct",  Math.round((double) premium / totalClientes * 100.0 * 100.0) / 100.0
                    ),
                    "descuentos_por_mes", descuentosPorMes,
                    "timestamp",          LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/datos")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Listar ventas", description = "Devuelve todas las ventas registradas.")
    @GetMapping("/ventas")
    public Map<String, Object> listarVentas() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            registry.counter("tienda_requests", "endpoint", "GET /api/ventas", "status", "ok").increment();
            return Map.of("ventas", repository.findAllVentas(), "timestamp", LocalDateTime.now().toString());
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "GET /api/ventas")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Listar clientes", description = "Devuelve todos los clientes registrados.")
    @GetMapping("/clientes")
    public Map<String, Object> listarClientes() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            registry.counter("tienda_requests", "endpoint", "GET /api/clientes", "status", "ok").increment();
            return Map.of("clientes", repository.findAllClientes(), "timestamp", LocalDateTime.now().toString());
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "GET /api/clientes")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(
        summary = "Registrar venta",
        description = "Registra una nueva venta. Campos requeridos: mes (formato YYYY-MM), clienteId, total, descuentoAplicado."
    )
    @PostMapping("/ventas")
    public ResponseEntity<Object> crearVenta(@RequestBody VentaRequest req) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            if (req.mes() == null || req.mes().isBlank() || req.total() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "mes y total son obligatorios; total debe ser mayor a 0",
                        "timestamp", LocalDateTime.now().toString()
                ));
            }
            var nueva = repository.saveVenta(req);
            registry.counter("tienda_requests", "endpoint", "POST /api/ventas", "status", "ok").increment();
            return ResponseEntity.status(201).body(Map.of(
                    "mensaje", "Venta registrada exitosamente",
                    "venta",   nueva,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "POST /api/ventas")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(
        summary = "Actualizar estado premium de cliente",
        description = "Activa o desactiva la membresía premium de un cliente. Parámetro: premium=true|false."
    )
    @PutMapping("/clientes/{id}/premium")
    public ResponseEntity<Object> actualizarPremium(@PathVariable int id,
                                                    @RequestParam boolean premium) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            return repository.updateClientePremium(id, premium)
                    .map(c -> {
                        registry.counter("tienda_requests", "endpoint", "PUT /api/clientes/{id}/premium", "status", "ok").increment();
                        return ResponseEntity.ok((Object) Map.of(
                                "mensaje",   premium ? "Membresía premium activada" : "Membresía premium desactivada",
                                "cliente",   c,
                                "timestamp", LocalDateTime.now().toString()
                        ));
                    })
                    .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                            "error",     "Cliente con id " + id + " no encontrado",
                            "timestamp", LocalDateTime.now().toString()
                    )));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "PUT /api/clientes/{id}/premium")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Actualizar venta", description = "Actualiza el total y el descuento de una venta existente.")
    @PutMapping("/ventas/{id}")
    public ResponseEntity<Object> actualizarVenta(@PathVariable int id,
                                                  @RequestBody VentaUpdateRequest req) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            if (req.total() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "total debe ser mayor a 0",
                        "timestamp", LocalDateTime.now().toString()
                ));
            }
            return repository.updateVenta(id, req)
                    .map(v -> {
                        registry.counter("tienda_requests", "endpoint", "PUT /api/ventas/{id}", "status", "ok").increment();
                        return ResponseEntity.ok((Object) Map.of(
                                "mensaje",   "Venta " + id + " actualizada",
                                "venta",     v,
                                "timestamp", LocalDateTime.now().toString()
                        ));
                    })
                    .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                            "error",     "Venta con id " + id + " no encontrada",
                            "timestamp", LocalDateTime.now().toString()
                    )));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "PUT /api/ventas/{id}")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(
        summary = "Cancelar venta",
        description = "Elimina una venta por su ID. Devuelve 404 si la venta no existe."
    )
    @DeleteMapping("/ventas/{id}")
    public ResponseEntity<Object> eliminarVenta(@PathVariable int id) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            boolean eliminada = repository.deleteVenta(id);
            if (eliminada) {
                registry.counter("tienda_requests", "endpoint", "DELETE /api/ventas/{id}", "status", "ok").increment();
                return ResponseEntity.ok(Map.of(
                        "mensaje",   "Venta " + id + " cancelada exitosamente",
                        "timestamp", LocalDateTime.now().toString()
                ));
            }
            return ResponseEntity.status(404).body(Map.of(
                    "error",     "Venta con id " + id + " no encontrada",
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "DELETE /api/ventas/{id}")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Listar campañas de descuento", description = "Devuelve todas las campañas de descuento registradas.")
    @GetMapping("/descuentos")
    public Map<String, Object> listarDescuentos() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            registry.counter("tienda_requests", "endpoint", "GET /api/descuentos", "status", "ok").increment();
            return Map.of("campanas", repository.findAllCampanas(), "timestamp", LocalDateTime.now().toString());
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "GET /api/descuentos")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Crear campaña de descuento", description = "Registra una nueva campaña de descuento para un mes. fechaExpiracion opcional (YYYY-MM-DD).")
    @PostMapping("/descuentos")
    public ResponseEntity<Object> crearCampana(@RequestBody DescuentoCampanaRequest req) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            if (req.mes() == null || req.mes().isBlank() || req.descripcion() == null || req.descripcion().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "mes y descripcion son obligatorios",
                        "timestamp", LocalDateTime.now().toString()
                ));
            }
            var nueva = repository.saveCampana(req);
            registry.counter("tienda_requests", "endpoint", "POST /api/descuentos", "status", "ok").increment();
            return ResponseEntity.status(201).body(Map.of(
                    "mensaje", "Campaña registrada exitosamente",
                    "campana", nueva,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "POST /api/descuentos")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Eliminar campaña de descuento", description = "Elimina una campaña por su ID. Devuelve 404 si no existe.")
    @DeleteMapping("/descuentos/{id}")
    public ResponseEntity<Object> eliminarCampana(@PathVariable int id) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            boolean eliminada = repository.deleteCampana(id);
            if (eliminada) {
                registry.counter("tienda_requests", "endpoint", "DELETE /api/descuentos/{id}", "status", "ok").increment();
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Campaña " + id + " eliminada",
                        "timestamp", LocalDateTime.now().toString()
                ));
            }
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Campaña con id " + id + " no encontrada",
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "DELETE /api/descuentos/{id}")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(
        summary = "Simulación de procesamiento lento",
        description = "Introduce una latencia artificial de 2 a 3 segundos para observar métricas de latencia en Grafana (p95, p99)."
    )
    @GetMapping("/lento")
    public Map<String, Object> lento() throws InterruptedException {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            long delay = 2000L + (long) (random.nextDouble() * 1000L);
            Thread.sleep(delay);

            registry.counter("tienda_requests", "endpoint", "/api/lento", "status", "ok").increment();
            return Map.of(
                    "mensaje",               "Procesamiento completado",
                    "tiempo_procesamiento_ms", delay,
                    "timestamp",             LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/lento")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }
}
