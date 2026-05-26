package com.tiendamoda.controller;

import com.tiendamoda.model.Producto;
import com.tiendamoda.repository.TiendaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(name = "Búsqueda", description = "Búsqueda de productos y simulación de carga")
@RestController
@RequestMapping("/api")
public class BusquedaController {

    private final TiendaRepository repository;
    private final MeterRegistry registry;
    private final AtomicInteger activeRequests;
    private final Random random = new Random();

    public BusquedaController(TiendaRepository repository, MeterRegistry registry,
                              AtomicInteger activeRequestsGauge) {
        this.repository = repository;
        this.registry = registry;
        this.activeRequests = activeRequestsGauge;
    }

    @Operation(summary = "Simula procesamiento pesado (2-3 segundos de latencia)")
    @GetMapping("/lento")
    public Map<String, Object> lento() throws InterruptedException {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            long delay = 2000L + (long) (random.nextDouble() * 1000L);
            Thread.sleep(delay);

            registry.counter("tienda_requests", "endpoint", "/api/lento", "status", "ok").increment();

            return Map.of(
                    "mensaje", "Procesamiento pesado completado",
                    "tiempo_procesamiento_ms", delay,
                    "productos_procesados", repository.findAll().size(),
                    "timestamp", LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/lento")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Búsqueda de productos por nombre o categoría (latencia variable 300-1200ms)")
    @GetMapping("/buscar")
    public Map<String, Object> buscar(@RequestParam(defaultValue = "") String q) throws InterruptedException {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            // Simula búsqueda con carga variable en base de datos (300-1200ms)
            long delay = 300L + (long) (random.nextDouble() * 900L);
            Thread.sleep(delay);

            List<Producto> resultados = q.isBlank()
                    ? repository.findAll()
                    : repository.search(q);

            registry.counter("tienda_requests", "endpoint", "/api/buscar", "status", "ok").increment();

            return Map.of(
                    "query", q.isBlank() ? "*" : q,
                    "total_resultados", resultados.size(),
                    "tiempo_busqueda_ms", delay,
                    "resultados", resultados,
                    "timestamp", LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/buscar")
                    .description("Duración de búsquedas (incluye latencia simulada de BD)")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }
}
