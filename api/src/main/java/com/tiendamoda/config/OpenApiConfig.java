package com.tiendamoda.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UrbanGlow — API de Tienda de Moda")
                        .description("API REST para gestión de productos, carrito de compras y reportes de inventario. Instrumentada con métricas Prometheus.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("UrbanGlow Dev")
                                .email("dev@urbanglow.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Docker")
                ));
    }
}
