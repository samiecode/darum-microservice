package com.darum.employee.service;

import com.darum.employee.domain.dto.EmployeeDTO;
import com.darum.employee.domain.entity.Department;
import com.darum.employee.domain.entity.Employee;
import com.darum.employee.domain.repository.DepartmentRepository;
import com.darum.employee.domain.repository.EmployeeRepository;
import com.darum.shareddomain.dto.ResponseBody;
import com.darum.shareddomain.lib.Utils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing Employee operations.
 * Handles business logic for employee CRUD operations and department
 * assignments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

        private final EmployeeRepository employeeRepository;
        private final DepartmentRepository departmentRepository;
        private final DiscoveryClient discoveryClient;
        private final RestClient restClient;
        private final Utils utils;

        /**
         * Creates a new employee and registers them in the auth service.
         *
         * @param employee Employee data transfer object
         * @return ResponseEntity containing the created employee
         * @throws IllegalArgumentException if email already exists or department not
         *                                  found
         */
        @Transactional
        public ResponseEntity<ResponseBody<EmployeeDTO>> addEmployee(EmployeeDTO employee) {
                log.info("Adding new employee with email: {}", employee.email());

                // Check if email already exists
                if (employeeRepository.existsByEmail(employee.email())) {
                        log.error("Employee with email {} already exists", employee.email());
                        throw new IllegalArgumentException(
                                        "Employee with email " + employee.email() + " already exists");
                }

                Employee newEmployee = Employee.builder()
                                .firstName(employee.firstName())
                                .lastName(employee.lastName())
                                .email(employee.email())
                                .build();

                // Assign department if departmentId is provided
                if (employee.departmentId() != null) {
                        Department department = departmentRepository.findById(employee.departmentId())
                                        .orElseThrow(() -> {
                                                log.error("Department not found with ID: {}", employee.departmentId());
                                                return new EntityNotFoundException("Department not found with id: "
                                                                + employee.departmentId());
                                        });
                        newEmployee.setDepartment(department);
                        log.info("Assigned employee to department: {}", department.getName());
                }

                employeeRepository.save(newEmployee);
                log.info("Successfully created employee with ID: {}", newEmployee.getId());

                // Create user account in auth-service
                try {
                        ServiceInstance serviceInstance = discoveryClient.getInstances("auth-service").get(0);
                        restClient.post().uri(serviceInstance.getUri() + "/api/v1/auth/register")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + utils.getJWTToken())
                                        .body(Map.of(
                                                        "email", employee.email(),
                                                        "name", employee.firstName() + " " + employee.lastName(),
                                                        "password", "password" // In real scenario, generate or receive
                                                                               // a password
                                        ))
                                        .retrieve()
                                        .body(Object.class);
                        log.info("Successfully created auth account for employee: {}", employee.email());
                } catch (Exception e) {
                        log.error("Failed to create auth account for employee: {}", employee.email(), e);
                        // Continue even if auth account creation fails
                }

                EmployeeDTO createdEmployee = convertToDTO(newEmployee);
                ResponseBody<EmployeeDTO> response = ResponseBody.success("Employee added successfully",
                                createdEmployee);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Retrieves all employees.
         *
         * @return ResponseEntity containing list of all employees
         */
        @Transactional(readOnly = true)
        public ResponseEntity<ResponseBody<List<EmployeeDTO>>> getEmployees() {
                log.info("Fetching all employees");

                List<EmployeeDTO> employees = employeeRepository.findAll().stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());

                log.info("Successfully retrieved {} employees", employees.size());
                ResponseBody<List<EmployeeDTO>> response = ResponseBody.success("Employees retrieved successfully",
                                employees);
                return ResponseEntity.ok(response);
        }

        /**
         * Retrieves a specific employee by ID.
         *
         * @param id Employee ID
         * @return ResponseEntity containing employee details
         * @throws EntityNotFoundException if employee not found
         */
        @Transactional(readOnly = true)
        public ResponseEntity<ResponseBody<EmployeeDTO>> getEmployeeById(Long id) {
                log.info("Fetching employee with ID: {}", id);

                Employee employee = employeeRepository.findById(id)
                                .orElseThrow(() -> {
                                        log.error("Employee not found with ID: {}", id);
                                        return new EntityNotFoundException("Employee not found with id: " + id);
                                });

                EmployeeDTO employeeDTO = convertToDTO(employee);
                log.info("Successfully retrieved employee: {} {}", employee.getFirstName(), employee.getLastName());

                ResponseBody<EmployeeDTO> response = ResponseBody.success("Employee retrieved successfully",
                                employeeDTO);
                return ResponseEntity.ok(response);
        }

        /**
         * Updates an existing employee.
         *
         * @param id       Employee ID
         * @param employee Updated employee data
         * @return ResponseEntity containing updated employee
         * @throws EntityNotFoundException  if employee or department not found
         * @throws IllegalArgumentException if trying to update email to an existing one
         */
        @Transactional
        public ResponseEntity<ResponseBody<EmployeeDTO>> updateEmployee(Long id, EmployeeDTO employee) {
                log.info("Updating employee with ID: {}", id);

                Employee existingEmployee = employeeRepository.findById(id)
                                .orElseThrow(() -> {
                                        log.error("Employee not found with ID: {}", id);
                                        return new EntityNotFoundException("Employee not found with id: " + id);
                                });

                // Check if email is being changed and if new email already exists
                if (!existingEmployee.getEmail().equals(employee.email()) &&
                                employeeRepository.existsByEmail(employee.email())) {
                        log.error("Email {} is already in use", employee.email());
                        throw new IllegalArgumentException("Email " + employee.email() + " is already in use");
                }

                existingEmployee.setFirstName(employee.firstName());
                existingEmployee.setLastName(employee.lastName());
                existingEmployee.setEmail(employee.email());

                // Update department if provided
                if (employee.departmentId() != null) {
                        Department department = departmentRepository.findById(employee.departmentId())
                                        .orElseThrow(() -> {
                                                log.error("Department not found with ID: {}", employee.departmentId());
                                                return new EntityNotFoundException("Department not found with id: "
                                                                + employee.departmentId());
                                        });
                        existingEmployee.setDepartment(department);
                        log.info("Updated employee department to: {}", department.getName());
                }

                employeeRepository.save(existingEmployee);
                log.info("Successfully updated employee with ID: {}", id);

                EmployeeDTO employeeDTO = convertToDTO(existingEmployee);
                ResponseBody<EmployeeDTO> response = ResponseBody.success("Employee updated successfully", employeeDTO);
                return ResponseEntity.ok(response);
        }

        /**
         * Deletes an employee by ID.
         *
         * @param id Employee ID
         * @return ResponseEntity with success message
         * @throws EntityNotFoundException if employee not found
         */
        @Transactional
        public ResponseEntity<ResponseBody<Void>> deleteEmployee(Long id) {
                log.info("Deleting employee with ID: {}", id);

                if (!employeeRepository.existsById(id)) {
                        log.error("Employee not found with ID: {}", id);
                        throw new EntityNotFoundException("Employee not found with id: " + id);
                }

                employeeRepository.deleteById(id);
                log.info("Successfully deleted employee with ID: {}", id);

                ResponseBody<Void> response = ResponseBody.success("Employee deleted successfully", null);
                return ResponseEntity.ok(response);
        }

        /**
         * Retrieves all employees in a specific department by department name.
         *
         * @param departmentName Name of the department
         * @return ResponseEntity containing list of employees in the department
         */
        @Transactional(readOnly = true)
        public ResponseEntity<ResponseBody<List<EmployeeDTO>>> getEmployeesByDepartment(String departmentName) {
                log.info("Fetching employees in department: {}", departmentName);

                List<EmployeeDTO> employees = employeeRepository.findByDepartmentNameIgnoreCase(departmentName).stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());

                log.info("Successfully retrieved {} employees in department: {}", employees.size(), departmentName);
                ResponseBody<List<EmployeeDTO>> response = ResponseBody.success(
                                "Employees retrieved successfully", employees);
                return ResponseEntity.ok(response);
        }

        /**
         * Retrieves the profile of the currently authenticated employee.
         *
         * @return ResponseEntity containing employee profile
         * @throws EntityNotFoundException if employee data not found
         */
        @Transactional(readOnly = true)
        public ResponseEntity<ResponseBody<EmployeeDTO>> getMyProfile() {
                String email = utils.getUserEmail();
                log.info("Fetching profile for employee with email: {}", email);

                Employee employee = employeeRepository.findByEmail(email)
                                .orElseThrow(() -> {
                                        log.error("Employee data not found for email: {}", email);
                                        return new EntityNotFoundException("Employee data not found");
                                });

                EmployeeDTO employeeDTO = convertToDTO(employee);
                log.info("Successfully retrieved profile for: {} {}", employee.getFirstName(), employee.getLastName());

                ResponseBody<EmployeeDTO> response = ResponseBody.success("Employee retrieved successfully",
                                employeeDTO);
                return ResponseEntity.ok(response);
        }

        /**
         * Converts an Employee entity to an EmployeeDTO.
         *
         * @param employee Employee entity
         * @return EmployeeDTO representation
         */
        private EmployeeDTO convertToDTO(Employee employee) {
                return EmployeeDTO.builder()
                                .id(employee.getId())
                                .firstName(employee.getFirstName())
                                .lastName(employee.getLastName())
                                .email(employee.getEmail())
                                .status(employee.getStatus())
                                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId()
                                                : null)
                                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName()
                                                : null)
                                .createdAt(employee.getCreatedAt())
                                .build();
        }
}
