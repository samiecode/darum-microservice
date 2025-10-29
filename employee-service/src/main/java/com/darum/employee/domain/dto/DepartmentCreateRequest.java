package com.darum.employee.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new department.
 * Contains validation constraints to ensure data integrity.
 *
 * @param name        Name of the department (required, 2-100 characters)
 * @param description Description of the department (optional, max 500
 *                    characters)
 */
public record DepartmentCreateRequest(
        @NotBlank(message = "Department name is required") @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters") String name,

        @Size(max = 500, message = "Description must not exceed 500 characters") String description) {
}
