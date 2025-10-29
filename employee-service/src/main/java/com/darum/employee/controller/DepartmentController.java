package com.darum.employee.controller;

import com.darum.employee.domain.dto.DepartmentCreateRequest;
import com.darum.employee.domain.dto.DepartmentDTO;
import com.darum.employee.domain.dto.DepartmentUpdateRequest;
import com.darum.employee.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Get all departments.
     * 
     * @return List of all departments with employee counts
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<List<DepartmentDTO>>> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

    /**
     * Get a specific department by ID.
     * 
     * @param id The ID of the department
     * @return Department details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<DepartmentDTO>> getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id);
    }

    /**
     * Create a new department.
     * 
     * @param request Department creation request containing name and description
     * @return Created department details
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<DepartmentDTO>> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request) {
        return departmentService.createDepartment(request);
    }

    /**
     * Update an existing department.
     * 
     * @param id      The ID of the department to update
     * @param request Update request containing fields to modify
     * @return Updated department details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<DepartmentDTO>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        return departmentService.updateDepartment(id, request);
    }

    /**
     * Delete a department.
     * Department must have no assigned employees.
     * 
     * @param id The ID of the department to delete
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<Void>> deleteDepartment(@PathVariable Long id) {
        return departmentService.deleteDepartment(id);
    }

    /**
     * Get a department by name.
     * 
     * @param name The name of the department (case-insensitive)
     * @return Department details
     */
    @GetMapping("/name/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<DepartmentDTO>> getDepartmentByName(@PathVariable String name) {
        return departmentService.getDepartmentByName(name);
    }
}
