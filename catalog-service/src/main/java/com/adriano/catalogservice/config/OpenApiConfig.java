package com.adriano.catalogservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogServiceOpenApi(@Value("${app.public-base-url:}") String publicBaseUrl) {
        OpenAPI openApi = new OpenAPI();
        if (!publicBaseUrl.isBlank()) {
            // Aponta o "Try it out" do Swagger UI para a rota real e
            // autenticada da API através do api-gateway, não para o
            // caminho interno usado só para servir a documentação.
            openApi.addServersItem(new Server().url(publicBaseUrl));
        }
        return openApi;
    }
}
