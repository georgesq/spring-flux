package com.appsdeveloperblog.reactive.ws.users.application;

import com.appsdeveloperblog.reactive.ws.users.infrastructure.repository.UsersRepository;
import com.appsdeveloperblog.reactive.ws.users.model.entity.UsersEntity;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.AlbumRest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.UserRest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;

    private final PasswordEncoder passwordEncoder;

    private final Sinks.Many<UserRest> usersSink;

    private final WebClient webClient;

    @Value("${base.url}")
    private String baseUrl;

    @Override
    public Mono<UserRest> createUser(Mono<CreateUserRequest> createUserRequestMono) {
        return createUserRequestMono
                .flatMap(this::convertToEntity)
                .flatMap(usersRepository::save)
                .mapNotNull(this::convertToRest)
                .doOnSuccess(this.usersSink::tryEmitNext);
    }

    @Override
    public Mono<UserRest> getUserById(UUID userIdRequest, String include, String jwt) {

        Objects.requireNonNull(userIdRequest);

        return this.usersRepository.findById(userIdRequest)
                .mapNotNull(this::convertToRest)
                .flatMap(user -> {
                    if (include != null && include.equals("albums")) {
                        return includeUserAlbums(user, jwt);
                    }

                    return Mono.just(user);
                });

    }

    private Mono<UserRest> includeUserAlbums(UserRest user, String jwt) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .port(8084)
                        .path("/v1/albums")
                        .queryParam("userId", user.getId())
                        .build()
                )
                .header("Authorization", jwt)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    return Mono.error(new RuntimeException("Albums not found for user " + user.getId()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    return Mono.error(new RuntimeException("Server error when retrieving albums for user " + user.getId()));
                })
                .bodyToFlux(AlbumRest.class)
                .collectList()
                .map(albums -> {

                    user.setAlbums(albums);

                    return user;

                })
                .onErrorResume(e -> {;
                    log.error("Error retrieving albums for user {}: {}", user.getId(), e.getMessage());
                    return Mono.just(user);
                });
    }

    @Override
    public Flux<UserRest> findAllBy(int page, int limit) {
        if (page > 0)
            page -= 1;

        Pageable pageable = PageRequest.of(page, limit);

        return this.usersRepository.findAllBy(pageable).map(this::convertToRest);

    }

    @Override
    public Flux<UserRest> streamUsers() {
        return this.usersSink.asFlux()
                .publish()
                .autoConnect();
    }

    private UserRest convertToRest(UsersEntity usersEntity) {
        UserRest userRest = new UserRest();
        userRest.setId(usersEntity.getId());
        userRest.setFirstName(usersEntity.getFirstName());
        userRest.setLastName(usersEntity.getLastName());
        userRest.setEmail(usersEntity.getEmail());
        return userRest;
    }

    private Mono<UsersEntity> convertToEntity(CreateUserRequest request) {
        return Mono.fromCallable(() -> {
            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setFirstName(request.getFirstName());
            usersEntity.setLastName(request.getLastName());
            usersEntity.setEmail(request.getEmail());
            usersEntity.setPassword(this.passwordEncoder.encode(request.getPassword()));

            return usersEntity;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return this.usersRepository.findByEmail(username).map(this::convertToUserDetails);
    }

    private UserDetails convertToUserDetails(UsersEntity usersEntity) {
        return User
                .withUsername(usersEntity.getEmail())
                .password(usersEntity.getPassword())
                .authorities(new ArrayList<>())
                .build();
    }

}
