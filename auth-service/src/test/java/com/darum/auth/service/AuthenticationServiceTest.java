package com.darum.auth.service;

import com.darum.auth.domain.dto.AuthRequest;
import com.darum.auth.domain.dto.AuthenticationResponse;
import com.darum.auth.domain.dto.RegisterUserRequest;
import com.darum.auth.domain.entity.User;
import com.darum.auth.domain.enums.Role;
import com.darum.auth.domain.repository.UserRepository;
import com.darum.auth.jwt.JwtService;
import com.darum.auth.jwt.SecurityUser;
import com.darum.shareddomain.dto.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private SecurityUser securityUser;
    private AuthRequest authRequest;
    private RegisterUserRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .name("Test User")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .locked(false)
                .build();

        securityUser = new SecurityUser(testUser);
        authRequest = new AuthRequest("test@example.com", "password123");
        registerRequest = new RegisterUserRequest("test@example.com", "password123", "Test User");
    }

    @Test
    @DisplayName("Should successfully authenticate user with valid credentials")
    void authenticate_WithValidCredentials_ShouldReturnAuthenticationResponse() {
        // Arrange
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtService.generateToken(securityUser)).thenReturn(jwtToken);
        when(jwtService.getExpiration()).thenReturn(expirationDate);

        // Act
        ResponseEntity<ResponseBody<AuthenticationResponse>> response = authenticationService.authenticate(authRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.AUTHORIZATION))
                .contains("Bearer " + jwtToken);
        assertThat(response.getHeaders().get(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                .contains("Authorization");

        ResponseBody<AuthenticationResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("success");
        assertThat(body.getMessage()).isEqualTo("Login successfully");
        assertThat(body.getData()).isNotNull();
        assertThat(body.getData().token()).isEqualTo(jwtToken);
        assertThat(body.getData().expiresAt()).isEqualTo(expirationDate);

        // Verify interactions
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(securityUser);
        verify(jwtService, times(1)).getExpiration();
    }

    @Test
    @DisplayName("Should throw exception when authentication fails with invalid credentials")
    void authenticate_WithInvalidCredentials_ShouldThrowException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.authenticate(authRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should create authentication token with correct email and password")
    void authenticate_ShouldCreateCorrectAuthenticationToken() {
        // Arrange
        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor = ArgumentCaptor
                .forClass(UsernamePasswordAuthenticationToken.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(new Date());

        // Act
        authenticationService.authenticate(authRequest);

        // Assert
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        UsernamePasswordAuthenticationToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getPrincipal()).isEqualTo("test@example.com");
        assertThat(capturedToken.getCredentials()).isEqualTo("password123");
    }

    @Test
    @DisplayName("Should successfully register new user")
    void register_WithValidRequest_ShouldCreateUserAndReturnAuthResponse() {
        // Arrange
        String encodedPassword = "encodedPassword123";
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);

        when(passwordEncoder.encode(registerRequest.password())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtService.generateToken(securityUser)).thenReturn(jwtToken);
        when(jwtService.getExpiration()).thenReturn(expirationDate);

        // Act
        ResponseEntity<?> response = authenticationService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        ResponseBody<AuthenticationResponse> body = (ResponseBody<AuthenticationResponse>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("success");
        assertThat(body.getMessage()).isEqualTo("User registered successfully");
        assertThat(body.getData()).isNotNull();
        assertThat(body.getData().token()).isEqualTo(jwtToken);

        // Verify interactions
        verify(passwordEncoder, times(1)).encode(registerRequest.password());
        verify(userRepository, times(1)).save(any(User.class));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(securityUser);
    }

    @Test
    @DisplayName("Should save user with correct details during registration")
    void register_ShouldSaveUserWithCorrectDetails() {
        // Arrange
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        String encodedPassword = "encodedPassword123";

        when(passwordEncoder.encode(registerRequest.password())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(new Date());

        // Act
        authenticationService.register(registerRequest);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo(registerRequest.name());
        assertThat(savedUser.getEmail()).isEqualTo(registerRequest.email());
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getRole()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    @DisplayName("Should assign EMPLOYEE role to newly registered user")
    void register_ShouldAssignEmployeeRole() {
        // Arrange
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(new Date());

        // Act
        authenticationService.register(registerRequest);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    @DisplayName("Should authenticate user after successful registration")
    void register_ShouldAuthenticateAfterCreatingUser() {
        // Arrange
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(new Date());

        // Act
        authenticationService.register(registerRequest);

        // Assert
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(any());
    }

    @Test
    @DisplayName("Should handle password encoding during registration")
    void register_ShouldEncodePassword() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";
        RegisterUserRequest request = new RegisterUserRequest("user@example.com", rawPassword, "Test");

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(new Date());

        // Act
        authenticationService.register(request);

        // Assert
        verify(passwordEncoder, times(1)).encode(rawPassword);
    }
}
