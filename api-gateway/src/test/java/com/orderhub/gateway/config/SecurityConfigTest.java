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
}
