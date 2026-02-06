package com.appsdeveloperblog.reactive.ws.users.infrastructure.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public Mono<ErrorResponse> handleDuplicateKeyException(DuplicateKeyException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("Duplicate Key")
                .type(URI.create("https://api.example.com/errors/duplicate-key"))
                .property("timestamp", Instant.now())
                .build();
        return Mono.just(errorResponse);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ErrorResponse> handleWebExchangeBindException(WebExchangeBindException ex) {
        String errorsMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError) {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        return fieldName+": "+errorMessage;
                    } else {
                        return error.getDefaultMessage();
                    }
                })
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, errorsMessage)
                .title("Bind exception")
                .type(URI.create("https://api.example.com/errors/bind-exception"))
                .property("timestamp", Instant.now())
                .build();
        return Mono.just(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Mono<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder(ex, HttpStatus.UNAUTHORIZED, ex.getMessage())
                .title("Unauthorized")
                .type(URI.create("https://api.example.com/errors/unauthorized"))
                .property("timestamp", Instant.now())
                .build();
        return Mono.just(errorResponse);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public Mono<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder(ex, HttpStatus.UNAUTHORIZED, ex.getMessage())
                .title("Unauthorized method")
                .type(URI.create("https://api.example.com/errors/unauthorized-method"))
                .property("timestamp", Instant.now())
                .build();
        return Mono.just(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage())
                .title("Generic error")
                .type(URI.create("https://api.example.com/errors/generic-error"))
                .property("timestamp", Instant.now())
                .build();
        return Mono.just(errorResponse);
    }
}
