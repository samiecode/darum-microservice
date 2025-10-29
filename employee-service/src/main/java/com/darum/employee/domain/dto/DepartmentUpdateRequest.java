package com.darum.employee.domain.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing department.
 * All fields are optional to support partial updates.
 *
 * @param name        Updated name of the department (optional, 2-100 characters
 *                    if provided)
 * @param description Updated description of the department (optional, max 500
 *                    characters)
 */
public record DepartmentUpdateRequest(
        @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters") String name,

        @Size(max = 500, message = "Description must not exceed 500 characters") String description) {
}
