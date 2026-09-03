package com.orderhub.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class GatewayLoggingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange)
                .doOnSuccess(unused -> logIfErrorResponse(exchange, method, path))
                .doOnError(ex -> log.error("Gateway error handling {} {}", method, path, ex));
    }

    private void logIfErrorResponse(ServerWebExchange exchange, String method, String path) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        if (status == null) {
            return;
        }
        if (status.is5xxServerError()) {
            log.error("Gateway response {} {} -> {}", method, path, status.value());
        } else if (status.is4xxClientError()) {
            log.warn("Gateway response {} {} -> {}", method, path, status.value());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
