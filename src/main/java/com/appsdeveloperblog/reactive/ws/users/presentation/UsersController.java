package com.appsdeveloperblog.reactive.ws.users.presentation;

import com.appsdeveloperblog.reactive.ws.users.application.UsersService;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.UserRest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@Validated
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @PostMapping
    public Mono<ResponseEntity<UserRest>> createUser(@RequestBody @Valid Mono<CreateUserRequest> createUserRequest) {
        Mono<UserRest> userCreated = this.usersService.createUser(createUserRequest);

        return userCreated.map(userRest ->
                ResponseEntity
                        .status(HttpStatus.CREATED)
                        .location(URI.create("/users/" + userRest.getId()))
                        .body(userRest)
        );
    }

    @GetMapping("/{userId}")
    @PreAuthorize("authentication.principal.equals(#userIdRequest.toString()) or hasRole('ROLE_ADMIN')")
    public Mono<UserRest> getUserById(@PathVariable("userId") UUID userIdRequest,
                                      @RequestParam(name="include", required = false) String include,
                                      @RequestHeader(name="Authorization") String jwt
    ) {

        return this.usersService.getUserById(userIdRequest, include, jwt);

    }

    @GetMapping
    public Flux<UserRest> findAllUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "limit", defaultValue = "10") int limit) {

        return this.usersService.findAllBy(page, limit);

    }

    @GetMapping(value = "/v1/users/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UserRest> findAllStreams() {

        return this.usersService.streamUsers();

    }
}
