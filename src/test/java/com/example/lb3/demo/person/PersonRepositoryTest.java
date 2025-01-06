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
class PersonRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

    @Autowired
    private PersonRepository repository;

    private Person testPerson;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        testPerson = repository.save(Person.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(25)
                .build());
    }

    @Test
    void testSavePersonSuccess() {
        // Arrange
        Person newPerson = Person.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .age(30)
                .build();

        // Act
        Person savedPerson = repository.save(newPerson);

        // Assert
        assertNotNull(savedPerson.getId());
        assertEquals(newPerson.getName(), savedPerson.getName());
        assertEquals(newPerson.getEmail(), savedPerson.getEmail());
        assertEquals(newPerson.getAge(), savedPerson.getAge());
    }

    @Test
    void testFindByIdSuccess() {
        // Act
        Optional<Person> found = repository.findById(testPerson.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(testPerson.getName(), found.get().getName());
        assertEquals(testPerson.getEmail(), found.get().getEmail());
        assertEquals(testPerson.getAge(), found.get().getAge());
    }

    @Test
    void testFindAllSuccess() {
        // Arrange
        Person secondPerson = repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(30)
                .build());

        // Act
        List<Person> allPersons = repository.findAll();

        // Assert
        assertEquals(2, allPersons.size());
        assertTrue(allPersons.stream().anyMatch(p -> p.getEmail().equals("john@example.com")));
        assertTrue(allPersons.stream().anyMatch(p -> p.getEmail().equals("jane@example.com")));
    }

    @Test
    void testDeleteByIdSuccess() {
        // Arrange
        Long personId = testPerson.getId();

        // Act
        repository.deleteById(personId);

        // Assert
        Optional<Person> found = repository.findById(personId);
        assertFalse(found.isPresent());
        assertEquals(0, repository.count());
    }

    @Test
    void testExistsByIdSuccess() {
        // Arrange
        Long existingId = testPerson.getId();
        Long nonExistingId = existingId + 1;

        // Act & Assert
        assertTrue(repository.existsById(existingId));
        assertFalse(repository.existsById(nonExistingId));
    }

    @Test
    void testFindByNameSuccess() {
        // Arrange
        Person secondPerson = repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe2@example.com")
                .age(30)
                .build());

        // Act
        List<Person> found = repository.findByName("John Doe");

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("john@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("johndoe2@example.com")));
    }

    @Test
    void testFindByEmailSuccess() {
        // Act
        Optional<Person> found = repository.findByEmail("john@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
        assertEquals(25, found.get().getAge());
    }

    @Test
    void testFindByAgeSuccess() {
        // Arrange
        Person secondPerson = repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(25)
                .build());

        // Act
        List<Person> found = repository.findByAge(25);

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("John Doe")));
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("Jane Doe")));
    }

    @Test
    void testFindByNameContainingIgnoreCaseSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("JOHN SMITH")
                .email("johnsmith@example.com")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("johnny walker")
                .email("johnny@example.com")
                .age(35)
                .build());

        // Act
        List<Person> found = repository.findByNameContainingIgnoreCase("john");

        // Assert
        assertEquals(3, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("john@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("johnsmith@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("johnny@example.com")));
    }

    @Test
    void testFindByEmailContainingIgnoreCaseSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("John Smith")
                .email("JOHNSMITH@EXAMPLE.COM")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("Johnny Walker")
                .email("johnny@EXAMPLE.com")
                .age(35)
                .build());

        // Act
        List<Person> found = repository.findByEmailContainingIgnoreCase("JOHN");

        // Assert
        assertEquals(3, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("John Doe")));
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("John Smith")));
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("Johnny Walker")));
    }

    @Test
    void testFindByNameAndAgeSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe2@example.com")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe3@example.com")
                .age(25)
                .build());

        // Act
        List<Person> found = repository.findByNameAndAge("John Doe", 25);

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("john@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("johndoe3@example.com")));
    }

    @Test
    void testFindByNameOrEmailSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("different@example.com")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("Different Name")
                .email("jane@example.com")
                .age(35)
                .build());

        // Act
        List<Person> found = repository.findByNameOrEmail("Jane Doe", "jane@example.com");

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("jane@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("different@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("Different Name")));
    }

    @Test
    void testFindByAgeOrderByNameAscSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Charlie Brown")
                .email("charlie@example.com")
                .age(25)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(25)
                .build());

        // Act
        List<Person> found = repository.findByAgeOrderByNameAsc(25);

        // Assert
        assertEquals(3, found.size());
        assertEquals("Alice Smith", found.get(0).getName());
        assertEquals("Charlie Brown", found.get(1).getName());
        assertEquals("John Doe", found.get(2).getName());
    }

    @Test
    void testFindByNameOrderByAgeDescSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe2@example.com")
                .age(35)
                .build());

        repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe3@example.com")
                .age(30)
                .build());

        // Act
        List<Person> found = repository.findByNameOrderByAgeDesc("John Doe");

        // Assert
        assertEquals(3, found.size());
        assertEquals(35, found.get(0).getAge());
        assertEquals(30, found.get(1).getAge());
        assertEquals(25, found.get(2).getAge());
    }

    @Test
    void testFindByAgeGreaterThanSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(35)
                .build());

        // Act
        List<Person> found = repository.findByAgeGreaterThan(28);

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getAge() == 30));
        assertTrue(found.stream().anyMatch(p -> p.getAge() == 35));
        assertFalse(found.stream().anyMatch(p -> p.getAge() <= 28));
    }

    @Test
    void testFindByAgeLessThanSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(20)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(22)
                .build());

        // Act
        List<Person> found = repository.findByAgeLessThan(23);

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getAge() == 20));
        assertTrue(found.stream().anyMatch(p -> p.getAge() == 22));
        assertFalse(found.stream().anyMatch(p -> p.getAge() >= 23));
    }

    @Test
    void testFindByAgeBetweenSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(28)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(32)
                .build());

        repository.save(Person.builder()
                .name("Bob Wilson")
                .email("bob@example.com")
                .age(30)
                .build());

        // Act
        List<Person> found = repository.findByAgeBetween(28, 32);

        // Assert
        assertEquals(3, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getAge() == 28));
        assertTrue(found.stream().anyMatch(p -> p.getAge() == 30));
        assertTrue(found.stream().anyMatch(p -> p.getAge() == 32));
        assertFalse(found.stream().anyMatch(p -> p.getAge() < 28 || p.getAge() > 32));
    }

    @Test
    void testExistsByEmailSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(30)
                .build());

        // Act & Assert
        assertTrue(repository.existsByEmail("john@example.com"));
        assertTrue(repository.existsByEmail("jane@example.com"));
        assertFalse(repository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void testExistsByNameAndAgeSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe2@example.com")
                .age(30)
                .build());

        // Act & Assert
        assertTrue(repository.existsByNameAndAge("John Doe", 25));
        assertTrue(repository.existsByNameAndAge("John Doe", 30));
        assertFalse(repository.existsByNameAndAge("John Doe", 40));
        assertFalse(repository.existsByNameAndAge("Jane Doe", 25));
    }

    @Test
    void testCountByAgeSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(25)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(25)
                .build());

        // Act & Assert
        assertEquals(3, repository.countByAge(25));
        assertEquals(0, repository.countByAge(40));
    }

    @Test
    void testCountByNameContainingIgnoreCaseSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Johnny Walker")
                .email("johnny@example.com")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("JOHN SMITH")
                .email("johnsmith@example.com")
                .age(35)
                .build());

        // Act & Assert
        assertEquals(3, repository.countByNameContainingIgnoreCase("john"));
        assertEquals(0, repository.countByNameContainingIgnoreCase("xyz"));
        assertEquals(3, repository.countByNameContainingIgnoreCase("JOHN"));
    }

    @Test
    void testDeleteByEmailSuccess() {
        // Arrange
        Person secondPerson = repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(30)
                .build());

        long initialCount = repository.count();

        // Act
        repository.deleteByEmail("john@example.com");

        // Assert
        assertFalse(repository.existsByEmail("john@example.com"));
        assertTrue(repository.existsByEmail("jane@example.com"));
        assertEquals(initialCount - 1, repository.count());
    }

    @Test
    void testDeleteByNameAndAgeSuccess() {
        // Arrange
        Person secondPerson = repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe2@example.com")
                .age(25)
                .build());

        Person thirdPerson = repository.save(Person.builder()
                .name("John Doe")
                .email("johndoe3@example.com")
                .age(30)
                .build());

        long initialCount = repository.count();

        // Act
        repository.deleteByNameAndAge("John Doe", 25);

        // Assert
        assertEquals(initialCount - 2, repository.count());
        assertTrue(repository.existsByNameAndAge("John Doe", 30));
        assertFalse(repository.existsByEmail("john@example.com"));
        assertFalse(repository.existsByEmail("johndoe2@example.com"));
        assertTrue(repository.existsByEmail("johndoe3@example.com"));
    }

    @Test
    void testFindPeopleInAgeRangeSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(28)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(32)
                .build());

        repository.save(Person.builder()
                .name("Bob Wilson")
                .email("bob@example.com")
                .age(35)
                .build());

        // Act
        List<Person> found = repository.findPeopleInAgeRange(27, 33);

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("Jane Doe")));
        assertTrue(found.stream().anyMatch(p -> p.getName().equals("Alice Smith")));
        assertFalse(found.stream().anyMatch(p -> p.getName().equals("Bob Wilson")));
    }

    @Test
    void testSearchByNameOrEmailKeywordSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("John Smith")
                .email("johnsmith@example.com")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("Alice Johnson")
                .email("john.alice@example.com")
                .age(35)
                .build());

        // Act
        List<Person> found = repository.searchByNameOrEmailKeyword("john");

        // Assert
        assertEquals(3, found.size());
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("john@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("johnsmith@example.com")));
        assertTrue(found.stream().anyMatch(p -> p.getEmail().equals("john.alice@example.com")));
    }

    @Test
    void testFindOldestPeopleSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(40)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(40)
                .build());

        repository.save(Person.builder()
                .name("Bob Wilson")
                .email("bob@example.com")
                .age(35)
                .build());

        // Act
        List<Person> oldestPeople = repository.findOldestPeople();

        // Assert
        assertEquals(2, oldestPeople.size());
        assertTrue(oldestPeople.stream().allMatch(p -> p.getAge() == 40));
        assertTrue(oldestPeople.stream().anyMatch(p -> p.getName().equals("Jane Doe")));
        assertTrue(oldestPeople.stream().anyMatch(p -> p.getName().equals("Alice Smith")));
    }

    @Test
    void testFindTop3ByOrderByAgeDescSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(40)
                .build());

        repository.save(Person.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .age(35)
                .build());

        repository.save(Person.builder()
                .name("Bob Wilson")
                .email("bob@example.com")
                .age(30)
                .build());

        // Act
        List<Person> top3 = repository.findTop3ByOrderByAgeDesc();

        // Assert
        assertEquals(3, top3.size());
        assertEquals(40, top3.get(0).getAge());
        assertEquals(35, top3.get(1).getAge());
        assertEquals(30, top3.get(2).getAge());
    }

    @Test
    void testFindFirst5ByNameContainingOrderByAgeAscSuccess() {
        // Arrange
        repository.save(Person.builder()
                .name("John Smith")
                .email("smith@example.com")
                .age(40)
                .build());

        repository.save(Person.builder()
                .name("Johnny Walker")
                .email("walker@example.com")
                .age(35)
                .build());

        repository.save(Person.builder()
                .name("John Williams")
                .email("williams@example.com")
                .age(30)
                .build());

        repository.save(Person.builder()
                .name("John Brown")
                .email("brown@example.com")
                .age(45)
                .build());

        repository.save(Person.builder()
                .name("John Davis")
                .email("davis@example.com")
                .age(28)
                .build());

        // Act
        List<Person> first5 = repository.findFirst5ByNameContainingOrderByAgeAsc("John");

        // Assert
        assertEquals(5, first5.size());
        assertEquals(25, first5.get(0).getAge());
        assertEquals(28, first5.get(1).getAge());
        assertEquals(30, first5.get(2).getAge());
        assertEquals(35, first5.get(3).getAge());
        assertEquals(40, first5.get(4).getAge());
    }
}