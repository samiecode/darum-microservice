package com.darum.auth.jwt;

import com.darum.auth.domain.entity.User;
import com.darum.auth.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityUser Tests")
class SecurityUserTest {

    private User testUser;
    private SecurityUser securityUser;

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
    }

    @Test
    @DisplayName("Should return correct username (email)")
    void getUsername_ShouldReturnEmail() {
        // Act
        String username = securityUser.getUsername();

        // Assert
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return correct password")
    void getPassword_ShouldReturnUserPassword() {
        // Act
        String password = securityUser.getPassword();

        // Assert
        assertThat(password).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("Should return authorities from user role")
    void getAuthorities_ShouldReturnRoleAuthorities() {
        // Act
        Collection<? extends GrantedAuthority> authorities = securityUser.getAuthorities();

        // Assert
        assertThat(authorities).isNotNull();
        assertThat(authorities).isNotEmpty();
        assertThat(authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList()).contains("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("Should return wrapped user entity")
    void getUser_ShouldReturnWrappedUser() {
        // Act
        User user = securityUser.getUser();

        // Assert
        assertThat(user).isNotNull();
        assertThat(user).isEqualTo(testUser);
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should wrap admin user with admin authorities")
    void constructor_WithAdminUser_ShouldHaveAdminAuthorities() {
        // Arrange
        User adminUser = User.builder()
                .id(2L)
                .email("admin@example.com")
                .password("adminPassword")
                .name("Admin User")
                .role(Role.ADMIN)
                .build();

        // Act
        SecurityUser adminSecurityUser = new SecurityUser(adminUser);

        // Assert
        assertThat(adminSecurityUser.getAuthorities()).isNotNull();
        assertThat(adminSecurityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList()).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should wrap manager user with manager authorities")
    void constructor_WithManagerUser_ShouldHaveManagerAuthorities() {
        // Arrange
        User managerUser = User.builder()
                .id(3L)
                .email("manager@example.com")
                .password("managerPassword")
                .name("Manager User")
                .role(Role.MANAGER)
                .build();

        // Act
        SecurityUser managerSecurityUser = new SecurityUser(managerUser);

        // Assert
        assertThat(managerSecurityUser.getAuthorities()).isNotNull();
        assertThat(managerSecurityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList()).contains("ROLE_MANAGER");
    }

    @Test
    @DisplayName("Should be account non expired by default")
    void isAccountNonExpired_ShouldReturnTrue() {
        // Act & Assert
        assertThat(securityUser.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should be account non locked by default")
    void isAccountNonLocked_ShouldReturnTrue() {
        // Act & Assert
        assertThat(securityUser.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("Should be credentials non expired by default")
    void isCredentialsNonExpired_ShouldReturnTrue() {
        // Act & Assert
        assertThat(securityUser.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should be enabled by default")
    void isEnabled_ShouldReturnTrue() {
        // Act & Assert
        assertThat(securityUser.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should set user correctly")
    void setUser_ShouldUpdateUser() {
        // Arrange
        User newUser = User.builder()
                .id(99L)
                .email("new@example.com")
                .password("newPassword")
                .name("New User")
                .role(Role.ADMIN)
                .build();

        // Act
        securityUser.setUser(newUser);

        // Assert
        assertThat(securityUser.getUser()).isEqualTo(newUser);
        assertThat(securityUser.getUsername()).isEqualTo("new@example.com");
        assertThat(securityUser.getPassword()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("Should have same authorities count as role authorities")
    void getAuthorities_ShouldMatchRoleAuthoritiesCount() {
        // Act
        Collection<? extends GrantedAuthority> authorities = securityUser.getAuthorities();
        Collection<? extends GrantedAuthority> roleAuthorities = testUser.getRole().getAuthorities();

        // Assert
        assertThat(authorities).hasSameSizeAs(roleAuthorities);
    }

    @Test
    @DisplayName("Should implement UserDetails interface properly")
    void securityUser_ShouldBeUserDetails() {
        // Assert
        assertThat(securityUser).isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);
    }
}
