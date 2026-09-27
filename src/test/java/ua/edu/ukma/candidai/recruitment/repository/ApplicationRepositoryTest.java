package ua.edu.ukma.candidai.recruitment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.model.InterviewFeedback;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ApplicationRepositoryTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID vacancyId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        vacancyId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
    }

    @Test
    @DisplayName("save and findById - should save application and find it by id")
    void givenApplication_saveAndFindById_shouldPersistAndRetrieve() {
        Application application = createApplication("test@example.com");
        Application saved = applicationRepository.save(application);
        entityManager.flush();
        entityManager.clear();

        Optional<Application> found = applicationRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return true when application exists")
    void givenExistingApplication_existsByVacancyIdAndEmail_shouldReturnTrue() {
        Application application = createApplication("candidate@example.com");
        applicationRepository.save(application);
        entityManager.flush();

        boolean exists = applicationRepository.existsByVacancyIdAndEmail(vacancyId, "candidate@example.com");
        assertThat(exists).isTrue();

        boolean notExists = applicationRepository.existsByVacancyIdAndEmail(vacancyId, "other@example.com");
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("findByVacancyId - should return applications for vacancy")
    void givenApplications_findByVacancyId_shouldReturnMatchingList() {
        Application app1 = createApplication("app1@example.com");
        Application app2 = createApplication("app2@example.com");
        applicationRepository.save(app1);
        applicationRepository.save(app2);
        entityManager.flush();

        List<Application> results = applicationRepository.findByVacancyId(vacancyId);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("findByIdWithFeedbacks - should fetch application with feedbacks eagerly via LEFT JOIN FETCH")
    void givenApplicationWithFeedback_findByIdWithFeedbacks_shouldLoadFeedbacks() {
        Application app = createApplication("candidate.feedback@example.com");
        InterviewFeedback feedback = InterviewFeedback.builder()
                .id(UUID.randomUUID())
                .application(app)
                .interviewerName("Tech Lead")
                .technicalScore(9)
                .decision(InterviewDecision.HIRE)
                .notes("Strong architecture and JPA knowledge")
                .createdAt(Instant.now())
                .build();
        app.addFeedback(feedback);

        applicationRepository.save(app);
        entityManager.flush();
        entityManager.clear();

        Optional<Application> found = applicationRepository.findByIdWithFeedbacks(app.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFeedbacks()).hasSize(1);
        assertThat(found.get().getFeedbacks().getFirst().getInterviewerName()).isEqualTo("Tech Lead");
    }

    private Application createApplication(String email) {
        return Application.builder()
                .id(UUID.randomUUID())
                .vacancyId(vacancyId)
                .candidateId(candidateId)
                .candidateName("Jane Doe")
                .email(email)
                .status(ApplicationStatus.APPLIED)
                .appliedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
