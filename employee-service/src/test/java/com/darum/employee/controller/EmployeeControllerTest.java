package com.darum.employee.controller;

import com.darum.employee.domain.dto.EmployeeDTO;
import com.darum.employee.domain.entity.Department;
import com.darum.employee.domain.entity.Employee;
import com.darum.employee.domain.enums.EmployeeStatus;
import com.darum.employee.domain.repository.DepartmentRepository;
import com.darum.employee.domain.repository.EmployeeRepository;
import com.darum.shareddomain.lib.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("EmployeeController Integration Tests")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private Utils utils;

    private EmployeeDTO employeeDTO;
    private Employee testEmployee;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        testDepartment = Department.builder()
                .name("Engineering")
                .description("Engineering Department")
                .build();

        employeeDTO = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(EmployeeStatus.ACTIVE)
                .departmentId(null)
                .build();

        testEmployee = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create employee with valid data")
    void createEmployee_WithValidData_ShouldReturn201() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(employeeDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employee added successfully"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create employee with department assignment")
    void createEmployee_WithDepartment_ShouldAssignDepartment() throws Exception {
        Department saved = departmentRepository.save(testDepartment);
        EmployeeDTO dtoWithDept = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(saved.getId())
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoWithDept)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.departmentId").value(saved.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to create employee")
    void createEmployee_WithoutAdminRole_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(employeeDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated")
    void createEmployee_WithoutAuthentication_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(employeeDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when firstName is blank")
    void createEmployee_WithBlankFirstName_ShouldReturn400() throws Exception {
        EmployeeDTO invalidDto = EmployeeDTO.builder()
                .firstName("")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when lastName is too short")
    void createEmployee_WithShortLastName_ShouldReturn400() throws Exception {
        EmployeeDTO invalidDto = EmployeeDTO.builder()
                .firstName("John")
                .lastName("D")
                .email("john@example.com")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when firstName is too long")
    void createEmployee_WithLongFirstName_ShouldReturn400() throws Exception {
        String longName = "A".repeat(51);
        EmployeeDTO invalidDto = EmployeeDTO.builder()
                .firstName(longName)
                .lastName("Doe")
                .email("john@example.com")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when email is invalid")
    void createEmployee_WithInvalidEmail_ShouldReturn400() throws Exception {
        EmployeeDTO invalidDto = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when email is blank")
    void createEmployee_WithBlankEmail_ShouldReturn400() throws Exception {
        EmployeeDTO invalidDto = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when creating duplicate email")
    void createEmployee_WithDuplicateEmail_ShouldReturn400() throws Exception {
        // Create first employee
        employeeRepository.save(testEmployee);

        EmployeeDTO duplicateDto = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("jane.smith@example.com")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when creating employee with invalid department")
    void createEmployee_WithInvalidDepartmentId_ShouldReturn404() throws Exception {
        EmployeeDTO dtoWithInvalidDept = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(999L)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoWithInvalidDept)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Department not found")));
    }

    // ========== READ TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all employees")
    void getAllEmployees_ShouldReturn200WithList() throws Exception {
        // Create test data
        employeeRepository.save(testEmployee);
        employeeRepository.save(Employee.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].firstName").exists())
                .andExpect(jsonPath("$.data[0].email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty list when no employees exist")
    void getAllEmployees_WithNoData_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to get all employees")
    void getAllEmployees_WithoutAdminRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get employee by ID")
    void getEmployeeById_WithValidId_ShouldReturn200() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);

        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.email").value("jane.smith@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when employee not found by ID")
    void getEmployeeById_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to get employee by ID")
    void getEmployeeById_WithoutAdminRole_ShouldReturn403() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);

        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get employees by department name")
    void getEmployeesByDepartment_ShouldReturn200() throws Exception {
        Department dept = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(dept);
        employeeRepository.save(testEmployee);

        Employee emp2 = Employee.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(dept)
                .build();
        employeeRepository.save(emp2);

        mockMvc.perform(get("/api/v1/employees/department/{dept}", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].departmentName").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Should allow manager to get employees by department")
    void getEmployeesByDepartment_WithManagerRole_ShouldReturn200() throws Exception {
        Department dept = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(dept);
        employeeRepository.save(testEmployee);

        mockMvc.perform(get("/api/v1/employees/department/{dept}", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("Should return 403 when employee tries to get department employees")
    void getEmployeesByDepartment_WithEmployeeRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/employees/department/{dept}", "Engineering"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty list for department with no employees")
    void getEmployeesByDepartment_WithNoEmployees_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/employees/department/{dept}", "NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should be case insensitive when searching by department name")
    void getEmployeesByDepartment_WithDifferentCase_ShouldReturn200() throws Exception {
        Department dept = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(dept);
        employeeRepository.save(testEmployee);

        mockMvc.perform(get("/api/v1/employees/department/{dept}", "engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "jane.smith@example.com", roles = "EMPLOYEE")
    @DisplayName("Should get current user profile")
    void getMyProfile_ShouldReturn200() throws Exception {
        employeeRepository.save(testEmployee);
        when(utils.getUserEmail()).thenReturn("jane.smith@example.com");

        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").value("jane.smith@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Jane"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("Should allow admin to get their own profile")
    void getMyProfile_WithAdminRole_ShouldReturn200() throws Exception {
        Employee admin = Employee.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeRepository.save(admin);
        when(utils.getUserEmail()).thenReturn("admin@example.com");

        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@example.com"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Should allow manager to get their own profile")
    void getMyProfile_WithManagerRole_ShouldReturn200() throws Exception {
        Employee manager = Employee.builder()
                .firstName("Manager")
                .lastName("User")
                .email("manager@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeRepository.save(manager);
        when(utils.getUserEmail()).thenReturn("manager@example.com");

        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("manager@example.com"));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "EMPLOYEE")
    @DisplayName("Should return 404 when profile not found")
    void getMyProfile_WithNonExistentEmployee_ShouldReturn404() throws Exception {
        when(utils.getUserEmail()).thenReturn("nonexistent@example.com");

        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user tries to get profile")
    void getMyProfile_WithoutAuthentication_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isUnauthorized());
    }

    // ========== UPDATE TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update employee with valid data")
    void updateEmployee_WithValidData_ShouldReturn200() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);
        EmployeeDTO updateDto = EmployeeDTO.builder()
                .firstName("Jane Updated")
                .lastName("Smith Updated")
                .email("jane.updated@example.com")
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("updated successfully")))
                .andExpect(jsonPath("$.data.firstName").value("Jane Updated"))
                .andExpect(jsonPath("$.data.email").value("jane.updated@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update employee with department change")
    void updateEmployee_WithDepartmentChange_ShouldUpdateDepartment() throws Exception {
        Department dept1 = departmentRepository.save(testDepartment);
        Department dept2 = departmentRepository.save(Department.builder()
                .name("HR")
                .description("Human Resources")
                .build());

        testEmployee.setDepartment(dept1);
        Employee saved = employeeRepository.save(testEmployee);

        EmployeeDTO updateDto = EmployeeDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .departmentId(dept2.getId())
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").value(dept2.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("HR"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to update")
    void updateEmployee_WithoutAdminRole_ShouldReturn403() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);
        EmployeeDTO updateDto = EmployeeDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when updating non-existent employee")
    void updateEmployee_WithInvalidId_ShouldReturn404() throws Exception {
        EmployeeDTO updateDto = EmployeeDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when updating to duplicate email")
    void updateEmployee_WithDuplicateEmail_ShouldReturn400() throws Exception {
        employeeRepository.save(testEmployee);
        Employee emp2 = employeeRepository.save(Employee.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build());

        EmployeeDTO updateDto = EmployeeDTO.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("jane.smith@example.com")
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", emp2.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("already in use")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow updating with same email")
    void updateEmployee_WithSameEmail_ShouldSucceed() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);
        EmployeeDTO updateDto = EmployeeDTO.builder()
                .firstName("Jane Updated")
                .lastName("Smith Updated")
                .email("jane.smith@example.com")
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Jane Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when updating with validation errors")
    void updateEmployee_WithInvalidData_ShouldReturn400() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);
        EmployeeDTO invalidDto = EmployeeDTO.builder()
                .firstName("J")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete employee successfully")
    void deleteEmployee_WithValidId_ShouldReturn200() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);

        mockMvc.perform(delete("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("deleted successfully")));

        // Verify deletion
        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to delete")
    void deleteEmployee_WithoutAdminRole_ShouldReturn403() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);

        mockMvc.perform(delete("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when deleting non-existent employee")
    void deleteEmployee_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when request body is missing")
    void createEmployee_WithoutBody_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when request body is invalid JSON")
    void createEmployee_WithInvalidJson_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should include department info in employee response")
    void getEmployee_WithDepartment_ShouldIncludeDepartmentInfo() throws Exception {
        Department dept = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(dept);
        Employee saved = employeeRepository.save(testEmployee);

        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").value(dept.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle employee without department")
    void getEmployee_WithoutDepartment_ShouldReturnNullDepartment() throws Exception {
        Employee saved = employeeRepository.save(testEmployee);

        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").doesNotExist())
                .andExpect(jsonPath("$.data.departmentName").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle status in employee data")
    void createEmployee_WithStatus_ShouldPersistStatus() throws Exception {
        EmployeeDTO dtoWithStatus = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(EmployeeStatus.INACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoWithStatus)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }
}
