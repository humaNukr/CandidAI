package ua.edu.ukma.candidai.interview.scheduler;

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
import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.model.InterviewType;
import ua.edu.ukma.candidai.interview.repository.InterviewRepository;
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
@DisplayName("Integration tests for InterviewReminderScheduler with ShedLock")
class ShedLockInterviewReminderIntegrationTest {

    private static final String LOCK_NAME = "InterviewReminderJob";
    private static final UUID COMPANY_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");

    @Autowired
    private InterviewReminderScheduler scheduler;

    @Autowired
    private InterviewRepository interviewRepository;

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
    private UUID interviewId;

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
                .description("Java 25 and Spring Boot 4")
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
                .candidateName("John Doe")
                .email("john.doe@example.com")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(now)
                .updatedAt(now)
                .build();
        applicationRepository.saveAndFlush(application);

        interviewId = UUID.randomUUID();
        Interview interview = Interview.builder()
                .id(interviewId)
                .applicationId(applicationId)
                .interviewerName("Alice Recruiter")
                .type(InterviewType.TECHNICAL)
                .status(InterviewStatus.SCHEDULED)
                .scheduledAt(now.plus(Duration.ofHours(4)))
                .durationMinutes(60)
                .meetingLink("https://meet.candidai.com/interview-1")
                .createdAt(now)
                .updatedAt(now)
                .build();
        interviewRepository.saveAndFlush(interview);
    }

    @AfterEach
    void cleanUp() {
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        vacancyRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", LOCK_NAME);
    }

    @Test
    @DisplayName("sendUpcomingInterviewReminders - should acquire ShedLock and mark pending interview as sent")
    void sendUpcomingInterviewReminders_ShouldAcquireLockAndMarkReminderSent() {
        scheduler.sendUpcomingInterviewReminders();

        Interview updatedInterview = interviewRepository.findById(interviewId).orElseThrow();
        assertThat(updatedInterview.isReminderSent()).isTrue();
        assertThat(updatedInterview.getReminderSentAt()).isNotNull();

        Integer lockCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shedlock WHERE name = ?",
                Integer.class,
                LOCK_NAME
        );
        assertThat(lockCount).isEqualTo(1);
    }

    @Test
    @DisplayName("sendUpcomingInterviewReminders - should skip execution when ShedLock is held by another node")
    void sendUpcomingInterviewReminders_WhenLockedByAnotherNode_ShouldSkipExecution() {
        Instant now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO shedlock (name, lock_until, locked_at, locked_by) VALUES (?, ?, ?, ?)",
                LOCK_NAME,
                Timestamp.from(now.plus(Duration.ofMinutes(10))),
                Timestamp.from(now),
                "replica-node-99"
        );

        scheduler.sendUpcomingInterviewReminders();

        Interview unchangedInterview = interviewRepository.findById(interviewId).orElseThrow();
        assertThat(unchangedInterview.isReminderSent()).isFalse();
        assertThat(unchangedInterview.getReminderSentAt()).isNull();

        String lockedBy = jdbcTemplate.queryForObject(
                "SELECT locked_by FROM shedlock WHERE name = ?",
                String.class,
                LOCK_NAME
        );
        assertThat(lockedBy).isEqualTo("replica-node-99");
    }
}
