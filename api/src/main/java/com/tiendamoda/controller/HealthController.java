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

    @Operation(
        summary = "Estado del servicio",
        description = "Verifica que la API esté activa y devuelve el listado completo de todos los endpoints disponibles con su método HTTP."
    )
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
                            "datos",             "GET    /api/datos",
                            "ventas",            "GET    /api/ventas",
                            "clientes",          "GET    /api/clientes",
                            "lento",             "GET    /api/lento",
                            "metricas",          "GET    /metrics",
                            "registrarVenta",    "POST   /api/ventas",
                            "actualizarPremium", "PUT    /api/clientes/{id}/premium?premium=true|false",
                            "cancelarVenta",     "DELETE /api/ventas/{id}"
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
