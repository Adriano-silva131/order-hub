package com.orderhub.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
                        .pathMatchers("/docs/**").permitAll()
                        // Stripe/Mercado Pago call these directly — no app JWT, verified via
                        // gateway-specific signatures inside payment-service instead.
                        .pathMatchers("/api/v1/payments/webhooks/**").permitAll()
                        // Catalog browsing must be anonymous: the storefront (order-hub-store)
                        // server-renders /catalogo for SEO, and crawlers carry no JWT. Only GET
                        // is public — create/stock-mutation routes on the same path stay authenticated.
                        .pathMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }
}