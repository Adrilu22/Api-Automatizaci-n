package com.tiendamoda.config;

import com.tiendamoda.repository.TiendaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    private static final List<String> MESES = List.of("2026-02", "2026-03", "2026-04", "2026-05");

    private final AtomicInteger activeRequests = new AtomicInteger(0);

    @Bean
    public AtomicInteger activeRequestsGauge(MeterRegistry registry) {
        Gauge.builder("tienda_active_requests", activeRequests, AtomicInteger::get)
                .description("Requests en proceso en este momento")
                .register(registry);
        return activeRequests;
    }

    @Bean
    public ApplicationRunner businessMetricsRegistrar(MeterRegistry registry, TiendaRepository repository) {
        return args -> {
            for (String mes : MESES) {
                Gauge.builder("tienda_ventas_mensuales", repository, r -> (double) r.countVentasByMes(mes))
                        .tag("mes", mes)
                        .description("Número de ventas por mes")
                        .register(registry);
                Gauge.builder("tienda_ingresos_mensuales", repository, r -> r.sumIngresosByMes(mes))
                        .tag("mes", mes)
                        .description("Ingresos totales por mes en USD")
                        .register(registry);
                Gauge.builder("tienda_descuentos_mensuales", repository, r -> (double) r.countDescuentosByMes(mes))
                        .tag("mes", mes)
                        .description("Descuentos aplicados por mes")
                        .register(registry);
            }
            Gauge.builder("tienda_clientes_premium_total", repository, r -> (double) r.countClientesPremium())
                    .description("Clientes con membresía premium activa")
                    .register(registry);
        };
    }
}
