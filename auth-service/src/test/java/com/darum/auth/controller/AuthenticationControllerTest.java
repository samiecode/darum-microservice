package com.darum.auth.controller;

import com.darum.auth.domain.dto.AuthRequest;
import com.darum.auth.domain.dto.AuthenticationResponse;
import com.darum.auth.domain.dto.RegisterUserRequest;
import com.darum.auth.service.AuthenticationService;
import com.darum.shareddomain.dto.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthRequest authRequest;
    private RegisterUserRequest registerRequest;
    private AuthenticationResponse authResponse;

    @BeforeEach
    void setUp() {
        authRequest = new AuthRequest("user@example.com", "password123");
        registerRequest = new RegisterUserRequest("user@example.com", "password123", "Test User");
        authResponse = new AuthenticationResponse(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                new Date(System.currentTimeMillis() + 3600000));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should authenticate user successfully")
    void authenticate_WithValidCredentials_ShouldReturn200() throws Exception {
        // Arrange
        ResponseEntity<ResponseBody<AuthenticationResponse>> response = ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.accessToken())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization")
                .body(ResponseBody.success("Login successfully", authResponse));

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION,
                        "Bearer " + authResponse.accessToken()))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Login successfully"))
                .andExpect(jsonPath("$.data.accessToken").value(authResponse.accessToken()))
                .andExpect(jsonPath("$.data.expiresAt").exists());

        verify(authService, times(1)).authenticate(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return token in Authorization header")
    void authenticate_ShouldReturnAuthorizationHeader() throws Exception {
        // Arrange
        String token = "mySecretToken123";
        AuthenticationResponse response = new AuthenticationResponse(token, new Date());
        ResponseEntity<ResponseBody<AuthenticationResponse>> responseEntity = ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(ResponseBody.success("Login successfully", response));

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(responseEntity);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should handle authentication failure")
    void authenticate_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should accept valid JSON request body")
    void authenticate_ShouldAcceptValidJsonRequestBody() throws Exception {
        // Arrange
        ResponseEntity<ResponseBody<AuthenticationResponse>> response = ResponseEntity
                .ok(ResponseBody.success("Login successfully", authResponse));

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(authService, times(1)).authenticate(any(AuthRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/auth/register - Should register user successfully with admin role")
    void register_WithAdminRole_ShouldReturn201() throws Exception {
        // Arrange
        ResponseEntity<ResponseBody<AuthenticationResponse>> response = ResponseEntity
                .ok(ResponseBody.success("User registered successfully", authResponse));

        when(authService.register(any(RegisterUserRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists());

        verify(authService, times(1)).register(any(RegisterUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("POST /api/v1/auth/register - Should deny access without admin role")
    void register_WithoutAdminRole_ShouldReturn403() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any(RegisterUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("POST /api/v1/auth/register - Should deny access with employee role")
    void register_WithEmployeeRole_ShouldReturn403() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any(RegisterUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/auth/register - Should accept valid registration request")
    void register_ShouldAcceptValidRegistrationRequest() throws Exception {
        // Arrange
        ResponseEntity<ResponseBody<AuthenticationResponse>> response = ResponseEntity
                .ok(ResponseBody.success("User registered successfully", authResponse));

//                when(authService.register(any(RegisterUserRequest.class))).thenReturn(response);

        String requestBody = """
                {
                    "email": "newuser@example.com",
                    "password": "securePassword123",
                    "name": "New User"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        verify(authService, times(1)).register(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return response with timestamp")
    void authenticate_ShouldReturnResponseWithTimestamp() throws Exception {
        // Arrange
        ResponseEntity<ResponseBody<AuthenticationResponse>> response = ResponseEntity
                .ok(ResponseBody.success("Login successfully", authResponse));

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should handle empty request body")
    void authenticate_WithEmptyBody_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/auth/register - Should handle empty registration request")
    void register_WithEmptyBody_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}