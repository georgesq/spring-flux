package com.appsdeveloperblog.reactive.ws.users.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final Environment env;

    private SecretKey getSigningKey() {
        var secretKey = env.getProperty("token.secret");

        return Optional.ofNullable(secretKey)
                .map(tokenSecret -> tokenSecret.getBytes(StandardCharsets.UTF_8))
                .map(tokenSecretBytes -> Keys.hmacShaKeyFor(tokenSecretBytes))
                .orElseThrow(() -> new IllegalStateException("Token secret key is not configured properly"));

    }

    @Override
    public String generateToken(String subject) {

        return Jwts.builder().subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact()
                ;
    }

    @Override
    public Mono<Boolean> validateToken(String token) {
        return Mono.just(token)
                .map(this::parseToken)
                .map(claims -> !claims.getExpiration().before(new Date()))
                .onErrorReturn(false);
    }

    @Override
    public String extractTokenSubject(String token) {
        return this.parseToken(token).getSubject();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

}
