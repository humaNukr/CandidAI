package ua.edu.ukma.candidai.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.model.User;
import ua.edu.ukma.candidai.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "candidai.security.seed-data.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommonGenerator commonGenerator;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SeedAccount> accounts = List.of(
                new SeedAccount("admin@candidai.com", "Admin User", "AdminPass123!", UserRole.ADMIN),
                new SeedAccount("recruiter@candidai.com", "Recruiter User", "RecruiterPass123!", UserRole.RECRUITER),
                new SeedAccount("techlead@candidai.com", "Tech Lead User", "TechLeadPass123!", UserRole.TECH_LEAD),
                new SeedAccount("candidate@candidai.com", "Candidate User", "CandidatePass123!", UserRole.CANDIDATE)
        );

        for (SeedAccount account : accounts) {
            if (!userRepository.existsByEmailIgnoreCase(account.email())) {
                User user = User.builder()
                        .id(commonGenerator.uuid())
                        .fullName(account.fullName())
                        .email(account.email().toLowerCase())
                        .passwordHash(passwordEncoder.encode(account.rawPassword()))
                        .role(account.role())
                        .createdAt(commonGenerator.now())
                        .build();

                userRepository.save(user);
                log.info("Provisioned default user: {} with role ROLE_{}", account.email(), account.role());
            }
        }
    }

    private record SeedAccount(String email, String fullName, String rawPassword, UserRole role) {
    }
}
