package com.tiendamoda.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    private final AtomicInteger activeRequests = new AtomicInteger(0);

    @Bean
    public AtomicInteger activeRequestsGauge(MeterRegistry registry) {
        Gauge.builder("tienda_active_requests", activeRequests, AtomicInteger::get)
                .description("Requests en proceso en este momento")
                .register(registry);
        return activeRequests;
    }
}
