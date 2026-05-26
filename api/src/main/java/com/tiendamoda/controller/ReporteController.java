package com.tiendamoda.controller;

import com.tiendamoda.model.Producto;
import com.tiendamoda.repository.TiendaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Tag(name = "Reporte", description = "Reportes de inventario y estadísticas")
@RestController
@RequestMapping("/api/reporte")
public class ReporteController {

    private final TiendaRepository repository;
    private final MeterRegistry registry;
    private final AtomicInteger activeRequests;

    public ReporteController(TiendaRepository repository, MeterRegistry registry,
                             AtomicInteger activeRequestsGauge) {
        this.repository = repository;
        this.registry = registry;
        this.activeRequests = activeRequestsGauge;
    }

    @Operation(summary = "Reporte completo de inventario")
    @GetMapping("/inventario")
    public Map<String, Object> inventario() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            List<Producto> todos = repository.findAll();
            long disponibles = todos.stream().filter(Producto::disponible).count();
            long agotados = todos.size() - disponibles;
            double valorTotal = todos.stream().mapToDouble(p -> p.precio() * p.stock()).sum();
            double precioPromedio = todos.stream().mapToDouble(Producto::precio).average().orElse(0);

            registry.counter("tienda_requests", "endpoint", "/api/reporte/inventario", "status", "ok").increment();
            return Map.of(
                    "total_productos", todos.size(),
                    "disponibles", disponibles,
                    "agotados", agotados,
                    "valor_inventario", Math.round(valorTotal * 100.0) / 100.0,
                    "precio_promedio", Math.round(precioPromedio * 100.0) / 100.0,
                    "timestamp", LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/reporte/inventario")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Reporte de productos agrupados por categoría")
    @GetMapping("/categorias")
    public Map<String, Object> porCategorias() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            List<Producto> todos = repository.findAll();
            var porCategoria = todos.stream().collect(
                    Collectors.groupingBy(Producto::categoria,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    lista -> Map.of(
                                            "total", lista.size(),
                                            "disponibles", lista.stream().filter(Producto::disponible).count(),
                                            "precio_promedio", Math.round(lista.stream().mapToDouble(Producto::precio).average().orElse(0) * 100.0) / 100.0
                                    )
                            )
                    )
            );

            registry.counter("tienda_requests", "endpoint", "/api/reporte/categorias", "status", "ok").increment();
            return Map.of(
                    "total_categorias", porCategoria.size(),
                    "categorias", porCategoria,
                    "timestamp", LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/reporte/categorias")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }
}
