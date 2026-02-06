package com.appsdeveloperblog.reactive.ws.users.infrastructure.repository;

import com.appsdeveloperblog.reactive.ws.users.model.entity.UsersEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsersRepository extends ReactiveCrudRepository<UsersEntity, UUID> {

    Flux<UsersEntity> findAllBy(Pageable pageable);

    Mono<UsersEntity> findByEmail(String username);

}
