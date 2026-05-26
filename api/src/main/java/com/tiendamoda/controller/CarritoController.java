package com.tiendamoda.controller;

import com.tiendamoda.model.CarritoItem;
import com.tiendamoda.repository.TiendaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(name = "Carrito", description = "Gestión del carrito de compras")
@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final TiendaRepository repository;
    private final MeterRegistry registry;
    private final AtomicInteger activeRequests;

    private final ConcurrentHashMap<String, List<Map<String, Object>>> carritos = new ConcurrentHashMap<>();

    public CarritoController(TiendaRepository repository, MeterRegistry registry,
                             AtomicInteger activeRequestsGauge) {
        this.repository = repository;
        this.registry = registry;
        this.activeRequests = activeRequestsGauge;
    }

    @Operation(summary = "Agregar producto al carrito")
    @PostMapping("/agregar")
    public ResponseEntity<?> agregar(@RequestBody CarritoItem item) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            var producto = repository.findById(item.productoId());
            if (producto.isEmpty()) {
                registry.counter("tienda_requests", "endpoint", "/api/carrito/agregar", "status", "not_found").increment();
                return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado", "id", item.productoId()));
            }
            if (!producto.get().disponible()) {
                registry.counter("tienda_requests", "endpoint", "/api/carrito/agregar", "status", "agotado").increment();
                return ResponseEntity.badRequest().body(Map.of("error", "Producto agotado", "nombre", producto.get().nombre()));
            }

            var lineaCarrito = Map.<String, Object>of(
                    "productoId", item.productoId(),
                    "nombre", producto.get().nombre(),
                    "precio", producto.get().precio(),
                    "cantidad", item.cantidad(),
                    "subtotal", producto.get().precio() * item.cantidad()
            );
            carritos.computeIfAbsent(item.sesionId(), k -> new java.util.ArrayList<>()).add(lineaCarrito);

            registry.counter("tienda_requests", "endpoint", "/api/carrito/agregar", "status", "ok").increment();
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Producto agregado al carrito",
                    "sesionId", item.sesionId(),
                    "producto", producto.get().nombre(),
                    "cantidad", item.cantidad(),
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/carrito/agregar")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Ver resumen y total del carrito")
    @GetMapping("/total")
    public ResponseEntity<?> total(@RequestParam String sesionId) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            var items = carritos.getOrDefault(sesionId, List.of());
            double total = items.stream()
                    .mapToDouble(i -> (double) i.get("subtotal"))
                    .sum();

            registry.counter("tienda_requests", "endpoint", "/api/carrito/total", "status", "ok").increment();
            return ResponseEntity.ok(Map.of(
                    "sesionId", sesionId,
                    "items", items,
                    "total_items", items.size(),
                    "total_precio", Math.round(total * 100.0) / 100.0,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/carrito/total")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Vaciar carrito de una sesión")
    @DeleteMapping("/vaciar")
    public ResponseEntity<?> vaciar(@RequestParam String sesionId) {
        carritos.remove(sesionId);
        registry.counter("tienda_requests", "endpoint", "/api/carrito/vaciar", "status", "ok").increment();
        return ResponseEntity.ok(Map.of("mensaje", "Carrito vaciado", "sesionId", sesionId));
    }
}
