package com.example.lb3.demo.person;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PersonIntegrationFailureTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PersonRepository personRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/persons";
        personRepository.deleteAll();
    }

    // Input Validation Failures

    @Test
    void shouldFailToCreatePersonWithNullName() {
        Person person = Person.builder()
                .email("test@example.com")
                .age(25)
                .build();

        ResponseEntity<Object> response = restTemplate.postForEntity(
                baseUrl,
                person,
                Object.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldFailToCreatePersonWithInvalidEmail() {
        Person person = Person.builder()
                .name("Test User")
                .email("invalid-email")
                .age(25)
                .build();

        ResponseEntity<Object> response = restTemplate.postForEntity(
                baseUrl,
                person,
                Object.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldFailToCreatePersonWithNegativeAge() {
        Person person = Person.builder()
                .name("Test User")
                .email("test@example.com")
                .age(-1)
                .build();

        ResponseEntity<Object> response = restTemplate.postForEntity(
                baseUrl,
                person,
                Object.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // Duplicate Data Failures

    @Test
    void shouldFailToCreatePersonWithDuplicateEmail() {
        Person person1 = Person.builder()
                .name("First Person")
                .email("duplicate@example.com")
                .age(25)
                .build();

        Person person2 = Person.builder()
                .name("Second Person")
                .email("duplicate@example.com")
                .age(30)
                .build();

        restTemplate.postForEntity(baseUrl, person1, Person.class);
        ResponseEntity<Object> response = restTemplate.postForEntity(
                baseUrl,
                person2,
                Object.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // Not Found Scenarios

    @Test
    void shouldFailToRetrieveNonExistentPerson() {
        ResponseEntity<Person> response = restTemplate.getForEntity(
                baseUrl + "/999",
                Person.class
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldFailToUpdateNonExistentPerson() {
        Person person = Person.builder()
                .id(999L)
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        ResponseEntity<Object> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.PUT,
                new HttpEntity<>(person),
                Object.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldFailToDeleteNonExistentPerson() {
        ResponseEntity<Object> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.DELETE,
                null,
                Object.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // Invalid Request Format

    @Test
    void shouldFailWithInvalidJsonFormat() {
        String invalidJson = "{ invalid-json }";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Object> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                new HttpEntity<>(invalidJson, headers),
                Object.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // Search Failures

    @Test
    void shouldReturnEmptyListForNonExistentNameSearch() {
        ResponseEntity<List<Person>> response = restTemplate.exchange(
                baseUrl + "/search/name/nonexistent",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Person>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void shouldFailForInvalidAgeRangeSearch() {
        ResponseEntity<List<Person>> response = restTemplate.exchange(
                baseUrl + "/search/age/range?startAge=30&endAge=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Person>>() {}
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // Batch Operation Failures

    @Test
    void shouldFailBatchCreateWithInvalidData() {
        List<Person> invalidPeople = Arrays.asList(
                Person.builder().name(null).email("test1@example.com").age(25).build(),
                Person.builder().name("Test").email("invalid-email").age(30).build()
        );

        ResponseEntity<Object> response = restTemplate.exchange(
                baseUrl + "/batch",
                HttpMethod.POST,
                new HttpEntity<>(invalidPeople),
                Object.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldFailBatchDeleteWithNonExistentIds() {
        List<Person> nonExistentPeople = Arrays.asList(
                Person.builder().id(999L).build(),
                Person.builder().id(1000L).build()
        );

        ResponseEntity<Object> response = restTemplate.exchange(
                baseUrl + "/batch",
                HttpMethod.DELETE,
                new HttpEntity<>(nonExistentPeople),
                Object.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // Concurrency Failures

    @Test
    void shouldHandleConcurrentUpdateConflicts() throws InterruptedException {
        Person person = personRepository.save(Person.builder()
                .name("Original Name")
                .email("concurrent@example.com")
                .age(25)
                .build());

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Person update = Person.builder()
                        .id(person.getId())
                        .name("Updated Name " + index)
                        .email("concurrent@example.com")
                        .age(25)
                        .build();

                restTemplate.exchange(
                        baseUrl + "/" + person.getId(),
                        HttpMethod.PUT,
                        new HttpEntity<>(update),
                        Object.class
                );
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        ResponseEntity<Person> response = restTemplate.getForEntity(
                baseUrl + "/" + person.getId(),
                Person.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotEquals("Original Name", response.getBody().getName());
    }

    // Resource Exhaustion

    @Test
    void shouldHandleLargePayload() {
        String veryLongName = "a".repeat(10000);
        Person person = Person.builder()
                .name(veryLongName)
                .email("test@example.com")
                .age(25)
                .build();

        ResponseEntity<Object> response = restTemplate.postForEntity(
                baseUrl,
                person,
                Object.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldHandleTooManyRequests() {
        IntStream.range(0, 100).parallel().forEach(i -> {
            restTemplate.getForEntity(baseUrl, Object.class);
        });

        // If rate limiting is implemented, this should return 429 TOO_MANY_REQUESTS
        ResponseEntity<Object> response = restTemplate.getForEntity(
                baseUrl,
                Object.class
        );
        assertTrue(List.of(HttpStatus.OK, HttpStatus.TOO_MANY_REQUESTS)
                .contains(response.getStatusCode()));
    }

    // Invalid Content Type

    @Test
    void shouldFailWithInvalidContentType() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<Object> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                new HttpEntity<>("invalid content", headers),
                Object.class
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    }
}