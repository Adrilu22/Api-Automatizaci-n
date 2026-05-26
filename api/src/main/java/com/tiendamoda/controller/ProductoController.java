package com.tiendamoda.controller;

import com.tiendamoda.model.Categoria;
import com.tiendamoda.model.Producto;
import com.tiendamoda.model.ProductoRequest;
import com.tiendamoda.repository.TiendaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(name = "Productos", description = "Gestión del catálogo de productos de moda")
@RestController
@RequestMapping("/api")
public class ProductoController {

    private final TiendaRepository repository;
    private final MeterRegistry registry;
    private final AtomicInteger activeRequests;

    public ProductoController(TiendaRepository repository, MeterRegistry registry,
                              AtomicInteger activeRequestsGauge) {
        this.repository = repository;
        this.registry = registry;
        this.activeRequests = activeRequestsGauge;
    }

    @Operation(summary = "Listar todos los productos (filtro opcional por categoría)")
    @GetMapping("/productos")
    public Map<String, Object> getProductos(
            @RequestParam(required = false) String categoria) {

        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            List<Producto> lista = (categoria != null && !categoria.isBlank())
                    ? repository.findByCategoria(categoria)
                    : repository.findAll();

            registry.counter("tienda_requests", "endpoint", "/api/productos", "status", "ok").increment();

            return Map.of(
                    "total", lista.size(),
                    "filtro", categoria != null ? categoria : "todos",
                    "productos", lista,
                    "timestamp", LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/productos")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Obtener producto por ID")
    @GetMapping("/productos/{id}")
    public ResponseEntity<?> getProductoById(@PathVariable int id) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            Optional<Producto> producto = repository.findById(id);
            if (producto.isEmpty()) {
                registry.counter("tienda_requests", "endpoint", "/api/productos/{id}", "status", "not_found").increment();
                return ResponseEntity.notFound().build();
            }
            registry.counter("tienda_requests", "endpoint", "/api/productos/{id}", "status", "ok").increment();
            return ResponseEntity.ok(Map.of(
                    "producto", producto.get(),
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/productos/{id}")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Agregar nuevo producto al catálogo")
    @PostMapping("/productos")
    public ResponseEntity<?> crearProducto(@RequestBody ProductoRequest req) {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            Producto nuevo = repository.save(req);
            registry.counter("tienda_requests", "endpoint", "/api/productos", "status", "created").increment();
            return ResponseEntity.status(201).body(Map.of(
                    "mensaje", "Producto creado correctamente",
                    "producto", nuevo,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/productos")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }

    @Operation(summary = "Alias de /api/productos (endpoint requerido)")
    @GetMapping("/datos")
    public Map<String, Object> getDatos() {
        return getProductos(null);
    }

    @Operation(summary = "Listar todas las categorías disponibles")
    @GetMapping("/categorias")
    public Map<String, Object> getCategorias() {
        activeRequests.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        try {
            List<Categoria> categorias = repository.findAllCategorias();
            registry.counter("tienda_requests", "endpoint", "/api/categorias", "status", "ok").increment();

            return Map.of(
                    "total", categorias.size(),
                    "categorias", categorias,
                    "timestamp", LocalDateTime.now().toString()
            );
        } finally {
            sample.stop(Timer.builder("tienda_request_duration_seconds")
                    .tag("endpoint", "/api/categorias")
                    .publishPercentileHistogram(true)
                    .register(registry));
            activeRequests.decrementAndGet();
        }
    }
}
