package ua.edu.ukma.candidai.recruitment.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for StaleApplicationsScheduler")
class StaleApplicationsSchedulerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CommonGenerator commonGenerator;

    @InjectMocks
    private StaleApplicationsScheduler scheduler;

    private final Instant now = Instant.parse("2026-11-21T10:00:00Z");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "inactivityDays", 30);
        when(commonGenerator.now()).thenReturn(now);
    }

    @Test
    @DisplayName("archiveStaleApplications - should reject and save when stale applications found")
    void archiveStaleApplications_WhenStaleApplicationsExist_ShouldRejectAndSave() {
        Instant oldTimestamp = now.minus(Duration.ofDays(45));
        Application application = Application.builder()
                .id(UUID.randomUUID())
                .vacancyId(UUID.randomUUID())
                .candidateName("Stale Candidate")
                .email("stale@example.com")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(oldTimestamp)
                .updatedAt(oldTimestamp)
                .build();

        Instant expectedCutoff = now.minus(Duration.ofDays(30));
        when(applicationRepository.findByStatusInAndUpdatedAtBefore(any(), eq(expectedCutoff)))
                .thenReturn(List.of(application));

        scheduler.archiveStaleApplications();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getComment()).isEqualTo("Automatically closed due to inactivity");
        assertThat(application.getUpdatedAt()).isEqualTo(now);

        verify(applicationRepository).saveAll(List.of(application));
    }

    @Test
    @DisplayName("archiveStaleApplications - should do nothing when no stale applications found")
    void archiveStaleApplications_WhenNoStaleApplications_ShouldNotSave() {
        Instant expectedCutoff = now.minus(Duration.ofDays(30));
        when(applicationRepository.findByStatusInAndUpdatedAtBefore(any(), eq(expectedCutoff)))
                .thenReturn(List.of());

        scheduler.archiveStaleApplications();

        verify(applicationRepository, never()).saveAll(any());
    }
}
