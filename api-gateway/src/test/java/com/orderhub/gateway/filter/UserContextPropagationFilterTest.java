package com.orderhub.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextPropagationFilterTest {

    private final UserContextPropagationFilter filter = new UserContextPropagationFilter();

    @Test
    void addsUserIdEmailAndRolesHeadersWhenAuthenticated() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .claim("sub", "user-123")
                .claim("email", "user-123@example.com")
                .claim("roles", java.util.List.of("CUSTOMER", "SELLER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null);
        authentication.setAuthenticated(true);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            ServerWebExchange mutatedExchange = invocation.getArgument(0);
            ServerHttpRequest request = mutatedExchange.getRequest();
            assertThat(request.getHeaders().getFirst("X-User-Id")).isEqualTo("user-123");
            assertThat(request.getHeaders().getFirst("X-User-Email")).isEqualTo("user-123@example.com");
            assertThat(request.getHeaders().getFirst("X-User-Roles")).isEqualTo("CUSTOMER,SELLER");
            return Mono.empty();
        });

        StepVerifier.create(
                        filter.filter(exchange, chain)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .verifyComplete();
    }

    @Test
    void doesNotAddHeadersWhenNotAuthenticated() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/payments/webhooks/stripe"));

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            ServerWebExchange passedExchange = invocation.getArgument(0);
            ServerHttpRequest request = passedExchange.getRequest();
            assertThat(request.getHeaders().getFirst("X-User-Id")).isNull();
            assertThat(request.getHeaders().getFirst("X-User-Email")).isNull();
            assertThat(request.getHeaders().getFirst("X-User-Roles")).isNull();
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}
