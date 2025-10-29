package com.darum.employee.service;

import com.darum.employee.domain.dto.EmployeeDTO;
import com.darum.employee.domain.entity.Department;
import com.darum.employee.domain.entity.Employee;
import com.darum.employee.domain.enums.EmployeeStatus;
import com.darum.employee.domain.repository.DepartmentRepository;
import com.darum.employee.domain.repository.EmployeeRepository;
import com.darum.shareddomain.dto.ResponseBody;
import com.darum.shareddomain.lib.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private RestClient restClient;

    @Mock
    private Utils utils;

    @Mock
    private ServiceInstance serviceInstance;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private Department testDepartment;
    private EmployeeDTO employeeDTO;

    @BeforeEach
    void setUp() {
        testDepartment = Department.builder()
                .id(1L)
                .name("Engineering")
                .description("Engineering Department")
                .createdAt(Instant.now())
                .build();

        testEmployee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(EmployeeStatus.ACTIVE)
                .department(testDepartment)
                .createdAt(Instant.now())
                .build();

        employeeDTO = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Should create employee successfully without department")
    void addEmployee_WithoutDepartment_ShouldCreateSuccessfully() {
        // Arrange
        EmployeeDTO dtoWithoutDept = EmployeeDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .build();

        when(employeeRepository.existsByEmail(dtoWithoutDept.email())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        when(discoveryClient.getInstances("auth-service")).thenReturn(Arrays.asList(serviceInstance));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://localhost:8080"));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Object.class)).thenReturn(new Object());
        when(utils.getJWTToken()).thenReturn("mock-token");

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.addEmployee(dtoWithoutDept);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("success");

        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should create employee with department assignment")
    void addEmployee_WithDepartment_ShouldAssignDepartment() {
        // Arrange
        when(employeeRepository.existsByEmail(employeeDTO.email())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        when(discoveryClient.getInstances("auth-service")).thenReturn(Arrays.asList(serviceInstance));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://localhost:8080"));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Object.class)).thenReturn(new Object());
        when(utils.getJWTToken()).thenReturn("mock-token");

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.addEmployee(employeeDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(departmentRepository, times(1)).findById(1L);
        verify(employeeRepository)
                .save(argThat(emp -> emp.getDepartment() != null && emp.getDepartment().getId().equals(1L)));
    }

    @Test
    @DisplayName("Should throw exception when creating employee with duplicate email")
    void addEmployee_WithDuplicateEmail_ShouldThrowException() {
        // Arrange
        when(employeeRepository.existsByEmail(employeeDTO.email())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.addEmployee(employeeDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(employeeRepository, times(1)).existsByEmail(employeeDTO.email());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when department not found during employee creation")
    void addEmployee_WithInvalidDepartmentId_ShouldThrowException() {
        // Arrange
        when(employeeRepository.existsByEmail(employeeDTO.email())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.addEmployee(employeeDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Department not found");

        verify(departmentRepository, times(1)).findById(1L);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should continue even if auth service call fails")
    void addEmployee_WhenAuthServiceFails_ShouldStillCreateEmployee() {
        // Arrange
        EmployeeDTO dto = EmployeeDTO.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .build();

        when(employeeRepository.existsByEmail(dto.email())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        when(discoveryClient.getInstances("auth-service")).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.addEmployee(dto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("Should get all employees successfully")
    void getEmployees_ShouldReturnListOfEmployees() {
        // Arrange
        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findAll()).thenReturn(Arrays.asList(testEmployee, employee2));

        // Act
        ResponseEntity<ResponseBody<List<EmployeeDTO>>> response = employeeService.getEmployees();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(2);
        assertThat(response.getBody().getData().get(0).email()).isEqualTo("john.doe@example.com");

        verify(employeeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get employee by ID successfully")
    void getEmployeeById_WithValidId_ShouldReturnEmployee() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.getEmployeeById(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().id()).isEqualTo(1L);
        assertThat(response.getBody().getData().firstName()).isEqualTo("John");

        verify(employeeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when employee not found by ID")
    void getEmployeeById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.getEmployeeById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(employeeRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get employees by department name")
    void getEmployeesByDepartment_ShouldReturnEmployeesInDepartment() {
        // Arrange
        when(employeeRepository.findByDepartmentNameIgnoreCase("Engineering"))
                .thenReturn(Arrays.asList(testEmployee));

        // Act
        ResponseEntity<ResponseBody<List<EmployeeDTO>>> response = employeeService
                .getEmployeesByDepartment("Engineering");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).departmentName()).isEqualTo("Engineering");

        verify(employeeRepository, times(1)).findByDepartmentNameIgnoreCase("Engineering");
    }

    @Test
    @DisplayName("Should get my profile successfully")
    void getMyProfile_ShouldReturnCurrentEmployeeProfile() {
        // Arrange
        when(utils.getUserEmail()).thenReturn("john.doe@example.com");
        when(employeeRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(testEmployee));

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.getMyProfile();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().email()).isEqualTo("john.doe@example.com");

        verify(utils, times(1)).getUserEmail();
        verify(employeeRepository, times(1)).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Should throw exception when profile not found")
    void getMyProfile_WhenEmployeeNotFound_ShouldThrowException() {
        // Arrange
        when(utils.getUserEmail()).thenReturn("nonexistent@example.com");
        when(employeeRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.getMyProfile())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Employee data not found");
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("Should update employee successfully")
    void updateEmployee_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        EmployeeDTO updateDTO = EmployeeDTO.builder()
                .firstName("John Updated")
                .lastName("Doe Updated")
                .email("john.updated@example.com")
                .departmentId(1L)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.existsByEmail(updateDTO.email())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.updateEmployee(1L, updateDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("updated successfully");

        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when updating to duplicate email")
    void updateEmployee_WithDuplicateEmail_ShouldThrowException() {
        // Arrange
        EmployeeDTO updateDTO = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("duplicate@example.com")
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.updateEmployee(1L, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should allow updating with same email")
    void updateEmployee_WithSameEmail_ShouldAllowUpdate() {
        // Arrange
        EmployeeDTO updateDTO = EmployeeDTO.builder()
                .firstName("John Updated")
                .lastName("Doe Updated")
                .email("john.doe@example.com")
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.updateEmployee(1L, updateDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(employeeRepository, never()).existsByEmail(any());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent employee")
    void updateEmployee_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.updateEmployee(999L, employeeDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw exception when updating with invalid department")
    void updateEmployee_WithInvalidDepartmentId_ShouldThrowException() {
        // Arrange
        EmployeeDTO updateDTO = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(999L)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.updateEmployee(1L, updateDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Department not found");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Should delete employee successfully")
    void deleteEmployee_WithValidId_ShouldDeleteSuccessfully() {
        // Arrange
        when(employeeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(1L);

        // Act
        ResponseEntity<ResponseBody<Void>> response = employeeService.deleteEmployee(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("deleted successfully");

        verify(employeeRepository, times(1)).existsById(1L);
        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent employee")
    void deleteEmployee_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(employeeRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.deleteEmployee(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(employeeRepository, never()).deleteById(any());
    }

    // ========== DTO CONVERSION TESTS ==========

    @Test
    @DisplayName("Should convert employee with department to DTO correctly")
    void convertToDTO_WithDepartment_ShouldIncludeDepartmentInfo() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.getEmployeeById(1L);

        // Assert
        EmployeeDTO dto = response.getBody().getData();
        assertThat(dto.departmentId()).isEqualTo(1L);
        assertThat(dto.departmentName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Should convert employee without department to DTO correctly")
    void convertToDTO_WithoutDepartment_ShouldHaveNullDepartmentInfo() {
        // Arrange
        Employee employeeWithoutDept = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employeeWithoutDept));

        // Act
        ResponseEntity<ResponseBody<EmployeeDTO>> response = employeeService.getEmployeeById(1L);

        // Assert
        EmployeeDTO dto = response.getBody().getData();
        assertThat(dto.departmentId()).isNull();
        assertThat(dto.departmentName()).isNull();
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Should handle empty employee list")
    void getEmployees_WithNoEmployees_ShouldReturnEmptyList() {
        // Arrange
        when(employeeRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<ResponseBody<List<EmployeeDTO>>> response = employeeService.getEmployees();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty department employee list")
    void getEmployeesByDepartment_WithNoEmployees_ShouldReturnEmptyList() {
        // Arrange
        when(employeeRepository.findByDepartmentNameIgnoreCase("EmptyDept"))
                .thenReturn(Arrays.asList());

        // Act
        ResponseEntity<ResponseBody<List<EmployeeDTO>>> response = employeeService
                .getEmployeesByDepartment("EmptyDept");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    @DisplayName("Should save employee with correct data")
    void addEmployee_ShouldSaveWithCorrectData() {
        // Arrange
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        when(employeeRepository.existsByEmail(any())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        when(discoveryClient.getInstances("auth-service")).thenReturn(Arrays.asList());

        // Act
        employeeService.addEmployee(employeeDTO);

        // Assert
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee savedEmployee = employeeCaptor.getValue();
        assertThat(savedEmployee.getFirstName()).isEqualTo(employeeDTO.firstName());
        assertThat(savedEmployee.getLastName()).isEqualTo(employeeDTO.lastName());
        assertThat(savedEmployee.getEmail()).isEqualTo(employeeDTO.email());
    }
}
