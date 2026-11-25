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
@DisplayName("Integration tests for StaleApplicationsScheduler with ShedLock")
class ShedLockStaleApplicationsIntegrationTest {

    private static final String LOCK_NAME = "StaleApplicationsJob";
    private static final UUID COMPANY_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");

    @Autowired
    private StaleApplicationsScheduler scheduler;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LockProvider lockProvider;

    private UUID vacancyId;
    private UUID applicationId;

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
                .title("Mid Java Developer")
                .description("Spring Boot expert")
                .category(JobCategory.ENGINEERING)
                .employmentType(EmploymentType.FULL_TIME)
                .status(VacancyStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
        vacancyRepository.saveAndFlush(vacancy);

        applicationId = UUID.randomUUID();
        Application application = Application.builder()
                .id(applicationId)
                .vacancyId(vacancyId)
                .candidateName("Inactive Candidate")
                .email("inactive@example.com")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(now.minus(Duration.ofDays(45)))
                .updatedAt(now.minus(Duration.ofDays(45)))
                .build();
        applicationRepository.saveAndFlush(application);
    }

    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        vacancyRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", LOCK_NAME);
    }

    @Test
    @DisplayName("archiveStaleApplications - should acquire ShedLock and reject stale application")
    void archiveStaleApplications_ShouldAcquireLockAndArchiveStaleApplication() {
        scheduler.archiveStaleApplications();

        Application updatedApplication = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(updatedApplication.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(updatedApplication.getComment()).isEqualTo("Automatically closed due to inactivity");

        Integer lockCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shedlock WHERE name = ?",
                Integer.class,
                LOCK_NAME
        );
        assertThat(lockCount).isEqualTo(1);
    }

    @Test
    @DisplayName("archiveStaleApplications - should skip execution when ShedLock is held by another node")
    void archiveStaleApplications_WhenLockedByAnotherNode_ShouldSkipExecution() {
        Instant now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO shedlock (name, lock_until, locked_at, locked_by) VALUES (?, ?, ?, ?)",
                LOCK_NAME,
                Timestamp.from(now.plus(Duration.ofMinutes(10))),
                Timestamp.from(now),
                "replica-node-99"
        );

        scheduler.archiveStaleApplications();

        Application unchangedApplication = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(unchangedApplication.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(unchangedApplication.getComment()).isNull();

        String lockedBy = jdbcTemplate.queryForObject(
                "SELECT locked_by FROM shedlock WHERE name = ?",
                String.class,
                LOCK_NAME
        );
        assertThat(lockedBy).isEqualTo("replica-node-99");
    }
}
