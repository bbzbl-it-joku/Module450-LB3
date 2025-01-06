package com.example.lb3.demo.person;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositorySuccessTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

    @Autowired
    private PersonRepository repository;

    private Person person1;
    private Person person2;
    private Person person3;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        person1 = repository.save(Person.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(25)
                .build());

        person2 = repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(30)
                .build());

        person3 = repository.save(Person.builder()
                .name("John Smith")
                .email("smith@example.com")
                .age(35)
                .build());
    }

    @Test
    void shouldFindByName() {
        List<Person> found = repository.findByName("John Doe");

        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals("john@example.com", found.get(0).getEmail());
    }

    @Test
    void shouldFindByEmail() {
        Optional<Person> found = repository.findByEmail("john@example.com");

        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
    }

    @Test
    void shouldFindByAge() {
        List<Person> found = repository.findByAge(30);

        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals("jane@example.com", found.get(0).getEmail());
    }

    @Test
    void shouldFindByNameContainingIgnoreCase() {
        List<Person> found = repository.findByNameContainingIgnoreCase("john");

        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("john@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("smith@example.com")));
    }

    @Test
    void shouldFindByEmailContainingIgnoreCase() {
        List<Person> found = repository.findByEmailContainingIgnoreCase("EXAMPLE");

        assertEquals(3, found.size());
    }

    @Test
    void shouldFindByNameAndAge() {
        List<Person> found = repository.findByNameAndAge("Jane Doe", 30);

        assertEquals(1, found.size());
        assertEquals("jane@example.com", found.get(0).getEmail());
    }

    @Test
    void shouldFindByNameOrEmail() {
        List<Person> found = repository.findByNameOrEmail("John Doe", "jane@example.com");

        assertEquals(2, found.size());
    }

    @Test
    void shouldFindByAgeOrderByNameAsc() {
        Person anotherJohn = repository.save(Person.builder()
                .name("Aaron Smith")
                .email("aaron@example.com")
                .age(25)
                .build());

        List<Person> found = repository.findByAgeOrderByNameAsc(25);

        assertEquals(2, found.size());
        assertEquals("Aaron Smith", found.get(0).getName());
        assertEquals("John Doe", found.get(1).getName());
    }

    @Test
    void shouldFindByAgeGreaterThan() {
        List<Person> found = repository.findByAgeGreaterThan(30);

        assertEquals(1, found.size());
        assertEquals("smith@example.com", found.get(0).getEmail());
    }

    @Test
    void shouldFindByAgeLessThan() {
        List<Person> found = repository.findByAgeLessThan(30);

        assertEquals(1, found.size());
        assertEquals("john@example.com", found.get(0).getEmail());
    }

    @Test
    void shouldFindByAgeBetween() {
        List<Person> found = repository.findByAgeBetween(25, 30);

        assertEquals(2, found.size());
    }

    @Test
    void shouldCheckIfExistsByEmail() {
        assertTrue(repository.existsByEmail("john@example.com"));
        assertFalse(repository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void shouldCheckIfExistsByNameAndAge() {
        assertTrue(repository.existsByNameAndAge("John Doe", 25));
        assertFalse(repository.existsByNameAndAge("John Doe", 30));
    }

    @Test
    void shouldCountByAge() {
        assertEquals(1, repository.countByAge(25));
        assertEquals(0, repository.countByAge(40));
    }

    @Test
    void shouldCountByNameContaining() {
        assertEquals(2, repository.countByNameContainingIgnoreCase("John"));
        assertEquals(0, repository.countByNameContainingIgnoreCase("xyz"));
    }

    @Test
    void shouldDeleteByEmail() {
        repository.deleteByEmail("john@example.com");

        assertFalse(repository.existsByEmail("john@example.com"));
        assertEquals(2, repository.count());
    }

    @Test
    void shouldDeleteByNameAndAge() {
        repository.deleteByNameAndAge("John Doe", 25);

        assertFalse(repository.existsByEmail("john@example.com"));
        assertEquals(2, repository.count());
    }

    @Test
    void shouldFindPeopleInAgeRange() {
        List<Person> found = repository.findPeopleInAgeRange(25, 30);

        assertEquals(2, found.size());
    }

    @Test
    void shouldSearchByNameOrEmailKeyword() {
        List<Person> found = repository.searchByNameOrEmailKeyword("doe");

        assertEquals(2, found.size());
    }

    @Test
    void shouldFindOldestPeople() {
        List<Person> oldest = repository.findOldestPeople();

        assertFalse(oldest.isEmpty());
        assertEquals(35, oldest.get(0).getAge());
    }

    @Test
    void shouldFindTop3ByOrderByAgeDesc() {
        List<Person> top3 = repository.findTop3ByOrderByAgeDesc();

        assertEquals(3, top3.size());
        assertEquals(35, top3.get(0).getAge());
        assertEquals(30, top3.get(1).getAge());
        assertEquals(25, top3.get(2).getAge());
    }
}