package com.example.lb3.demo.person;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PersonService {
    private final PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    // Basic CRUD operations
    public List<Person> getAllPersons() {
        return repository.findAll();
    }

    public Optional<Person> getPersonById(Long id) {
        return repository.findById(id);
    }

    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    public Person createPerson(@Valid Person person) {
        return repository.save(person);
    }

    public Person updatePerson(@Valid Person person) {
        if (person.getId() == null) {
            throw new IllegalArgumentException("Person ID cannot be null for update operation");
        }
        return repository.save(person);
    }

    public void deletePerson(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Person not found with id: " + id);
        }
        repository.deleteById(id);
    }

    // Find by single field
    public List<Person> findByName(String name) {
        return repository.findByName(name);
    }

    public Optional<Person> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public List<Person> findByAge(int age) {
        return repository.findByAge(age);
    }

    // Find by field containing/like
    public List<Person> searchByNameContaining(String nameKeyword) {
        return repository.findByNameContainingIgnoreCase(nameKeyword);
    }

    public List<Person> searchByEmailContaining(String emailKeyword) {
        return repository.findByEmailContainingIgnoreCase(emailKeyword);
    }

    // Find by multiple fields
    public List<Person> findByNameAndAge(String name, int age) {
        return repository.findByNameAndAge(name, age);
    }

    public List<Person> findByNameOrEmail(String name, String email) {
        return repository.findByNameOrEmail(name, email);
    }

    // Find with ordering
    public List<Person> findByAgeOrderedByName(int age) {
        return repository.findByAgeOrderByNameAsc(age);
    }

    public List<Person> findByNameOrderedByAgeDesc(String name) {
        return repository.findByNameOrderByAgeDesc(name);
    }

    // Find with age comparisons
    public List<Person> findPeopleOlderThan(int age) {
        return repository.findByAgeGreaterThan(age);
    }

    public List<Person> findPeopleYoungerThan(int age) {
        return repository.findByAgeLessThan(age);
    }

    public List<Person> findPeopleByAgeBetween(int startAge, int endAge) {
        return repository.findByAgeBetween(startAge, endAge);
    }

    // Exists checks
    public boolean doesEmailExist(String email) {
        return repository.existsByEmail(email);
    }

    public boolean doesPersonExistWithNameAndAge(String name, int age) {
        return repository.existsByNameAndAge(name, age);
    }

    // Count operations
    public long countPeopleByAge(int age) {
        return repository.countByAge(age);
    }

    public long countPeopleWithNameContaining(String namePattern) {
        return repository.countByNameContainingIgnoreCase(namePattern);
    }

    // Delete operations
    @Transactional
    public void deleteByEmail(String email) {
        repository.deleteByEmail(email);
    }

    @Transactional
    public void deleteByNameAndAge(String name, int age) {
        repository.deleteByNameAndAge(name, age);
    }

    // Custom query methods
    public List<Person> findPeopleInAgeRange(int minAge, int maxAge) {
        return repository.findPeopleInAgeRange(minAge, maxAge);
    }

    public List<Person> searchByNameOrEmailKeyword(String keyword) {
        return repository.searchByNameOrEmailKeyword(keyword);
    }

    public List<Person> findOldestPeople() {
        return repository.findOldestPeople();
    }

    // Limited results
    public List<Person> findTop3OldestPeople() {
        return repository.findTop3ByOrderByAgeDesc();
    }

    public List<Person> findFirst5ByNameContainingOrderedByAge(String name) {
        return repository.findFirst5ByNameContainingOrderByAgeAsc(name);
    }

    // Batch operations
    @Transactional
    public List<Person> saveAll(List<Person> people) {
        return repository.saveAll(people);
    }

    @Transactional
    public void deleteAll(List<Person> people) {
        for (Person person : people) {
            if (!repository.existsById(person.getId())) {
                throw new EntityNotFoundException("Person not found with id: " + person.getId());
            }
        }
        repository.deleteAll(people);
    }

    // Additional utility methods
    public boolean isValidForCreation(Person person) {
        return person.getId() == null &&
                person.getEmail() != null &&
                !doesEmailExist(person.getEmail());
    }

    public boolean isValidForUpdate(Person person) {
        if (person.getId() == null) {
            return false;
        }
        Optional<Person> existing = getPersonById(person.getId());
        if (existing.isEmpty()) {
            return false;
        }
        // Check if email is being changed and if new email already exists
        return person.getEmail().equals(existing.get().getEmail()) ||
                !doesEmailExist(person.getEmail());
    }

    public long getTotalCount() {
        return repository.count();
    }

    @Transactional
    public void deleteAllPersons() {
        repository.deleteAll();
    }
}