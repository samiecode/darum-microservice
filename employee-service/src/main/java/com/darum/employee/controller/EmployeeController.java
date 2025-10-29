package com.darum.employee.controller;

import com.darum.employee.domain.dto.EmployeeDTO;
import com.darum.employee.service.EmployeeService;
import com.darum.shareddomain.dto.ResponseBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees")
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Get all employees.
     * Requires ADMIN role.
     * 
     * @return List of all employees
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<List<EmployeeDTO>>> getEmployees() {
        return employeeService.getEmployees();
    }

    /**
     * Get a specific employee by ID.
     * Requires ADMIN role.
     * 
     * @param id The ID of the employee
     * @return Employee details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    /**
     * Create a new employee.
     * Requires ADMIN role.
     * 
     * @param employee Employee data to create
     * @return Created employee details
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<EmployeeDTO>> addEmployee(@Valid @RequestBody EmployeeDTO employee) {
        return employeeService.addEmployee(employee);
    }

    /**
     * Update an existing employee.
     * Requires ADMIN role.
     * 
     * @param id       The ID of the employee to update
     * @param employee Updated employee data
     * @return Updated employee details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<EmployeeDTO>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDTO employee) {
        return employeeService.updateEmployee(id, employee);
    }

    /**
     * Delete an employee.
     * Requires ADMIN role.
     * 
     * @param id The ID of the employee to delete
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<Void>> deleteEmployee(@PathVariable Long id) {
        return employeeService.deleteEmployee(id);
    }

    /**
     * Get all employees in a specific department.
     * Requires ADMIN or MANAGER role.
     * 
     * @param dept The name of the department
     * @return List of employees in the department
     */
    @GetMapping("/department/{dept}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ResponseBody<List<EmployeeDTO>>> getEmployeesByDepartment(@PathVariable String dept) {
        return employeeService.getEmployeesByDepartment(dept);
    }

    /**
     * Get the profile of the currently authenticated employee.
     * Requires ADMIN, MANAGER, or EMPLOYEE role.
     * 
     * @return Current employee's profile
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ResponseBody<EmployeeDTO>> getMyProfile() {
        return employeeService.getMyProfile();
    }
}
