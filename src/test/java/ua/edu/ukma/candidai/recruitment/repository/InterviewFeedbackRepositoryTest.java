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
class InterviewFeedbackRepositoryTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InterviewFeedbackRepository feedbackRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Application testApplication;

    @BeforeEach
    void setUp() {
        testApplication = Application.builder()
                .id(UUID.randomUUID())
                .vacancyId(UUID.randomUUID())
                .candidateName("John Doe")
                .email("john.doe@example.com")
                .status(ApplicationStatus.INTERVIEW)
                .appliedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        entityManager.persistAndFlush(testApplication);
    }

    @Test
    @DisplayName("save - should persist feedback and assign it to application")
    void givenFeedback_save_shouldPersistSuccessfully() {
        UUID feedbackId = UUID.randomUUID();
        InterviewFeedback feedback = InterviewFeedback.builder()
                .id(feedbackId)
                .application(testApplication)
                .interviewerName("Alice Senior")
                .technicalScore(8)
                .decision(InterviewDecision.HIRE)
                .notes("Excellent knowledge of Spring Boot and Java concurrency.")
                .createdAt(Instant.now())
                .build();

        feedbackRepository.save(feedback);
        entityManager.flush();
        entityManager.clear();

        Optional<InterviewFeedback> found = feedbackRepository.findById(feedbackId);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(feedbackId);
        assertThat(found.get().getInterviewerName()).isEqualTo("Alice Senior");
        assertThat(found.get().getTechnicalScore()).isEqualTo(8);
        assertThat(found.get().getDecision()).isEqualTo(InterviewDecision.HIRE);
    }

    @Test
    @DisplayName("findByApplicationId - should find all feedbacks for given application")
    void givenFeedbacks_findByApplicationId_shouldReturnMatchingFeedbacks() {
        InterviewFeedback feedback1 = createFeedback("Interviewer 1", 7, InterviewDecision.HIRE);
        InterviewFeedback feedback2 = createFeedback("Interviewer 2", 9, InterviewDecision.HIRE);

        feedbackRepository.save(feedback1);
        feedbackRepository.save(feedback2);
        entityManager.flush();
        entityManager.clear();

        List<InterviewFeedback> results = feedbackRepository.findByApplicationId(testApplication.getId());

        assertThat(results).hasSize(2);
        assertThat(results).extracting(InterviewFeedback::getInterviewerName)
                .containsExactlyInAnyOrder("Interviewer 1", "Interviewer 2");
    }

    @Test
    @DisplayName("findByIdWithApplication - should fetch feedback with application eagerly via JOIN FETCH")
    void givenFeedback_findByIdWithApplication_shouldFetchApplication() {
        InterviewFeedback feedback = createFeedback("Interviewer Lead", 6, InterviewDecision.REJECT);
        feedbackRepository.save(feedback);
        entityManager.flush();
        entityManager.clear();

        Optional<InterviewFeedback> found = feedbackRepository.findByIdWithApplication(feedback.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getApplication()).isNotNull();
        assertThat(found.get().getApplication().getId()).isEqualTo(testApplication.getId());
        assertThat(found.get().getApplication().getCandidateName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("findAllByApplicationIdWithApplication - should fetch feedbacks with application via JOIN FETCH")
    void givenFeedbacks_findAllByApplicationIdWithApplication_shouldFetchAllWithApplication() {
        InterviewFeedback feedback = createFeedback("Tech Lead", 9, InterviewDecision.HIRE);
        feedbackRepository.save(feedback);
        entityManager.flush();
        entityManager.clear();

        List<InterviewFeedback> results = feedbackRepository
                .findAllByApplicationIdWithApplication(testApplication.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getApplication()).isNotNull();
        assertThat(results.get(0).getApplication().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("orphanRemoval - removing feedback from application collection should delete it from database")
    void givenApplicationWithFeedback_whenFeedbackRemoved_shouldDeleteFeedbackFromDatabase() {
        InterviewFeedback feedback = createFeedback("Interviewer A", 5, InterviewDecision.REJECT);
        testApplication.addFeedback(feedback);
        entityManager.persistAndFlush(testApplication);
        entityManager.clear();

        Application loadedApp = entityManager.find(Application.class, testApplication.getId());
        assertThat(loadedApp.getFeedbacks()).hasSize(1);

        InterviewFeedback feedbackToRemove = loadedApp.getFeedbacks().get(0);
        UUID removedFeedbackId = feedbackToRemove.getId();
        loadedApp.removeFeedback(feedbackToRemove);

        entityManager.persistAndFlush(loadedApp);
        entityManager.clear();

        Optional<InterviewFeedback> deleted = feedbackRepository.findById(removedFeedbackId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("cascade delete - deleting application should cascade delete all associated feedbacks")
    void givenApplicationWithFeedback_whenApplicationDeleted_shouldCascadeDeleteFeedbacks() {
        InterviewFeedback feedback = createFeedback("Interviewer B", 8, InterviewDecision.HIRE);
        testApplication.addFeedback(feedback);
        entityManager.persistAndFlush(testApplication);
        entityManager.clear();

        Application loadedApp = entityManager.find(Application.class, testApplication.getId());
        InterviewFeedback loadedFeedback = loadedApp.getFeedbacks().get(0);
        UUID feedbackId = loadedFeedback.getId();

        entityManager.remove(loadedApp);
        entityManager.flush();
        entityManager.clear();

        Optional<InterviewFeedback> deleted = feedbackRepository.findById(feedbackId);
        assertThat(deleted).isEmpty();
    }

    private InterviewFeedback createFeedback(String interviewerName, int score, InterviewDecision decision) {
        return InterviewFeedback.builder()
                .id(UUID.randomUUID())
                .application(testApplication)
                .interviewerName(interviewerName)
                .technicalScore(score)
                .decision(decision)
                .notes("Interview notes for " + interviewerName)
                .createdAt(Instant.now())
                .build();
    }
}
