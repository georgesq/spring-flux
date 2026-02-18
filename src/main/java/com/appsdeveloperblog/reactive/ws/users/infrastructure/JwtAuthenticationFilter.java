package com.appsdeveloperblog.reactive.ws.users.infrastructure;

import com.appsdeveloperblog.reactive.ws.users.service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractToken(exchange);

        if(token==null) return chain.filter(exchange);

        return validateToken(token)
                .flatMap(isValid-> isValid ? authenticateAndContinue(token, exchange, chain)
                        :handleInvalidToken(exchange));
    }

    private Mono<Void> authenticateAndContinue(String token, ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.just(jwtService.extractTokenSubject(token))
                .flatMap(subject-> {
                    Authentication auth = new UsernamePasswordAuthenticationToken(subject, null,
                            Collections.emptyList());

                    return chain
                            .filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                });
    }

    private Mono<Void> handleInvalidToken(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private String extractToken(ServerWebExchange exchange) {
        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return null;
    }

    private Mono<Boolean> validateToken(String token) {
        return jwtService.validateJwt(token);
    }

}
