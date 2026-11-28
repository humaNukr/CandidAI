package ua.edu.ukma.candidai.recruitment.scheduler;

import java.time.Instant;
import java.util.UUID;
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
@DisplayName("Integration tests for CandidateFunnelMetricsScheduler")
class CandidateFunnelMetricsSchedulerIntegrationTest {

    private static final String JOB_NAME = "CandidateFunnelMetricsJob";
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

    private UUID vacancyId;

    @BeforeEach
    void setUpTestData() {
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
    }

    @Test
    @DisplayName("calculateFunnelMetrics - should compute snapshot from PostgreSQL without ShedLock")
    void calculateFunnelMetrics_ShouldComputeFunnelMetricsWithoutShedLock() {
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
                JOB_NAME
        );
        assertThat(lockCount).isZero();
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
