package com.darum.employee.domain.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * Data Transfer Object for Department entity.
 * Used for transferring department data between layers.
 *
 * @param id            Unique identifier of the department
 * @param name          Name of the department
 * @param description   Description of the department
 * @param employeeCount Number of employees in the department
 * @param createdAt     Timestamp when the department was created
 */
@Builder
public record DepartmentDTO(
        Long id,
        String name,
        String description,
        Long employeeCount,
        Instant createdAt) {
}
