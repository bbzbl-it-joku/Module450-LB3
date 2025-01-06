package com.example.lb3.demo.person;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/persons")
public class PersonController {
    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    // Basic CRUD Operations
    @GetMapping
    public List<Person> getAllPersons() {
        return service.getAllPersons();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Person> getPersonById(@PathVariable Long id) {
        return service.getPersonById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Person> createPerson(@Valid @RequestBody Person person) {
        if (service.doesEmailExist(person.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if (!service.isValidForCreation(person)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createPerson(person));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Person> updatePerson(@PathVariable Long id,
            @Valid @RequestBody Person person) {
        if (!service.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        person.setId(id);
        if (!service.isValidForUpdate(person)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.updatePerson(person));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        try {
            service.deletePerson(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Search by single field
    @GetMapping("/search/name/{name}")
    public List<Person> findByName(@PathVariable String name) {
        return service.findByName(name);
    }

    @GetMapping("/search/email/{email}")
    public ResponseEntity<Person> findByEmail(@PathVariable String email) {
        return service.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/age/{age}")
    public List<Person> findByAge(@PathVariable int age) {
        return service.findByAge(age);
    }

    // Search by containing pattern
    @GetMapping("/search/name/containing")
    public List<Person> searchByNameContaining(@RequestParam String keyword) {
        return service.searchByNameContaining(keyword);
    }

    @GetMapping("/search/email/containing")
    public List<Person> searchByEmailContaining(@RequestParam String keyword) {
        return service.searchByEmailContaining(keyword);
    }

    // Multiple field searches
    @GetMapping("/search/nameAndAge")
    public List<Person> findByNameAndAge(@RequestParam String name,
            @RequestParam int age) {
        return service.findByNameAndAge(name, age);
    }

    @GetMapping("/search/nameOrEmail")
    public List<Person> findByNameOrEmail(@RequestParam String name,
            @RequestParam String email) {
        return service.findByNameOrEmail(name, email);
    }

    // Ordered searches
    @GetMapping("/search/age/orderByName/{age}")
    public List<Person> findByAgeOrderedByName(@PathVariable int age) {
        return service.findByAgeOrderedByName(age);
    }

    @GetMapping("/search/name/orderByAge/{name}")
    public List<Person> findByNameOrderedByAgeDesc(@PathVariable String name) {
        return service.findByNameOrderedByAgeDesc(name);
    }

    // Age comparison searches
    @GetMapping("/search/age/older/{age}")
    public List<Person> findPeopleOlderThan(@PathVariable int age) {
        return service.findPeopleOlderThan(age);
    }

    @GetMapping("/search/age/younger/{age}")
    public List<Person> findPeopleYoungerThan(@PathVariable int age) {
        return service.findPeopleYoungerThan(age);
    }

    @GetMapping("/search/age/range")
    public ResponseEntity<List<Person>> findPeopleInAgeRange(
            @RequestParam int startAge,
            @RequestParam int endAge) {
        if (startAge > endAge) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.findPeopleInAgeRange(startAge, endAge));
    }

    // Existence checks
    @GetMapping("/exists/email/{email}")
    public boolean doesEmailExist(@PathVariable String email) {
        return service.doesEmailExist(email);
    }

    @GetMapping("/exists/nameAndAge")
    public boolean doesPersonExistWithNameAndAge(@RequestParam String name,
            @RequestParam int age) {
        return service.doesPersonExistWithNameAndAge(name, age);
    }

    // Count operations
    @GetMapping("/count/age/{age}")
    public long countPeopleByAge(@PathVariable int age) {
        return service.countPeopleByAge(age);
    }

    @GetMapping("/count/name/containing")
    public long countPeopleWithNameContaining(@RequestParam String namePattern) {
        return service.countPeopleWithNameContaining(namePattern);
    }

    // Additional delete operations
    @DeleteMapping("/email/{email}")
    public ResponseEntity<Void> deleteByEmail(@PathVariable String email) {
        service.deleteByEmail(email);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/nameAndAge")
    public ResponseEntity<Void> deleteByNameAndAge(@RequestParam String name,
            @RequestParam int age) {
        service.deleteByNameAndAge(name, age);
        return ResponseEntity.ok().build();
    }

    // Custom queries
    @GetMapping("/search/keyword")
    public List<Person> searchByNameOrEmailKeyword(@RequestParam String keyword) {
        return service.searchByNameOrEmailKeyword(keyword);
    }

    @GetMapping("/oldest")
    public List<Person> findOldestPeople() {
        return service.findOldestPeople();
    }

    // Limited results
    @GetMapping("/oldest/top3")
    public List<Person> findTop3OldestPeople() {
        return service.findTop3OldestPeople();
    }

    @GetMapping("/search/name/top5")
    public List<Person> findFirst5ByNameContaining(@RequestParam String name) {
        return service.findFirst5ByNameContainingOrderedByAge(name);
    }

    // Batch operations
    @PostMapping("/batch")
    public ResponseEntity<List<Person>> createPersons(@Valid @RequestBody List<Person> people) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.saveAll(people));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> deletePersons(@RequestBody List<Person> people) {
        try {
            service.deleteAll(people);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Additional utility endpoints
    @GetMapping("/count")
    public long getTotalCount() {
        return service.getTotalCount();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllPersons() {
        service.deleteAllPersons();
        return ResponseEntity.ok().build();
    }
}