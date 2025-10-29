package com.darum.auth.controller;

import com.darum.auth.domain.dto.AuthRequest;
import com.darum.auth.domain.dto.RegisterUserRequest;
import com.darum.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;

    @PostMapping("/login")
    ResponseEntity<?> authenticate(@RequestBody @Valid AuthRequest request) {
        return authService.authenticate(request);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<?> register(@RequestBody @Valid RegisterUserRequest request) {
        return authService.register(request);
    }
}
