package com.darum.employee.domain.repository;

import com.darum.employee.domain.entity.Department;
import com.darum.employee.domain.entity.Employee;
import com.darum.employee.domain.enums.EmployeeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Data JPA tests for DepartmentRepository.
 * Tests custom queries, relationships, and constraints.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DepartmentRepository Tests")
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Department testDepartment;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        testDepartment = Department.builder()
                .name("Engineering")
                .description("Engineering Department")
                .build();
    }

    // ========== BASIC CRUD TESTS ==========

    @Test
    @DisplayName("Should save department successfully")
    void save_ShouldPersistDepartment() {
        // Act
        Department saved = departmentRepository.save(testDepartment);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Engineering");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find department by ID")
    void findById_WithValidId_ShouldReturnDepartment() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        // Act
        Optional<Department> found = departmentRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Should find all departments")
    void findAll_ShouldReturnAllDepartments() {
        // Arrange
        departmentRepository.save(testDepartment);
        departmentRepository.save(Department.builder()
                .name("HR")
                .description("Human Resources")
                .build());

        // Act
        List<Department> departments = departmentRepository.findAll();

        // Assert
        assertThat(departments).hasSize(2);
    }

    @Test
    @DisplayName("Should delete department")
    void delete_ShouldRemoveDepartment() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        // Act
        departmentRepository.delete(saved);

        // Assert
        Optional<Department> found = departmentRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    // ========== CUSTOM QUERY TESTS ==========

    @Test
    @DisplayName("Should find department by name ignoring case")
    void findByNameIgnoreCase_ShouldReturnDepartment() {
        // Arrange
        departmentRepository.save(testDepartment);

        // Act
        Optional<Department> found = departmentRepository.findByNameIgnoreCase("engineering");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Should return empty when department name not found")
    void findByNameIgnoreCase_WithInvalidName_ShouldReturnEmpty() {
        // Act
        Optional<Department> found = departmentRepository.findByNameIgnoreCase("NonExistent");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if department exists by name ignoring case")
    void existsByNameIgnoreCase_ShouldReturnTrue() {
        // Arrange
        departmentRepository.save(testDepartment);

        // Act
        boolean exists = departmentRepository.existsByNameIgnoreCase("ENGINEERING");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when department name doesn't exist")
    void existsByNameIgnoreCase_WithInvalidName_ShouldReturnFalse() {
        // Act
        boolean exists = departmentRepository.existsByNameIgnoreCase("NonExistent");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should count employees in department")
    void countEmployeesByDepartmentId_ShouldReturnCorrectCount() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(saved)
                .build());

        employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(saved)
                .build());

        // Act
        long count = departmentRepository.countEmployeesByDepartmentId(saved.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero for department with no employees")
    void countEmployeesByDepartmentId_WithNoEmployees_ShouldReturnZero() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        // Act
        long count = departmentRepository.countEmployeesByDepartmentId(saved.getId());

        // Assert
        assertThat(count).isZero();
    }

    // ========== CONSTRAINT TESTS ==========

    @Test
    @DisplayName("Should enforce unique department name")
    void save_WithDuplicateName_ShouldThrowException() {
        // Arrange
        departmentRepository.save(testDepartment);
        Department duplicate = Department.builder()
                .name("Engineering")
                .description("Different description")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> {
            departmentRepository.save(duplicate);
            departmentRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should allow updating existing department")
    void save_UpdateExisting_ShouldSucceed() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        // Act
        saved.setDescription("Updated Description");
        Department updated = departmentRepository.save(saved);

        // Assert
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    // ========== RELATIONSHIP TESTS ==========

    @Test
    @DisplayName("Should maintain relationship with employees")
    void save_WithEmployees_ShouldMaintainRelationship() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(saved)
                .build();
        employeeRepository.save(employee);

        // Act
        Department found = departmentRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertThat(found.getEmployees()).hasSize(1);
        assertThat(found.getEmployees().get(0).getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should cascade delete to employees when configured")
    void delete_ShouldHandleEmployeeRelationship() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(saved)
                .build();
        employeeRepository.save(employee);

        // Act - Attempting to delete department with employees
        // This should either cascade or be prevented based on cascade configuration
        departmentRepository.delete(saved);
        departmentRepository.flush();

        // Assert
        Optional<Department> foundDept = departmentRepository.findById(saved.getId());
        assertThat(foundDept).isEmpty();
    }

    // ========== AUDITING TESTS ==========

    @Test
    @DisplayName("Should automatically set createdAt timestamp")
    void save_ShouldSetCreatedAt() {
        // Act
        Department saved = departmentRepository.save(testDepartment);

        // Assert
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should automatically update updatedAt timestamp")
    void save_UpdateExisting_ShouldUpdateTimestamp() throws InterruptedException {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);
        Thread.sleep(10); // Ensure time difference

        // Act
        saved.setDescription("Updated");
        Department updated = departmentRepository.save(saved);

        // Assert
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }
}
