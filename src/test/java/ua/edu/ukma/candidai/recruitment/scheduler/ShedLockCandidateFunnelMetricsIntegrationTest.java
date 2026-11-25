package ua.edu.ukma.candidai.recruitment.scheduler;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.support.StorageBasedLockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.repository.VacancyRepository;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DisplayName("Integration tests for CandidateFunnelMetricsScheduler with ShedLock")
class ShedLockCandidateFunnelMetricsIntegrationTest {

    private static final String LOCK_NAME = "CandidateFunnelMetricsJob";
    private static final UUID COMPANY_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");
    private static final int SCORE_APPLIED = 70;
    private static final int SCORE_SCREENING = 80;
    private static final int SCORE_HIRED = 95;

    @Autowired
    private CandidateFunnelMetricsScheduler scheduler;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LockProvider lockProvider;

    private UUID vacancyId;

    @BeforeEach
    void setUpTestData() {
        if (lockProvider instanceof StorageBasedLockProvider storageBasedLockProvider) {
            storageBasedLockProvider.clearCache();
        }
        jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", LOCK_NAME);

        Instant now = Instant.now();
        vacancyId = UUID.randomUUID();
        Vacancy vacancy = Vacancy.builder()
                .id(vacancyId)
                .companyId(COMPANY_ID)
                .title("Senior Backend Engineer")
                .description("Spring Boot and PostgreSQL")
                .category(JobCategory.ENGINEERING)
                .employmentType(EmploymentType.FULL_TIME)
                .status(VacancyStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
        vacancyRepository.saveAndFlush(vacancy);

        saveApplication("candidate1@example.com", ApplicationStatus.APPLIED, SCORE_APPLIED, now);
        saveApplication("candidate2@example.com", ApplicationStatus.SCREENING, SCORE_SCREENING, now);
        saveApplication("candidate3@example.com", ApplicationStatus.HIRED, SCORE_HIRED, now);
        saveApplication("candidate4@example.com", ApplicationStatus.REJECTED, null, now);
    }

    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        vacancyRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", LOCK_NAME);
    }

    @Test
    @DisplayName("calculateFunnelMetrics - should acquire ShedLock and compute snapshot from database")
    void calculateFunnelMetrics_ShouldAcquireLockAndComputeFunnelMetrics() {
        scheduler.calculateFunnelMetrics();

        CandidateFunnelMetricsSnapshot snapshot = scheduler.getLatestSnapshot().orElseThrow();
        assertThat(snapshot.totalApplications()).isEqualTo(4L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.APPLIED)).isEqualTo(1L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.SCREENING)).isEqualTo(1L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.HIRED)).isEqualTo(1L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.REJECTED)).isEqualTo(1L);
        assertThat(snapshot.hireRatePercent()).isEqualTo(25.0);
        assertThat(snapshot.averageMatchingScore()).isEqualTo(81.67);

        Integer lockCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shedlock WHERE name = ?",
                Integer.class,
                LOCK_NAME
        );
        assertThat(lockCount).isEqualTo(1);
    }

    @Test
    @DisplayName("calculateFunnelMetrics - should skip execution when ShedLock is held by another node")
    void calculateFunnelMetrics_WhenLockedByAnotherNode_ShouldSkipExecution() {
        scheduler.calculateFunnelMetrics();
        CandidateFunnelMetricsSnapshot initialSnapshot = scheduler.getLatestSnapshot().orElseThrow();

        if (lockProvider instanceof StorageBasedLockProvider storageBasedLockProvider) {
            storageBasedLockProvider.clearCache();
        }
        Instant now = Instant.now();
        jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", LOCK_NAME);
        jdbcTemplate.update(
                "INSERT INTO shedlock (name, lock_until, locked_at, locked_by) VALUES (?, ?, ?, ?)",
                LOCK_NAME,
                Timestamp.from(now.plus(Duration.ofMinutes(10))),
                Timestamp.from(now),
                "instance-2"
        );

        saveApplication("candidate5@example.com", ApplicationStatus.HIRED, SCORE_HIRED, now);

        scheduler.calculateFunnelMetrics();

        CandidateFunnelMetricsSnapshot snapshotAfterLockedCall = scheduler.getLatestSnapshot().orElseThrow();
        assertThat(snapshotAfterLockedCall).isEqualTo(initialSnapshot);

        String lockedBy = jdbcTemplate.queryForObject(
                "SELECT locked_by FROM shedlock WHERE name = ?",
                String.class,
                LOCK_NAME
        );
        assertThat(lockedBy).isEqualTo("instance-2");
    }

    private void saveApplication(String email, ApplicationStatus status, Integer matchingScore, Instant now) {
        Application application = Application.builder()
                .id(UUID.randomUUID())
                .vacancyId(vacancyId)
                .candidateName("Test Candidate")
                .email(email)
                .status(status)
                .matchingScore(matchingScore)
                .appliedAt(now)
                .updatedAt(now)
                .build();
        applicationRepository.saveAndFlush(application);
    }
}
