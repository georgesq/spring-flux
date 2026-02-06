package com.appsdeveloperblog.reactive.ws.users.application;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface AuthenticationService {

    Mono<Map<String, String>> authenticate(String userName, String password);

}
