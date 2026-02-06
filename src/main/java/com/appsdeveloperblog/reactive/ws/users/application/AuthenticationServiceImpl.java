package com.appsdeveloperblog.reactive.ws.users.application;

import com.appsdeveloperblog.reactive.ws.users.infrastructure.repository.UsersRepository;
import com.appsdeveloperblog.reactive.ws.users.model.entity.UsersEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final ReactiveAuthenticationManager authenticationManager;
    private final UsersRepository usersRepository;
    private final JwtService jwtService;

    @Override
    public Mono<Map<String, String>> authenticate(String userName, String password) {
        return this.authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(userName, password))
                .then(getUserDetails(userName))
                .map(this::createAuthResponse);
    }

    private Map<String, String> createAuthResponse(UsersEntity usersEntity) {
        return Map.of(
                "userId", usersEntity.getId().toString(),
                "token", this.jwtService.generateToken(usersEntity.getId().toString())
        );
    }

    private Mono<UsersEntity> getUserDetails(String userName) {
        return usersRepository.findByEmail(userName);
    }

}
