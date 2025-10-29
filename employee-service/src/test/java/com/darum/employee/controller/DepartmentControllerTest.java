package com.darum.employee.controller;

import com.darum.employee.domain.dto.DepartmentCreateRequest;
import com.darum.employee.domain.dto.DepartmentDTO;
import com.darum.employee.domain.dto.DepartmentUpdateRequest;
import com.darum.employee.domain.entity.Department;
import com.darum.employee.domain.repository.DepartmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DepartmentController Integration Tests")
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    private DepartmentCreateRequest createRequest;
    private DepartmentUpdateRequest updateRequest;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        departmentRepository.deleteAll();

        createRequest = new DepartmentCreateRequest(
                "Engineering",
                "Engineering Department");

        updateRequest = new DepartmentUpdateRequest(
                "Engineering Updated",
                "Updated Description");

        testDepartment = Department.builder()
                .name("IT Department")
                .description("Information Technology")
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create department with valid data")
    void createDepartment_WithValidData_ShouldReturn201() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Department created successfully"))
                .andExpect(jsonPath("$.data.name").value("Engineering"))
                .andExpect(jsonPath("$.data.description").value("Engineering Department"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.employeeCount").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to create department")
    void createDepartment_WithoutAdminRole_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated")
    void createDepartment_WithoutAuthentication_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when name is blank")
    void createDepartment_WithBlankName_ShouldReturn400() throws Exception {
        DepartmentCreateRequest invalidRequest = new DepartmentCreateRequest(
                "",
                "Description");

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when name is too short")
    void createDepartment_WithShortName_ShouldReturn400() throws Exception {
        DepartmentCreateRequest invalidRequest = new DepartmentCreateRequest(
                "A",
                "Description");

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when name is too long")
    void createDepartment_WithLongName_ShouldReturn400() throws Exception {
        String longName = "A".repeat(101);
        DepartmentCreateRequest invalidRequest = new DepartmentCreateRequest(
                longName,
                "Description");

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when description is too long")
    void createDepartment_WithLongDescription_ShouldReturn400() throws Exception {
        String longDescription = "A".repeat(501);
        DepartmentCreateRequest invalidRequest = new DepartmentCreateRequest(
                "Valid Name",
                longDescription);

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when creating duplicate department name")
    void createDepartment_WithDuplicateName_ShouldReturn400() throws Exception {
        // Create first department
        departmentRepository.save(testDepartment);

        DepartmentCreateRequest duplicateRequest = new DepartmentCreateRequest(
                "IT Department",
                "Duplicate");

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    // ========== READ TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all departments")
    void getAllDepartments_ShouldReturn200WithList() throws Exception {
        // Create test data
        departmentRepository.save(testDepartment);
        departmentRepository.save(Department.builder()
                .name("HR Department")
                .description("Human Resources")
                .build());

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].employeeCount").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty list when no departments exist")
    void getAllDepartments_WithNoData_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get department by ID")
    void getDepartmentById_WithValidId_ShouldReturn200() throws Exception {
        Department saved = departmentRepository.save(testDepartment);

        mockMvc.perform(get("/api/v1/departments/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.name").value("IT Department"))
                .andExpect(jsonPath("$.data.description").value("Information Technology"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when department not found by ID")
    void getDepartmentById_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/departments/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get department by name")
    void getDepartmentByName_WithValidName_ShouldReturn200() throws Exception {
        departmentRepository.save(testDepartment);

        mockMvc.perform(get("/api/v1/departments/name/{name}", "IT Department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.name").value("IT Department"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should be case insensitive when searching by name")
    void getDepartmentByName_WithDifferentCase_ShouldReturn200() throws Exception {
        departmentRepository.save(testDepartment);

        mockMvc.perform(get("/api/v1/departments/name/{name}", "it department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.name").value("IT Department"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when department not found by name")
    void getDepartmentByName_WithInvalidName_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/departments/name/{name}", "NonExistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    // ========== UPDATE TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update department with valid data")
    void updateDepartment_WithValidData_ShouldReturn200() throws Exception {
        Department saved = departmentRepository.save(testDepartment);

        mockMvc.perform(put("/api/v1/departments/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("updated successfully")))
                .andExpect(jsonPath("$.data.name").value("Engineering Updated"))
                .andExpect(jsonPath("$.data.description").value("Updated Description"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should partially update department")
    void updateDepartment_WithPartialData_ShouldUpdateOnlyProvidedFields() throws Exception {
        Department saved = departmentRepository.save(testDepartment);
        DepartmentUpdateRequest partialUpdate = new DepartmentUpdateRequest(
                null,
                "New Description Only");

        mockMvc.perform(put("/api/v1/departments/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("IT Department"))
                .andExpect(jsonPath("$.data.description").value("New Description Only"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to update")
    void updateDepartment_WithoutAdminRole_ShouldReturn403() throws Exception {
        Department saved = departmentRepository.save(testDepartment);

        mockMvc.perform(put("/api/v1/departments/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when updating non-existent department")
    void updateDepartment_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(put("/api/v1/departments/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when updating to duplicate name")
    void updateDepartment_WithDuplicateName_ShouldReturn400() throws Exception {
        Department dept1 = departmentRepository.save(testDepartment);
        departmentRepository.save(Department.builder()
                .name("HR Department")
                .description("HR")
                .build());

        DepartmentUpdateRequest duplicateUpdate = new DepartmentUpdateRequest(
                "HR Department",
                "Trying to use existing name");

        mockMvc.perform(put("/api/v1/departments/{id}", dept1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    // ========== DELETE TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete department successfully")
    void deleteDepartment_WithValidId_ShouldReturn200() throws Exception {
        Department saved = departmentRepository.save(testDepartment);

        mockMvc.perform(delete("/api/v1/departments/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("deleted successfully")));

        // Verify deletion
        mockMvc.perform(get("/api/v1/departments/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when non-admin tries to delete")
    void deleteDepartment_WithoutAdminRole_ShouldReturn403() throws Exception {
        Department saved = departmentRepository.save(testDepartment);

        mockMvc.perform(delete("/api/v1/departments/{id}", saved.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when deleting non-existent department")
    void deleteDepartment_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/v1/departments/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when request body is missing")
    void createDepartment_WithoutBody_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when request body is invalid JSON")
    void createDepartment_WithInvalidJson_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"invalid\" \"json\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should trim whitespace from name")
    void createDepartment_WithWhitespaceInName_ShouldTrimAndSave() throws Exception {
        DepartmentCreateRequest requestWithSpaces = new DepartmentCreateRequest(
                "  Sales Department  ",
                "Sales");

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithSpaces)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Sales Department"));
    }
}
