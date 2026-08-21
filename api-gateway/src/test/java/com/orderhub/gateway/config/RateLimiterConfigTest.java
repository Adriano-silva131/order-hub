package com.orderhub.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Instant;

class RateLimiterConfigTest {

    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    void resolvesJwtSubjectWhenAuthenticated() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .claim("sub", "user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null);
        authentication.setAuthenticated(true);

        StepVerifier.create(
                        config.userKeyResolver().resolve(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .expectNext("user-123")
                .verifyComplete();
    }

    @Test
    void fallsBackToRemoteAddressWhenNotAuthenticated() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .remoteAddress(new InetSocketAddress("203.0.113.5", 12345))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(config.userKeyResolver().resolve(exchange))
                .expectNext("203.0.113.5")
                .verifyComplete();
    }
}
