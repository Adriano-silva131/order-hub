package com.orderhub.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    var authentication = ctx.getAuthentication();
                    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                        return Mono.just(jwt.getSubject());
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.just(
                        exchange.getRequest().getRemoteAddress() != null
                                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                                : "anonymous"
                ));
    }
}
