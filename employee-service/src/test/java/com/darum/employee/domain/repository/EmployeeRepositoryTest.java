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
 * Data JPA tests for EmployeeRepository.
 * Tests custom queries, relationships, and constraints.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("EmployeeRepository Tests")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Employee testEmployee;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        testDepartment = Department.builder()
                .name("Engineering")
                .description("Engineering Department")
                .build();

        testEmployee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();
    }

    // ========== BASIC CRUD TESTS ==========

    @Test
    @DisplayName("Should save employee successfully")
    void save_ShouldPersistEmployee() {
        // Act
        Employee saved = employeeRepository.save(testEmployee);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find employee by ID")
    void findById_WithValidId_ShouldReturnEmployee() {
        // Arrange
        Employee saved = employeeRepository.save(testEmployee);

        // Act
        Optional<Employee> found = employeeRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should find all employees")
    void findAll_ShouldReturnAllEmployees() {
        // Arrange
        employeeRepository.save(testEmployee);
        employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build());

        // Act
        List<Employee> employees = employeeRepository.findAll();

        // Assert
        assertThat(employees).hasSize(2);
    }

    @Test
    @DisplayName("Should delete employee")
    void delete_ShouldRemoveEmployee() {
        // Arrange
        Employee saved = employeeRepository.save(testEmployee);

        // Act
        employeeRepository.delete(saved);

        // Assert
        Optional<Employee> found = employeeRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    // ========== CUSTOM QUERY TESTS ==========

    @Test
    @DisplayName("Should find employee by email")
    void findByEmail_WithValidEmail_ShouldReturnEmployee() {
        // Arrange
        employeeRepository.save(testEmployee);

        // Act
        Optional<Employee> found = employeeRepository.findByEmail("john.doe@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_WithInvalidEmail_ShouldReturnEmpty() {
        // Act
        Optional<Employee> found = employeeRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if employee exists by email")
    void existsByEmail_ShouldReturnTrue() {
        // Arrange
        employeeRepository.save(testEmployee);

        // Act
        boolean exists = employeeRepository.existsByEmail("john.doe@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email doesn't exist")
    void existsByEmail_WithInvalidEmail_ShouldReturnFalse() {
        // Act
        boolean exists = employeeRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find employees by department ID")
    void findByDepartmentId_ShouldReturnEmployees() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(saved);
        employeeRepository.save(testEmployee);

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(saved)
                .build();
        employeeRepository.save(employee2);

        // Act
        List<Employee> employees = employeeRepository.findByDepartmentId(saved.getId());

        // Assert
        assertThat(employees).hasSize(2);
        assertThat(employees).extracting(Employee::getEmail)
                .containsExactlyInAnyOrder("john.doe@example.com", "jane@example.com");
    }

    @Test
    @DisplayName("Should return empty list for department with no employees")
    void findByDepartmentId_WithNoEmployees_ShouldReturnEmptyList() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);

        // Act
        List<Employee> employees = employeeRepository.findByDepartmentId(saved.getId());

        // Assert
        assertThat(employees).isEmpty();
    }

    @Test
    @DisplayName("Should find employees by status")
    void findByStatus_ShouldReturnEmployeesWithStatus() {
        // Arrange
        employeeRepository.save(testEmployee);
        employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .status(EmployeeStatus.INACTIVE)
                .build());

        // Act
        List<Employee> activeEmployees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
        List<Employee> inactiveEmployees = employeeRepository.findByStatus(EmployeeStatus.INACTIVE);

        // Assert
        assertThat(activeEmployees).hasSize(1);
        assertThat(activeEmployees.get(0).getEmail()).isEqualTo("john.doe@example.com");
        assertThat(inactiveEmployees).hasSize(1);
        assertThat(inactiveEmployees.get(0).getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Should find employees by department name ignoring case")
    void findByDepartmentNameIgnoreCase_ShouldReturnEmployees() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(saved);
        employeeRepository.save(testEmployee);

        // Act
        List<Employee> employees = employeeRepository.findByDepartmentNameIgnoreCase("engineering");

        // Assert
        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).getDepartment().getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Should return empty list when department name not found")
    void findByDepartmentNameIgnoreCase_WithInvalidName_ShouldReturnEmptyList() {
        // Act
        List<Employee> employees = employeeRepository.findByDepartmentNameIgnoreCase("NonExistent");

        // Assert
        assertThat(employees).isEmpty();
    }

    // ========== CONSTRAINT TESTS ==========

    @Test
    @DisplayName("Should enforce unique email constraint")
    void save_WithDuplicateEmail_ShouldThrowException() {
        // Arrange
        employeeRepository.save(testEmployee);
        Employee duplicate = Employee.builder()
                .firstName("Different")
                .lastName("Person")
                .email("john.doe@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> {
            employeeRepository.save(duplicate);
            employeeRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should allow updating existing employee")
    void save_UpdateExisting_ShouldSucceed() {
        // Arrange
        Employee saved = employeeRepository.save(testEmployee);

        // Act
        saved.setFirstName("John Updated");
        Employee updated = employeeRepository.save(saved);

        // Assert
        assertThat(updated.getFirstName()).isEqualTo("John Updated");
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    // ========== RELATIONSHIP TESTS ==========

    @Test
    @DisplayName("Should maintain relationship with department")
    void save_WithDepartment_ShouldMaintainRelationship() {
        // Arrange
        Department saved = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(saved);

        // Act
        Employee savedEmployee = employeeRepository.save(testEmployee);

        // Assert
        assertThat(savedEmployee.getDepartment()).isNotNull();
        assertThat(savedEmployee.getDepartment().getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Should allow employee without department")
    void save_WithoutDepartment_ShouldSucceed() {
        // Act
        Employee saved = employeeRepository.save(testEmployee);

        // Assert
        assertThat(saved.getDepartment()).isNull();
    }

    @Test
    @DisplayName("Should update department assignment")
    void save_ChangeDepartment_ShouldUpdateRelationship() {
        // Arrange
        Department dept1 = departmentRepository.save(testDepartment);
        Department dept2 = departmentRepository.save(Department.builder()
                .name("HR")
                .description("Human Resources")
                .build());

        testEmployee.setDepartment(dept1);
        Employee saved = employeeRepository.save(testEmployee);

        // Act
        saved.setDepartment(dept2);
        Employee updated = employeeRepository.save(saved);

        // Assert
        assertThat(updated.getDepartment().getName()).isEqualTo("HR");
    }

    @Test
    @DisplayName("Should allow removing department assignment")
    void save_RemoveDepartment_ShouldSetNull() {
        // Arrange
        Department dept = departmentRepository.save(testDepartment);
        testEmployee.setDepartment(dept);
        Employee saved = employeeRepository.save(testEmployee);

        // Act
        saved.setDepartment(null);
        Employee updated = employeeRepository.save(saved);

        // Assert
        assertThat(updated.getDepartment()).isNull();
    }

    // ========== AUDITING TESTS ==========

    @Test
    @DisplayName("Should automatically set createdAt timestamp")
    void save_ShouldSetCreatedAt() {
        // Act
        Employee saved = employeeRepository.save(testEmployee);

        // Assert
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should automatically update updatedAt timestamp")
    void save_UpdateExisting_ShouldUpdateTimestamp() throws InterruptedException {
        // Arrange
        Employee saved = employeeRepository.save(testEmployee);
        Thread.sleep(10); // Ensure time difference

        // Act
        saved.setFirstName("Updated");
        Employee updated = employeeRepository.save(saved);

        // Assert
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    // ========== STATUS TESTS ==========

    @Test
    @DisplayName("Should default to ACTIVE status")
    void save_WithoutStatus_ShouldDefaultToActive() {
        // Arrange
        Employee employee = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        // Act
        Employee saved = employeeRepository.save(employee);

        // Assert
        assertThat(saved.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should allow changing employee status")
    void save_ChangeStatus_ShouldUpdate() {
        // Arrange
        Employee saved = employeeRepository.save(testEmployee);

        // Act
        saved.setStatus(EmployeeStatus.INACTIVE);
        Employee updated = employeeRepository.save(saved);

        // Assert
        assertThat(updated.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
    }

    // ========== QUERY COMBINATION TESTS ==========

    @Test
    @DisplayName("Should find active employees in specific department")
    void findByDepartmentIdAndStatus_ShouldReturnFilteredEmployees() {
        // Arrange
        Department dept = departmentRepository.save(testDepartment);

        Employee active1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(dept)
                .build();
        employeeRepository.save(active1);

        Employee inactive = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .status(EmployeeStatus.INACTIVE)
                .department(dept)
                .build();
        employeeRepository.save(inactive);

        // Act
        List<Employee> deptEmployees = employeeRepository.findByDepartmentId(dept.getId());
        List<Employee> activeEmployees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE);

        // Assert
        assertThat(deptEmployees).hasSize(2);
        assertThat(activeEmployees).hasSize(1);
        assertThat(activeEmployees.get(0).getEmail()).isEqualTo("john@example.com");
    }
}
