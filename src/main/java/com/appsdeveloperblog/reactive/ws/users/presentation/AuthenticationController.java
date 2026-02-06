package com.appsdeveloperblog.reactive.ws.users.presentation;

import com.appsdeveloperblog.reactive.ws.users.application.AuthenticationService;
import com.appsdeveloperblog.reactive.ws.users.application.UsersService;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.AuthenticationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/v1/authenticate")
    public Mono<ResponseEntity<Object>> authenticate(@RequestBody Mono<AuthenticationRequest> authenticationRequest) {

        return authenticationRequest
                .flatMap(request -> authenticationService.authenticate(request.getEmail(), request.getPassword()))
                .map(authResponse -> ResponseEntity.ok()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.get("token"))
                        .header("UserId", authResponse.get("userId"))
                        .build());
    }

}
