package com.example.lb3.demo.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    // Find by single field
    List<Person> findByName(String name);
    Optional<Person> findByEmail(String email);  // Changed to Optional since email should be unique
    List<Person> findByAge(int age);
    
    // Find by field containing/like
    List<Person> findByNameContainingIgnoreCase(String name);
    List<Person> findByEmailContainingIgnoreCase(String email);
    
    // Find by multiple fields
    List<Person> findByNameAndAge(String name, int age);
    List<Person> findByNameOrEmail(String name, String email);
    
    // Find with ordering
    List<Person> findByAgeOrderByNameAsc(int age);
    List<Person> findByNameOrderByAgeDesc(String name);
    
    // Find with age comparisons
    List<Person> findByAgeGreaterThan(int age);
    List<Person> findByAgeLessThan(int age);
    List<Person> findByAgeBetween(int startAge, int endAge);
    
    // Exists checks
    boolean existsByEmail(String email);
    boolean existsByNameAndAge(String name, int age);
    
    // Count queries
    long countByAge(int age);
    long countByNameContainingIgnoreCase(String name);
    
    // Delete operations
    void deleteByEmail(String email);
    void deleteByNameAndAge(String name, int age);
    
    // Custom queries using JPQL
    @Query("SELECT p FROM Person p WHERE p.age >= :minAge AND p.age <= :maxAge ORDER BY p.age ASC")
    List<Person> findPeopleInAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    
    @Query("SELECT p FROM Person p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Person> searchByNameOrEmailKeyword(@Param("keyword") String keyword);
    
    // Native SQL query example
    @Query(value = "SELECT * FROM person WHERE age = (SELECT MAX(age) FROM person)", nativeQuery = true)
    List<Person> findOldestPeople();
    
    // Limiting results
    List<Person> findTop3ByOrderByAgeDesc();
    List<Person> findFirst5ByNameContainingOrderByAgeAsc(String name);
}