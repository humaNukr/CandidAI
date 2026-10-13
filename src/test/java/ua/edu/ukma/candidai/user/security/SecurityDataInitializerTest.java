package ua.edu.ukma.candidai.user.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.model.User;
import ua.edu.ukma.candidai.user.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CommonGenerator commonGenerator;

    @InjectMocks
    private SecurityDataInitializer securityDataInitializer;

    @Test
    void givenEmptyDatabase_run_shouldProvisionAllDefaultAccountsWithEncodedPasswords() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("{argon2}$argon2id$mockedhash");
        when(commonGenerator.uuid()).thenReturn(UUID.randomUUID());
        when(commonGenerator.now()).thenReturn(Instant.parse("2026-10-12T12:00:00Z"));

        securityDataInitializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(userCaptor.capture());

        assertThat(userCaptor.getAllValues())
                .extracting("email", "role", "passwordHash")
                .containsExactlyInAnyOrder(
                        tuple("admin@candidai.com", UserRole.ADMIN, "{argon2}$argon2id$mockedhash"),
                        tuple("recruiter@candidai.com", UserRole.RECRUITER, "{argon2}$argon2id$mockedhash"),
                        tuple("techlead@candidai.com", UserRole.TECH_LEAD, "{argon2}$argon2id$mockedhash"),
                        tuple("candidate@candidai.com", UserRole.CANDIDATE, "{argon2}$argon2id$mockedhash")
                );
    }

    @Test
    void givenExistingAccounts_run_shouldSkipProvisioning() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        securityDataInitializer.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
    }

    @Test
    void givenPartiallyExistingAccounts_run_shouldProvisionOnlyMissingAccounts() {
        when(userRepository.existsByEmailIgnoreCase("admin@candidai.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("recruiter@candidai.com")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("techlead@candidai.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("candidate@candidai.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("{argon2}$argon2id$mockedhash");
        when(commonGenerator.uuid()).thenReturn(UUID.randomUUID());
        when(commonGenerator.now()).thenReturn(Instant.parse("2026-10-12T12:00:00Z"));

        securityDataInitializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());

        assertThat(userCaptor.getAllValues())
                .extracting("email", "role")
                .containsExactlyInAnyOrder(
                        tuple("recruiter@candidai.com", UserRole.RECRUITER),
                        tuple("candidate@candidai.com", UserRole.CANDIDATE)
                );
    }
}
