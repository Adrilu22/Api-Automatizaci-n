package com.tiendamoda.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(name = "General", description = "Estado del servicio y endpoints disponibles")
@RestController
public class HealthController {

    private final MeterRegistry registry;
    private final AtomicInteger activeRequests;

    public HealthController(MeterRegistry registry, AtomicInteger activeRequestsGauge) {
        this.registry = registry;
        this.activeRequests = activeRequestsGauge;
    }

    @Operation(summary = "Estado del servicio y listado de endpoints disponibles")
    @GetMapping("/")
    public Map<String, Object> health() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            registry.counter("tienda_requests", "endpoint", "/", "status", "ok").increment();
            return Map.of(
                    "status", "UP",
                    "service", "tienda-moda-api",
                    "version", "1.0.0",
                    "timestamp", LocalDateTime.now().toString(),
                    "endpoints", Map.of(
                            "datos",      "/api/datos",
                            "lento",      "/api/lento",
                            "productos",  "/api/productos",
                            "categorias", "/api/categorias",
                            "buscar",     "/api/buscar?q={termino}",
                            "metricas",   "/metrics"
                    )
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/")
                    .description("Duración de requests al endpoint raíz")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }
}
