package com.orderhub.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = "management.health.redis.enabled=false")
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void publicHealthEndpointIsAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedRouteWithoutTokenIsUnauthorized() {
        webTestClient.get()
                .uri("/api/v1/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void docsRouteIsAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/docs/orders/v3/api-docs")
                .exchange()
                .expectStatus().value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void paymentWebhookRouteIsAccessibleWithoutAuthentication() {
        webTestClient.post()
                .uri("/api/v1/payments/webhooks/stripe")
                .exchange()
                .expectStatus().value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void checkoutRouteWithoutTokenIsUnauthorized() {
        webTestClient.post()
                .uri("/api/v1/payments/checkout")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void productCatalogIsAccessibleWithoutAuthentication() {
        // order-hub-store server-renders /catalogo for SEO — crawlers carry no JWT.
        webTestClient.get()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void productCreationWithoutTokenIsUnauthorized() {
        // Only GET on the catalog path is public — mutating routes stay authenticated.
        webTestClient.post()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
