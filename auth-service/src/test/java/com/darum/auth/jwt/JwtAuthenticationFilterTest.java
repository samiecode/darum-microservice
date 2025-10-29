package com.darum.auth.jwt;

import com.darum.auth.domain.entity.User;
import com.darum.auth.domain.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private SecurityUser securityUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        User testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .name("Test User")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .locked(false)
                .build();

        securityUser = new SecurityUser(testUser);
        validToken = "valid.jwt.token";
    }

    @Test
    @DisplayName("Should authenticate user with valid Bearer token")
    void doFilterInternal_WithValidToken_ShouldAuthenticateUser() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(securityUser);
        when(jwtService.isTokenValid(validToken, securityUser)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(securityUser);
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should continue filter chain without authentication when no Authorization header")
    void doFilterInternal_WithoutAuthorizationHeader_ShouldContinueFilterChain() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header doesn't start with Bearer")
    void doFilterInternal_WithoutBearerPrefix_ShouldContinueFilterChain() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should extract token correctly from Bearer header")
    void doFilterInternal_ShouldExtractTokenAfterBearerPrefix() throws ServletException, IOException {
        // Arrange
        String token = "mytoken123";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(securityUser);
        when(jwtService.isTokenValid(token, securityUser)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService, times(1)).extractUsername(token);
    }

    @Test
    @DisplayName("Should send error response when token is invalid")
    void doFilterInternal_WithInvalidToken_ShouldSendErrorResponse() throws ServletException, IOException {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(securityUser);
        when(jwtService.isTokenValid(validToken, securityUser)).thenReturn(false);
        when(response.getWriter()).thenReturn(writer);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).getWriter();

        String jsonResponse = stringWriter.toString();
        assertThat(jsonResponse).contains("Invalid or expired token");

        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should not authenticate when user already authenticated")
    void doFilterInternal_WhenAlreadyAuthenticated_ShouldNotReAuthenticate() throws ServletException, IOException {
        // Arrange
        UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken(
                securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).isTokenValid(any(), any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should load user details using extracted username")
    void doFilterInternal_ShouldLoadUserDetailsByUsername() throws ServletException, IOException {
        // Arrange
        String username = "test@example.com";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(securityUser);
        when(jwtService.isTokenValid(validToken, securityUser)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(userDetailsService, times(1)).loadUserByUsername(username);
    }

    @Test
    @DisplayName("Should set authentication with correct details")
    void doFilterInternal_ShouldSetAuthenticationWithWebAuthenticationDetails() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(securityUser);
        when(jwtService.isTokenValid(validToken, securityUser)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(securityUser);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).isEqualTo(securityUser.getAuthorities());
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    @DisplayName("Should not authenticate when username is null")
    void doFilterInternal_WhenUsernameIsNull_ShouldNotAuthenticate() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should continue filter chain after successful authentication")
    void doFilterInternal_AfterSuccessfulAuth_ShouldContinueFilterChain() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(securityUser);
        when(jwtService.isTokenValid(validToken, securityUser)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear security context when token validation fails")
    void doFilterInternal_WithInvalidToken_ShouldNotSetSecurityContext() throws ServletException, IOException {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(securityUser);
        when(jwtService.isTokenValid(validToken, securityUser)).thenReturn(false);
        when(response.getWriter()).thenReturn(writer);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle user not found exception gracefully")
    void doFilterInternal_WhenUserNotFound_ShouldThrowException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("nonexistent@example.com");
        when(userDetailsService.loadUserByUsername("nonexistent@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        // Act & Assert
        try {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        } catch (UsernameNotFoundException e) {
            assertThat(e.getMessage()).contains("User not found");
        }

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should set correct HTTP status and content type for error response")
    void doFilterInternal_ErrorResponse_ShouldHaveCorrectStatusAndContentType() throws ServletException, IOException {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(securityUser);
        when(jwtService.isTokenValid(validToken, securityUser)).thenReturn(false);
        when(response.getWriter()).thenReturn(writer);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
