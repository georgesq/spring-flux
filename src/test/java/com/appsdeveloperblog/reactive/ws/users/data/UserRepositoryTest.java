package com.appsdeveloperblog.reactive.ws.users.data;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataR2dbcTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    void setUp() {
        UserEntity user1 = new UserEntity(
                UUID.randomUUID(),
                "John",
                "Doe",
                "jd@uol.com",
                "123456789"
        );

        UserEntity user2 = new UserEntity(
                UUID.randomUUID(),
                "John2",
                "Doe",
                "jd2@uol.com",
                "123456789"
        );

        this.insertUser(user1, user2);
    }

    private void insertUser(UserEntity user1, UserEntity user2) {
        var sql = "INSERT INTO users (id, first_name, last_name, email, password) VALUES (:id, :firstName, :lastName, :email, :password)";

        Flux.just(user1, user2)
                .concatMap(user -> databaseClient.sql(sql)
                        .bind("id", user.getId())
                        .bind("firstName", user.getFirstName())
                        .bind("lastName", user.getLastName())
                        .bind("email", user.getEmail())
                        .bind("password", user.getPassword())
                        .fetch()
                        .rowsUpdated())
                        .then()
                        .as(StepVerifier::create)
                        .verifyComplete();

    }

    @AfterAll
    void tearDown() {
        this.databaseClient.sql("TRUNCATE TABLE users")
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

    }

    @Test
    void findAllBy() {
    }

    @Test
    void testFindByEmail_WithExistEmail_ThenUserFinded() {
        // Arrange
        String email = "jd2@uol.com";

        // Act
        var userEntityMono = this.userRepository.findByEmail(email);

        // Assert
        StepVerifier.create(userEntityMono)
                .expectNextMatches(userEntity ->
                        email.equals(userEntity.getEmail())
                )
                .expectComplete()
                .verify();
        ;
    }

    @Test
    void testFindByEmail_WithNotExistEmail_ThenNotUserFinded() {
        // Arrange
        String email = "jd3@uol.com";

        // Act
        var userEntityMono = this.userRepository.findByEmail(email);

        // Assert
        StepVerifier.create(userEntityMono)
                .expectNextCount(0)
                .verifyComplete();
        ;
    }

    @Test
    void testFindAllBy_WithValidPageable_ReturnsPaginatedResults() {
        // Arrange
        var pageable = org.springframework.data.domain.PageRequest.of(0, 2);

        // Act & Assert
        StepVerifier.create(this.userRepository.findAllBy(pageable))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void testFindAllBy_WithInvalidPageable_ReturnsNoPaginatedResults() {
        // Arrange
        var pageable = org.springframework.data.domain.PageRequest.of(2, 2);

        // Act & Assert
        StepVerifier.create(this.userRepository.findAllBy(pageable))
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void testSave_whenExistingEmailProvided_shouldFail() {

        // Arrange
        UserEntity invalidUser = new UserEntity(
                null,
                "John",
                "Doe",
                "jd@uol.com",
                "123456789"
        );

        // Act & Assert
        userRepository.save(invalidUser)
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void testSave_whenNotExistingEmailProvided_shouldCreateUser() {

        // Arrange
        UserEntity validUser = new UserEntity(
                null,
                "John",
                "Doe",
                "jd3@uol.com",
                "123456789"
        );

        // Act & Assert
        userRepository.save(validUser)
                .as(StepVerifier::create)
                .expectNextMatches(savedUser -> {
                    return savedUser.getId() != null
                            && savedUser.getFirstName().equals(validUser.getFirstName())
                            && savedUser.getLastName().equals(validUser.getLastName())
                            && savedUser.getEmail().equals(validUser.getEmail());
                })
                .verifyComplete();
    }
}