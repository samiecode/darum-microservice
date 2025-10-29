package com.darum.employee.domain.repository;

import com.darum.employee.domain.entity.Employee;
import com.darum.employee.domain.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    
    Optional<Employee> findByEmail(String email);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByStatus(EmployeeStatus status);

    boolean existsByEmail(String email);

    /**
     * Find employees by department name (case-insensitive).
     *
     * @param departmentName The name of the department
     * @return List of employees in the department
     */
    @Query("SELECT e FROM Employee e WHERE LOWER(e.department.name) = LOWER(?1)")
    List<Employee> findByDepartmentNameIgnoreCase(String departmentName);
}
