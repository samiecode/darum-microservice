package com.darum.employee.domain.dto;

import com.darum.employee.domain.enums.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;

/**
 * Data Transfer Object for Employee entity.
 * Used for transferring employee data between layers.
 *
 * @param id             Unique identifier of the employee
 * @param firstName      First name of the employee
 * @param lastName       Last name of the employee
 * @param email          Email address of the employee
 * @param status         Current employment status
 * @param departmentId   ID of the department the employee belongs to
 * @param departmentName Name of the department
 * @param createdAt      Timestamp when the employee record was created
 */
@Builder
public record EmployeeDTO(
                Long id,

                @NotBlank(message = "First name is required") @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters") String firstName,

                @NotBlank(message = "Last name is required") @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters") String lastName,

                @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email,

                EmployeeStatus status,
                Long departmentId,
                String departmentName,
                Instant createdAt) {
}
