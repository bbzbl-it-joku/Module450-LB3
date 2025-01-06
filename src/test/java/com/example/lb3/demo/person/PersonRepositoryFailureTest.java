package com.example.lb3.demo.person;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositoryFailureTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

    @Autowired
    private PersonRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldNotFindByNonexistentEmail() {
        Optional<Person> found = repository.findByEmail("nonexistent@example.com");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldReturnEmptyListForNonexistentName() {
        List<Person> found = repository.findByName("Nonexistent Name");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNonexistentAge() {
        List<Person> found = repository.findByAge(999);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenSavingPersonWithDuplicateEmail() {
        // Save first person
        Person person1 = Person.builder()
                .name("First Person")
                .email("duplicate@example.com")
                .age(25)
                .build();
        repository.save(person1);

        // Try to save second person with same email
        Person person2 = Person.builder()
                .name("Second Person")
                .email("duplicate@example.com")
                .age(30)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(person2);
            repository.flush(); // Force the persistence context to flush
        });
    }

    @Test
    void shouldThrowExceptionWhenSavingPersonWithNullName() {
        Person person = Person.builder()
                .email("test@example.com")
                .age(25)
                .build();

        assertThrows(ConstraintViolationException.class, () -> {
            repository.save(person);
            repository.flush();
        });
    }

    @Test
    void shouldThrowExceptionWhenSavingPersonWithInvalidEmail() {
        Person person = Person.builder()
                .name("Test Person")
                .email("invalid-email")
                .age(25)
                .build();

        assertThrows(ConstraintViolationException.class, () -> {
            repository.save(person);
            repository.flush();
        });
    }

    @Test
    void shouldReturnEmptyListForNameContainingWithNoMatches() {
        List<Person> found = repository.findByNameContainingIgnoreCase("xyz123");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForEmailContainingWithNoMatches() {
        List<Person> found = repository.findByEmailContainingIgnoreCase("xyz123");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNameAndAgeWithNoMatches() {
        List<Person> found = repository.findByNameAndAge("Nonexistent", 999);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNameOrEmailWithNoMatches() {
        List<Person> found = repository.findByNameOrEmail("Nonexistent", "nonexistent@example.com");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForAgeGreaterThanMaxValue() {
        List<Person> found = repository.findByAgeGreaterThan(Integer.MAX_VALUE);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForAgeLessThanMinValue() {
        List<Person> found = repository.findByAgeLessThan(Integer.MIN_VALUE);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForInvalidAgeRange() {
        List<Person> found = repository.findByAgeBetween(30, 20); // Invalid range (start > end)
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnFalseForExistsByNonexistentEmail() {
        assertFalse(repository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void shouldReturnFalseForExistsByNonexistentNameAndAge() {
        assertFalse(repository.existsByNameAndAge("Nonexistent", 999));
    }

    @Test
    void shouldReturnZeroForCountByNonexistentAge() {
        assertEquals(0, repository.countByAge(999));
    }

    @Test
    void shouldReturnZeroForCountByNonexistentNamePattern() {
        assertEquals(0, repository.countByNameContainingIgnoreCase("xyz123"));
    }

    @Test
    void shouldNotFailWhenDeletingByNonexistentEmail() {
        repository.deleteByEmail("nonexistent@example.com");
        // Should not throw any exception
    }

    @Test
    void shouldNotFailWhenDeletingByNonexistentNameAndAge() {
        repository.deleteByNameAndAge("Nonexistent", 999);
        // Should not throw any exception
    }

    @Test
    void shouldReturnEmptyListForCustomQueriesWithNoMatches() {
        assertTrue(repository.findPeopleInAgeRange(999, 1000).isEmpty());
        assertTrue(repository.searchByNameOrEmailKeyword("xyz123").isEmpty());
        assertTrue(repository.findOldestPeople().isEmpty());
    }

    @Test
    void shouldReturnEmptyListForTop3WithNoData() {
        assertTrue(repository.findTop3ByOrderByAgeDesc().isEmpty());
    }

    @Test
    void shouldReturnEmptyListForFirst5WithNoMatches() {
        assertTrue(repository.findFirst5ByNameContainingOrderByAgeAsc("xyz123").isEmpty());
    }

    @Test
    void shouldHandleBatchOperationsWithEmptyList() {
        List<Person> savedPeople = repository.saveAll(List.of());
        assertTrue(savedPeople.isEmpty());
    }

    @Test
    void shouldThrowExceptionForBatchSaveWithInvalidData() {
        List<Person> invalidPeople = Arrays.asList(
                Person.builder().name(null).email("test1@example.com").age(25).build(),
                Person.builder().name("Test").email("invalid-email").age(30).build());

        assertThrows(ConstraintViolationException.class, () -> {
            repository.saveAll(invalidPeople);
            repository.flush();
        });
    }
}