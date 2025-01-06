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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PersonIntegrationSuccessTest {
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

    // Success Tests

    @Test
    void shouldCreateAndRetrievePerson() {
        // Create test person
        Person person = Person.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        // POST request to create person
        ResponseEntity<Person> createResponse = restTemplate.postForEntity(
                baseUrl,
                person,
                Person.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertNotNull(createResponse.getBody().getId());

        // GET request to retrieve person
        ResponseEntity<Person> getResponse = restTemplate.getForEntity(
                baseUrl + "/" + createResponse.getBody().getId(),
                Person.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(person.getName(), getResponse.getBody().getName());
        assertEquals(person.getEmail(), getResponse.getBody().getEmail());
    }

    @Test
    void shouldUpdatePerson() {
        // Create initial person
        Person person = personRepository.save(Person.builder()
                .name("Initial Name")
                .email("initial@example.com")
                .age(25)
                .build());

        // Update person
        person.setName("Updated Name");
        person.setAge(30);

        HttpEntity<Person> requestEntity = new HttpEntity<>(person);
        ResponseEntity<Person> response = restTemplate.exchange(
                baseUrl + "/" + person.getId(),
                HttpMethod.PUT,
                requestEntity,
                Person.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Name", response.getBody().getName());
        assertEquals(30, response.getBody().getAge());
    }

    @Test
    void shouldDeletePerson() {
        Person person = personRepository.save(Person.builder()
                .name("Delete Test")
                .email("delete@example.com")
                .age(30)
                .build());

        restTemplate.delete(baseUrl + "/" + person.getId());

        ResponseEntity<Person> response = restTemplate.getForEntity(
                baseUrl + "/" + person.getId(),
                Person.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldFindByNameContaining() {
        personRepository.saveAll(Arrays.asList(
                Person.builder().name("John Doe").email("john@example.com").age(25).build(),
                Person.builder().name("Jane Doe").email("jane@example.com").age(30).build(),
                Person.builder().name("Bob Smith").email("bob@example.com").age(35).build()));

        ResponseEntity<List<Person>> response = restTemplate.exchange(
                baseUrl + "/search/name/containing?keyword=Doe",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Person>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void shouldFindByAgeRange() {
        personRepository.saveAll(Arrays.asList(
                Person.builder().name("Young").email("young@example.com").age(20).build(),
                Person.builder().name("Middle").email("middle@example.com").age(30).build(),
                Person.builder().name("Old").email("old@example.com").age(40).build()));

        ResponseEntity<List<Person>> response = restTemplate.exchange(
                baseUrl + "/search/age/range?startAge=25&endAge=35",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Person>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    // Failure Tests

    @Test
    void shouldReturnBadRequestForInvalidEmail() {
        Person person = Person.builder()
                .name("Test User")
                .email("invalid-email")
                .age(25)
                .build();

        ResponseEntity<Person> response = restTemplate.postForEntity(
                baseUrl,
                person,
                Person.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForMissingName() {
        Person person = Person.builder()
                .email("test@example.com")
                .age(25)
                .build();

        ResponseEntity<Person> response = restTemplate.postForEntity(
                baseUrl,
                person,
                Person.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() {
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
        ResponseEntity<Person> response = restTemplate.postForEntity(
                baseUrl,
                person2,
                Person.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundForNonExistentPerson() {
        ResponseEntity<Person> response = restTemplate.getForEntity(
                baseUrl + "/999",
                Person.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldHandleBatchOperations() {
        List<Person> people = Arrays.asList(
                Person.builder().name("Batch 1").email("batch1@example.com").age(25).build(),
                Person.builder().name("Batch 2").email("batch2@example.com").age(30).build());

        ResponseEntity<List<Person>> response = restTemplate.exchange(
                baseUrl + "/batch",
                HttpMethod.POST,
                new HttpEntity<>(people),
                new ParameterizedTypeReference<List<Person>>() {
                });

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void shouldHandleSearchOperations() {
        personRepository.saveAll(Arrays.asList(
                Person.builder().name("John").email("john@example.com").age(25).build(),
                Person.builder().name("Jane").email("jane@example.com").age(30).build()));

        // Test search by keyword
        ResponseEntity<List<Person>> response = restTemplate.exchange(
                baseUrl + "/search/keyword?keyword=john",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Person>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() > 0);
    }

    @Test
    void shouldHandleCountOperations() {
        personRepository.saveAll(Arrays.asList(
                Person.builder().name("Count 1").email("count1@example.com").age(25).build(),
                Person.builder().name("Count 2").email("count2@example.com").age(25).build()));

        ResponseEntity<Long> response = restTemplate.getForEntity(
                baseUrl + "/count/age/25",
                Long.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody());
    }

    @Test
    void shouldHandleConcurrentOperations() throws InterruptedException {
        // Create multiple threads to simulate concurrent requests
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Person person = Person.builder()
                        .name("Concurrent " + index)
                        .email("concurrent" + index + "@example.com")
                        .age(25)
                        .build();
                restTemplate.postForEntity(baseUrl, person, Person.class);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        ResponseEntity<List<Person>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Person>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().size());
    }
}