package com.darum.employee.service;

import com.darum.employee.domain.dto.DepartmentCreateRequest;
import com.darum.employee.domain.dto.DepartmentDTO;
import com.darum.employee.domain.dto.DepartmentUpdateRequest;
import com.darum.employee.domain.entity.Department;
import com.darum.employee.domain.repository.DepartmentRepository;
import com.darum.shareddomain.dto.ResponseBody;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing Department operations.
 * Handles business logic for department CRUD operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Retrieves all departments with their employee counts.
     *
     * @return ResponseEntity containing a list of all departments
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseBody<List<DepartmentDTO>>> getAllDepartments() {
        log.info("Fetching all departments");

        List<DepartmentDTO> departments = departmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} departments", departments.size());
        ResponseBody<List<DepartmentDTO>> response = ResponseBody.success(
                "Departments retrieved successfully", departments);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific department by its ID.
     *
     * @param id The ID of the department to retrieve
     * @return ResponseEntity containing the department details
     * @throws EntityNotFoundException if department with given ID doesn't exist
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseBody<DepartmentDTO>> getDepartmentById(Long id) {
        log.info("Fetching department with ID: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Department not found with ID: {}", id);
                    return new EntityNotFoundException("Department not found with id: " + id);
                });

        DepartmentDTO departmentDTO = convertToDTO(department);

        log.info("Successfully retrieved department: {}", department.getName());
        ResponseBody<DepartmentDTO> response = ResponseBody.success(
                "Department retrieved successfully", departmentDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new department.
     *
     * @param request The department creation request containing name and
     *                description
     * @return ResponseEntity containing the created department
     * @throws IllegalArgumentException if department with the same name already
     *                                  exists
     */
    @Transactional
    public ResponseEntity<ResponseBody<DepartmentDTO>> createDepartment(DepartmentCreateRequest request) {
        log.info("Creating new department with name: {}", request.name());

        // Check if department with same name already exists
        if (departmentRepository.existsByNameIgnoreCase(request.name())) {
            log.error("Department with name '{}' already exists", request.name());
            throw new IllegalArgumentException("Department with name '" + request.name() + "' already exists");
        }

        Department department = Department.builder()
                .name(request.name())
                .description(request.description())
                .build();

        Department savedDepartment = departmentRepository.save(department);
        DepartmentDTO departmentDTO = convertToDTO(savedDepartment);

        log.info("Successfully created department with ID: {}", savedDepartment.getId());
        ResponseBody<DepartmentDTO> response = ResponseBody.success(
                "Department created successfully", departmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing department.
     * Only non-null fields in the request will be updated.
     *
     * @param id      The ID of the department to update
     * @param request The update request containing fields to update
     * @return ResponseEntity containing the updated department
     * @throws EntityNotFoundException  if department with given ID doesn't exist
     * @throws IllegalArgumentException if trying to update name to an existing
     *                                  department name
     */
    @Transactional
    public ResponseEntity<ResponseBody<DepartmentDTO>> updateDepartment(Long id, DepartmentUpdateRequest request) {
        log.info("Updating department with ID: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Department not found with ID: {}", id);
                    return new EntityNotFoundException("Department not found with id: " + id);
                });

        // Update name if provided and different from current
        if (request.name() != null && !request.name().equals(department.getName())) {
            // Check if new name already exists for a different department
            if (departmentRepository.existsByNameIgnoreCase(request.name())) {
                log.error("Department with name '{}' already exists", request.name());
                throw new IllegalArgumentException("Department with name '" + request.name() + "' already exists");
            }
            department.setName(request.name());
        }

        // Update description if provided
        if (request.description() != null) {
            department.setDescription(request.description());
        }

        Department updatedDepartment = departmentRepository.save(department);
        DepartmentDTO departmentDTO = convertToDTO(updatedDepartment);

        log.info("Successfully updated department with ID: {}", id);
        ResponseBody<DepartmentDTO> response = ResponseBody.success(
                "Department updated successfully", departmentDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a department by its ID.
     * Department can only be deleted if it has no assigned employees.
     *
     * @param id The ID of the department to delete
     * @return ResponseEntity with success message
     * @throws EntityNotFoundException if department with given ID doesn't exist
     * @throws IllegalStateException   if department has assigned employees
     */
    @Transactional
    public ResponseEntity<ResponseBody<Void>> deleteDepartment(Long id) {
        log.info("Deleting department with ID: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Department not found with ID: {}", id);
                    return new EntityNotFoundException("Department not found with id: " + id);
                });

        // Check if department has any employees
        Long employeeCount = departmentRepository.countEmployeesByDepartmentId(id);
        if (employeeCount > 0) {
            log.error("Cannot delete department with ID: {} as it has {} assigned employees", id, employeeCount);
            throw new IllegalStateException(
                    "Cannot delete department with " + employeeCount + " assigned employees. " +
                            "Please reassign or remove employees first.");
        }

        try {
            departmentRepository.delete(department);
            log.info("Successfully deleted department with ID: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to delete department due to data integrity constraint: {}", e.getMessage());
            throw new IllegalStateException("Cannot delete department due to existing references", e);
        }

        ResponseBody<Void> response = ResponseBody.success("Department deleted successfully", null);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a department by its name (case-insensitive).
     *
     * @param name The name of the department to search for
     * @return ResponseEntity containing the department
     * @throws EntityNotFoundException if department with given name doesn't exist
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseBody<DepartmentDTO>> getDepartmentByName(String name) {
        log.info("Fetching department with name: {}", name);

        Department department = departmentRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> {
                    log.error("Department not found with name: {}", name);
                    return new EntityNotFoundException("Department not found with name: " + name);
                });

        DepartmentDTO departmentDTO = convertToDTO(department);

        log.info("Successfully retrieved department: {}", name);
        ResponseBody<DepartmentDTO> response = ResponseBody.success(
                "Department retrieved successfully", departmentDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Converts a Department entity to a DepartmentDTO.
     *
     * @param department The department entity to convert
     * @return DepartmentDTO representation of the department
     */
    private DepartmentDTO convertToDTO(Department department) {
        Long employeeCount = departmentRepository.countEmployeesByDepartmentId(department.getId());

        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .employeeCount(employeeCount)
                .createdAt(department.getCreatedAt())
                .build();
    }
}
