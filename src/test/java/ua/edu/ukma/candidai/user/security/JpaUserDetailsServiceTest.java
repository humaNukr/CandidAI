package ua.edu.ukma.candidai.user.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ua.edu.ukma.candidai.user.model.User;
import ua.edu.ukma.candidai.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.user.security.UserSecurityTestResources.*;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserDetailsService userDetailsService;

    @Test
    void givenExistingEmail_loadUserByUsername_shouldReturnMatchingUserDetails() {
        User expectedUser = testUser("{argon2}$argon2id$mockhash");
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(expectedUser));

        UserDetails actual = userDetailsService.loadUserByUsername(TEST_EMAIL);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedUser);
        assertThat(actual.getAuthorities()).extracting("authority").containsExactly("ROLE_RECRUITER");
    }

    @Test
    void givenNonExistingEmail_loadUserByUsername_shouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmailIgnoreCase(MISSING_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(MISSING_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(MISSING_EMAIL);
    }
}
