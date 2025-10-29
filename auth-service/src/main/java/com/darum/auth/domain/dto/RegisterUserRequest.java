package com.darum.auth.domain.dto;

public record RegisterUserRequest(
        String email,
        String password,
        String name
) {
}
