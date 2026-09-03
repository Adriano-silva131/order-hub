package com.orderhub.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class UserContextPropagationFilter implements GlobalFilter, Ordered {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String EMAIL_HEADER = "X-User-Email";
    private static final String ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> resolveExchange(exchange, ctx.getAuthentication()))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private ServerWebExchange resolveExchange(ServerWebExchange exchange, Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
            return exchange;
        }

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header(USER_ID_HEADER, jwt.getSubject());

        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        if (email != null && !email.isBlank()) {
            requestBuilder.header(EMAIL_HEADER, email);
        }

        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        if (roles != null && !roles.isEmpty()) {
            requestBuilder.header(ROLES_HEADER, String.join(",", roles));
        }

        return exchange.mutate().request(requestBuilder.build()).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
