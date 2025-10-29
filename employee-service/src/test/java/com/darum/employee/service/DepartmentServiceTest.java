package com.darum.employee.service;

import com.darum.employee.domain.dto.DepartmentCreateRequest;
import com.darum.employee.domain.dto.DepartmentDTO;
import com.darum.employee.domain.dto.DepartmentUpdateRequest;
import com.darum.employee.domain.entity.Department;
import com.darum.employee.domain.repository.DepartmentRepository;
import com.darum.shareddomain.dto.ResponseBody;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService Unit Tests")
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department testDepartment;
    private DepartmentCreateRequest createRequest;
    private DepartmentUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testDepartment = Department.builder()
                .id(1L)
                .name("Engineering")
                .description("Software Engineering Department")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = new DepartmentCreateRequest("Engineering", "Software Engineering Department");
        updateRequest = new DepartmentUpdateRequest("Engineering Updated", "Updated description");
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Should create department successfully")
    void createDepartment_WithValidRequest_ShouldReturnCreatedDepartment() {
        // Arrange
        when(departmentRepository.existsByNameIgnoreCase(createRequest.name())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(0L);

        // Act
        ResponseEntity<ResponseBody<DepartmentDTO>> response = departmentService.createDepartment(createRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("success");
        assertThat(response.getBody().getData().name()).isEqualTo("Engineering");

        verify(departmentRepository, times(1)).existsByNameIgnoreCase(createRequest.name());
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    @DisplayName("Should throw exception when creating department with existing name")
    void createDepartment_WithDuplicateName_ShouldThrowException() {
        // Arrange
        when(departmentRepository.existsByNameIgnoreCase(createRequest.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(departmentRepository, times(1)).existsByNameIgnoreCase(createRequest.name());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Should save department with correct data")
    void createDepartment_ShouldSaveDepartmentWithCorrectData() {
        // Arrange
        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        when(departmentRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);
        when(departmentRepository.countEmployeesByDepartmentId(any())).thenReturn(0L);

        // Act
        departmentService.createDepartment(createRequest);

        // Assert
        verify(departmentRepository).save(departmentCaptor.capture());
        Department savedDepartment = departmentCaptor.getValue();
        assertThat(savedDepartment.getName()).isEqualTo(createRequest.name());
        assertThat(savedDepartment.getDescription()).isEqualTo(createRequest.description());
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("Should get all departments successfully")
    void getAllDepartments_ShouldReturnListOfDepartments() {
        // Arrange
        Department dept2 = Department.builder()
                .id(2L)
                .name("HR")
                .description("Human Resources")
                .createdAt(Instant.now())
                .build();

        when(departmentRepository.findAll()).thenReturn(Arrays.asList(testDepartment, dept2));
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(5L);
        when(departmentRepository.countEmployeesByDepartmentId(2L)).thenReturn(3L);

        // Act
        ResponseEntity<ResponseBody<List<DepartmentDTO>>> response = departmentService.getAllDepartments();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(2);
        assertThat(response.getBody().getData().get(0).employeeCount()).isEqualTo(5L);

        verify(departmentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get department by ID successfully")
    void getDepartmentById_WithValidId_ShouldReturnDepartment() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(5L);

        // Act
        ResponseEntity<ResponseBody<DepartmentDTO>> response = departmentService.getDepartmentById(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().id()).isEqualTo(1L);
        assertThat(response.getBody().getData().name()).isEqualTo("Engineering");

        verify(departmentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when department not found by ID")
    void getDepartmentById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> departmentService.getDepartmentById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(departmentRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get department by name successfully")
    void getDepartmentByName_WithValidName_ShouldReturnDepartment() {
        // Arrange
        when(departmentRepository.findByNameIgnoreCase("Engineering")).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(5L);

        // Act
        ResponseEntity<ResponseBody<DepartmentDTO>> response = departmentService.getDepartmentByName("Engineering");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().name()).isEqualTo("Engineering");

        verify(departmentRepository, times(1)).findByNameIgnoreCase("Engineering");
    }

    @Test
    @DisplayName("Should throw exception when department not found by name")
    void getDepartmentByName_WithInvalidName_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findByNameIgnoreCase("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> departmentService.getDepartmentByName("NonExistent"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(departmentRepository, times(1)).findByNameIgnoreCase("NonExistent");
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("Should update department successfully")
    void updateDepartment_WithValidRequest_ShouldReturnUpdatedDepartment() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.existsByNameIgnoreCase(updateRequest.name())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(5L);

        // Act
        ResponseEntity<ResponseBody<DepartmentDTO>> response = departmentService.updateDepartment(1L, updateRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("updated successfully");

        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    @DisplayName("Should update only name when only name provided")
    void updateDepartment_WithOnlyName_ShouldUpdateOnlyName() {
        // Arrange
        DepartmentUpdateRequest nameOnlyRequest = new DepartmentUpdateRequest("New Name", null);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.existsByNameIgnoreCase("New Name")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(0L);

        // Act
        departmentService.updateDepartment(1L, nameOnlyRequest);

        // Assert
        verify(departmentRepository).save(argThat(dept -> "New Name".equals(dept.getName())));
    }

    @Test
    @DisplayName("Should throw exception when updating to existing department name")
    void updateDepartment_WithDuplicateName_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.existsByNameIgnoreCase(updateRequest.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.updateDepartment(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent department")
    void updateDepartment_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> departmentService.updateDepartment(999L, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(departmentRepository, times(1)).findById(999L);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Should allow updating to same name")
    void updateDepartment_WithSameName_ShouldNotCheckDuplicate() {
        // Arrange
        DepartmentUpdateRequest sameNameRequest = new DepartmentUpdateRequest("Engineering", "New description");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(0L);

        // Act
        departmentService.updateDepartment(1L, sameNameRequest);

        // Assert
        verify(departmentRepository, never()).existsByNameIgnoreCase(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Should delete department successfully when no employees assigned")
    void deleteDepartment_WithNoEmployees_ShouldDeleteSuccessfully() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(0L);
        doNothing().when(departmentRepository).delete(testDepartment);

        // Act
        ResponseEntity<ResponseBody<Void>> response = departmentService.deleteDepartment(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("deleted successfully");

        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, times(1)).countEmployeesByDepartmentId(1L);
        verify(departmentRepository, times(1)).delete(testDepartment);
    }

    @Test
    @DisplayName("Should throw exception when deleting department with employees")
    void deleteDepartment_WithEmployees_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(5L);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assigned employees");

        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, times(1)).countEmployeesByDepartmentId(1L);
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent department")
    void deleteDepartment_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> departmentService.deleteDepartment(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(departmentRepository, times(1)).findById(999L);
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    @Test
    @DisplayName("Should handle data integrity violation during delete")
    void deleteDepartment_WithDataIntegrityViolation_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(0L);
        doThrow(new DataIntegrityViolationException("Foreign key constraint"))
                .when(departmentRepository).delete(testDepartment);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("existing references");

        verify(departmentRepository, times(1)).delete(testDepartment);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Should handle empty department list")
    void getAllDepartments_WithNoDepartments_ShouldReturnEmptyList() {
        // Arrange
        when(departmentRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<ResponseBody<List<DepartmentDTO>>> response = departmentService.getAllDepartments();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    @DisplayName("Should include employee count in DTO")
    void getDepartmentById_ShouldIncludeEmployeeCount() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(10L);

        // Act
        ResponseEntity<ResponseBody<DepartmentDTO>> response = departmentService.getDepartmentById(1L);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().employeeCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should handle null description in create request")
    void createDepartment_WithNullDescription_ShouldSucceed() {
        // Arrange
        DepartmentCreateRequest requestWithNullDesc = new DepartmentCreateRequest("Test Dept", null);
        when(departmentRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);
        when(departmentRepository.countEmployeesByDepartmentId(any())).thenReturn(0L);

        // Act
        ResponseEntity<ResponseBody<DepartmentDTO>> response = departmentService.createDepartment(requestWithNullDesc);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("Should handle null values in update request")
    void updateDepartment_WithAllNullFields_ShouldNotUpdateAnything() {
        // Arrange
        DepartmentUpdateRequest nullRequest = new DepartmentUpdateRequest(null, null);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);
        when(departmentRepository.countEmployeesByDepartmentId(1L)).thenReturn(0L);

        // Act
        departmentService.updateDepartment(1L, nullRequest);

        // Assert
        verify(departmentRepository).save(argThat(dept -> "Engineering".equals(dept.getName()) // Original name should
                                                                                               // remain
        ));
    }
}
