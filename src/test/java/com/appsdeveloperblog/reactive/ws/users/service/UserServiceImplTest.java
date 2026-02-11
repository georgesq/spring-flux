package com.appsdeveloperblog.reactive.ws.users.service;

import com.appsdeveloperblog.reactive.ws.users.data.UserEntity;
import com.appsdeveloperblog.reactive.ws.users.data.UserRepository;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.AlbumRest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.UserRest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Sinks.Many<UserRest> usersSink;

    @Mock
    private WebClient webClient;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        this.usersSink = Sinks.many().multicast().onBackpressureBuffer();
        this.userService = new UserServiceImpl(userRepository, passwordEncoder, usersSink, webClient);
    }

    @Test
    void testCreateUser_withValidRequest_thenUserCreatedAndReturnUserRest() {

        // Arrange
        String pwd = "123456789";
        CreateUserRequest createUserRequest = new CreateUserRequest(
                "Sergey",
                "Kargopolov",
                "sk@sk.com",
                pwd
        );
        var userEntity = new UserEntity(
                UUID.randomUUID(),
                createUserRequest.getFirstName(),
                createUserRequest.getLastName(),
                createUserRequest.getEmail(),
                BCrypt.hashpw(pwd, BCrypt.gensalt())
        );

        when(this.passwordEncoder.encode(Mockito.anyString())).thenReturn(
                BCrypt.hashpw(pwd, BCrypt.gensalt()));

        when(this.userRepository.save(Mockito.any(UserEntity.class))).thenReturn(
                Mono.just(userEntity));

        // Act
        var userCreated = this.userService.createUser(Mono.just(createUserRequest));

        // Assert
        StepVerifier.create(userCreated)
                .expectNextMatches(user ->
                        user.getId().equals(userEntity.getId()) &&
                                user.getFirstName().equals(createUserRequest.getFirstName()) &&
                                user.getLastName().equals(createUserRequest.getLastName()) &&
                                user.getEmail().equals(createUserRequest.getEmail())
                ).verifyComplete();

        Mockito.verify(userRepository, Mockito.times(1))
                .save(Mockito.any(UserEntity.class));
    }

    @Test
    void testGetUserById_withValidId_thenReturnUserRest() {
        // Arrange
        UUID userId = UUID.randomUUID();
        var userEntity = new UserEntity(
                userId,
                "Sergey",
                "Kargopolov",
                "sk@sk.com",
                BCrypt.hashpw("123456789", BCrypt.gensalt())
        );

        when(this.userRepository.findById(Mockito.any(UUID.class)))
                .thenReturn(Mono.just(userEntity));

        // Act
        var userFinded = this.userService.getUserById(userId, null, null);

        // Assert
        StepVerifier.create(userFinded)
                .expectNextMatches(user ->
                        user.getId().equals(userEntity.getId()) &&
                                user.getFirstName().equals(userEntity.getFirstName()) &&
                                user.getLastName().equals(userEntity.getLastName()) &&
                                user.getEmail().equals(userEntity.getEmail())
                ).verifyComplete();

        Mockito.verify(userRepository, Mockito.times(1))
                .findById(Mockito.any(UUID.class));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testGetUserById_WithIncludeAlbums_ReturnsAlbums() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String jwt = "valid-jwt";

        // 1. Setup UserEntity
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setFirstName("Sergey");
        userEntity.setLastName("Kargopolov");
        userEntity.setEmail("test@test.com");
        userEntity.setPassword("encodedPass");

        // 2. Mock repository response
        when(userRepository.findById(userId)).thenReturn(Mono.just(userEntity));

        // 3. Mock WebClient response with albums
        WebClient.RequestHeadersUriSpec getSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(getSpec);
        when(getSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.header(eq("Authorization"), eq(jwt))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        // Explicitly return test albums
        AlbumRest album1 = new AlbumRest("album1", "Summer Vacation");
        AlbumRest album2 = new AlbumRest("album2", "Family Reunion");
        when(responseSpec.bodyToFlux(AlbumRest.class))
                .thenReturn(Flux.just(album1, album2));

        // Act
        Mono<UserRest> result = userService.getUserById(userId, "albums", jwt);

        // Assert: Verify albums are present
        StepVerifier.create(result)
                .expectNextMatches(user -> {
                    // Verify user details
                    assertEquals(userId, user.getId(), "User ID mismatch");
                    assertEquals(userEntity.getFirstName(), user.getFirstName(), "First name mismatch");
                    assertEquals(userEntity.getLastName(), user.getLastName(), "Last name mismatch");
                    assertEquals(userEntity.getEmail(), user.getEmail(), "Email mismatch");

                    // Verify albums
                    assertNotNull(user.getAlbums(), "Albums list should not be null");
                    assertEquals(2, user.getAlbums().size(), "Incorrect number of albums");
                    assertEquals("Summer Vacation", user.getAlbums().get(0).getTitle());
                    assertEquals("Family Reunion", user.getAlbums().get(1).getTitle());
                    return true;
                })
                .verifyComplete();

        // Verify repository call
        verify(userRepository).findById(userId);
    }
}