package ua.edu.ukma.candidai.user.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import ua.edu.ukma.candidai.user.UserRole;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.user.UserTestResources.*;

class UserTest {

    @Test
    @DisplayName("getAuthorities should return role with ROLE_ prefix")
    void givenUser_getAuthorities_shouldReturnRolePrefixedAuthority() {
        User user = sampleUserBuilder().role(UserRole.ADMIN).build();

        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("getPassword should return passwordHash")
    void givenUser_getPassword_shouldReturnPasswordHash() {
        User user = sampleUser();

        assertThat(user.getPassword()).isEqualTo(DEFAULT_PASSWORD_HASH);
    }

    @Test
    @DisplayName("getUsername should return email")
    void givenUser_getUsername_shouldReturnEmail() {
        User user = sampleUser();

        assertThat(user.getUsername()).isEqualTo(DEFAULT_EMAIL);
    }

    @Test
    @DisplayName("isAccountNonExpired should return true")
    void givenUser_isAccountNonExpired_shouldReturnTrue() {
        User user = sampleUser();

        assertThat(user.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isAccountNonLocked should return true")
    void givenUser_isAccountNonLocked_shouldReturnTrue() {
        User user = sampleUser();

        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("isCredentialsNonExpired should return true")
    void givenUser_isCredentialsNonExpired_shouldReturnTrue() {
        User user = sampleUser();

        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isEnabled should return true")
    void givenUser_isEnabled_shouldReturnTrue() {
        User user = sampleUser();

        assertThat(user.isEnabled()).isTrue();
    }
}
