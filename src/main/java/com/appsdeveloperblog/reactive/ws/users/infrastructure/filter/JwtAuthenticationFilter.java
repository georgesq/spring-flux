package com.appsdeveloperblog.reactive.ws.users.infrastructure.filter;

import com.appsdeveloperblog.reactive.ws.users.application.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var token = extractToken(exchange);

        if (token == null) {
            return chain.filter(exchange);
        }

        return validateToken(token)
                .flatMap(isValid -> isValid ?
                        authenticateAndContinue(token, exchange, chain)
                        : handleInvalidToken(exchange));
    }

    private Mono<Void> handleInvalidToken(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    private Mono<Void> authenticateAndContinue(String token, ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.just(this.jwtService.extractTokenSubject(token))
                .flatMap(subject -> {;

                    var auth = new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());

                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

                });
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }

    private Mono<Boolean> validateToken(String token) {
        return this.jwtService.validateToken(token);

    }

}
