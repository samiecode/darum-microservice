package com.darum.auth.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    ADMIN_ADD("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_DELETE("admin:delete"),
    ADMIN_VIEW("admin:view"),
    MANAGER_VIEW("manager:view"),
    EMPLOYEE_VIEW("employee:view");

    private final String permission;
}