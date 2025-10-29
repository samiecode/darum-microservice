package com.darum.employee.domain.repository;

import com.darum.employee.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Find a department by its name (case-insensitive).
     *
     * @param name The name of the department to search for
     * @return Optional containing the department if found
     */
    Optional<Department> findByNameIgnoreCase(String name);

    /**
     * Check if a department exists with the given name (case-insensitive).
     *
     * @param name The name to check
     * @return true if a department with this name exists
     */
    boolean existsByNameIgnoreCase(String name);

    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId")
    Long countEmployeesByDepartmentId(Long departmentId);
}
