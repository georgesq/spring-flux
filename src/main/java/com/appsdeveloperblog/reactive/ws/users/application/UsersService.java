package com.appsdeveloperblog.reactive.ws.users.application;

import com.appsdeveloperblog.reactive.ws.users.presentation.model.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.UserRest;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsersService extends ReactiveUserDetailsService {

    Mono<UserRest> createUser(Mono<CreateUserRequest> createUserRequest);

    Mono<UserRest> getUserById(UUID userIdRequest, String include, String jwt);

    Flux<UserRest> findAllBy(int page, int limit);

    Flux<UserRest> streamUsers();
}
