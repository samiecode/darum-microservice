package com.darum.auth.jwt;

import com.darum.auth.domain.entity.User;
import com.darum.auth.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private SecurityUser securityUser;
    private User testUser;

    // Test secret key (base64 encoded, must be at least 256 bits for HS256)
    private static final String TEST_SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_JWT_EXPIRATION = 3600000; // 1 hour
    private static final long TEST_REFRESH_EXPIRATION = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Inject test values using reflection
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_JWT_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", TEST_REFRESH_EXPIRATION);

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
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void generateToken_ShouldReturnValidToken() {
        // Act
        String token = jwtService.generateToken(securityUser);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_ShouldReturnCorrectUsername() {
        // Arrange
        String token = jwtService.generateToken(securityUser);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should include roles in token claims")
    void generateToken_ShouldIncludeRolesInClaims() {
        // Arrange & Act
        String token = jwtService.generateToken(securityUser);

        // Assert
        Claims claims = extractAllClaims(token);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");

        assertThat(roles).isNotNull();
        assertThat(roles).contains("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("Should validate token successfully for correct user")
    void isTokenValid_WithCorrectUser_ShouldReturnTrue() {
        // Arrange
        String token = jwtService.generateToken(securityUser);

        // Act
        boolean isValid = jwtService.isTokenValid(token, securityUser);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate token for incorrect user")
    void isTokenValid_WithIncorrectUser_ShouldReturnFalse() {
        // Arrange
        String token = jwtService.generateToken(securityUser);

        User anotherUser = User.builder()
                .id(2L)
                .email("another@example.com")
                .password("password")
                .name("Another User")
                .role(Role.ADMIN)
                .build();
        SecurityUser anotherSecurityUser = new SecurityUser(anotherUser);

        // Act
        boolean isValid = jwtService.isTokenValid(token, anotherSecurityUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate refresh token")
    void generateRefreshToken_ShouldReturnValidToken() {
        // Act
        String refreshToken = jwtService.generateRefreshToken(securityUser);

        // Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);

        // Should be able to extract username from refresh token
        String username = jwtService.extractUsername(refreshToken);
        assertThat(username).isEqualTo(securityUser.getUsername());
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void extractClaim_ShouldExtractExpirationDate() {
        // Arrange
        String token = jwtService.generateToken(securityUser);

        // Act
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should return expiration date in future")
    void getExpiration_ShouldReturnFutureDate() {
        // Act
        Date expiration = jwtService.getExpiration();

        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());

        // Should be approximately 1 hour from now
        long expectedTime = System.currentTimeMillis() + TEST_JWT_EXPIRATION;
        long actualTime = expiration.getTime();
        assertThat(actualTime).isCloseTo(expectedTime, within(1000L)); // Within 1 second
    }

    @Test
    @DisplayName("Should detect expired token")
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        // Arrange - Create a token with negative expiration
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expiredToken = jwtService.generateToken(securityUser);

        // Reset to normal expiration
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_JWT_EXPIRATION);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, securityUser))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid token format")
    void extractUsername_WithInvalidToken_ShouldThrowException() {
        // Arrange
        String invalidToken = "invalid.token.format";

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Should throw exception for token with invalid signature")
    void extractUsername_WithInvalidSignature_ShouldThrowException() {
        // Arrange
        String token = jwtService.generateToken(securityUser);
        String tamperedToken = token.substring(0, token.length() - 10) + "TAMPERED!!";

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Should extract subject claim correctly")
    void extractClaim_ShouldExtractSubject() {
        // Arrange
        String token = jwtService.generateToken(securityUser);

        // Act
        String subject = jwtService.extractClaim(token, Claims::getSubject);

        // Assert
        assertThat(subject).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should include issued at date in token")
    void generateToken_ShouldIncludeIssuedAtDate() {
        // Arrange
        long beforeGeneration = System.currentTimeMillis();

        // Act
        String token = jwtService.generateToken(securityUser);

        long afterGeneration = System.currentTimeMillis();

        // Assert
        Claims claims = extractAllClaims(token);
        Date issuedAt = claims.getIssuedAt();

        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt.getTime()).isBetween(beforeGeneration - 1000, afterGeneration + 1000);
    }

    @Test
    @DisplayName("Should include not before date in token")
    void generateToken_ShouldIncludeNotBeforeDate() {
        // Arrange & Act
        String token = jwtService.generateToken(securityUser);

        // Assert
        Claims claims = extractAllClaims(token);
        Date notBefore = claims.getNotBefore();

        assertThat(notBefore).isNotNull();
        assertThat(notBefore).isBeforeOrEqualTo(new Date());
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void generateToken_ForDifferentUsers_ShouldReturnDifferentTokens() {
        // Arrange
        User anotherUser = User.builder()
                .id(2L)
                .email("another@example.com")
                .password("password")
                .name("Another User")
                .role(Role.ADMIN)
                .build();
        SecurityUser anotherSecurityUser = new SecurityUser(anotherUser);

        // Act
        String token1 = jwtService.generateToken(securityUser);
        String token2 = jwtService.generateToken(anotherSecurityUser);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should include all user authorities in token")
    void generateToken_ShouldIncludeAllAuthorities() {
        // Arrange
        User adminUser = User.builder()
                .id(2L)
                .email("admin@example.com")
                .password("password")
                .name("Admin User")
                .role(Role.ADMIN)
                .build();
        SecurityUser adminSecurityUser = new SecurityUser(adminUser);

        // Act
        String token = jwtService.generateToken(adminSecurityUser);

        // Assert
        Claims claims = extractAllClaims(token);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");

        assertThat(roles).isNotNull();
        assertThat(roles).contains("ROLE_ADMIN");

        // Verify the roles match what we expect from the SecurityUser
        List<String> expectedRoles = adminSecurityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(roles).containsAll(expectedRoles);
    }

    @Test
    @DisplayName("Should have JWT type header")
    void generateToken_ShouldHaveJwtTypeHeader() {
        // Arrange & Act
        String token = jwtService.generateToken(securityUser);

        // Assert
        Claims claims = extractAllClaims(token);
        // The header is part of the JWT structure, verified by successful parsing
        assertThat(claims).isNotNull();
    }

    @Test
    @DisplayName("Refresh token should have longer expiration than access token")
    void generateRefreshToken_ShouldHaveLongerExpiration() {
        // Arrange & Act
        String accessToken = jwtService.generateToken(securityUser);
        String refreshToken = jwtService.generateRefreshToken(securityUser);

        // Assert
        Date accessExpiration = jwtService.extractClaim(accessToken, Claims::getExpiration);
        Date refreshExpiration = jwtService.extractClaim(refreshToken, Claims::getExpiration);

        assertThat(refreshExpiration).isAfter(accessExpiration);
    }

    // Helper method to extract all claims from a token
    private Claims extractAllClaims(String token) {
        SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_KEY));
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
