package com.appsdeveloperblog.reactive.ws.users.application;

import reactor.core.publisher.Mono;

public interface JwtService {

    String generateToken(String subject);

    Mono<Boolean> validateToken(String token);

    String extractTokenSubject(String token);
}
