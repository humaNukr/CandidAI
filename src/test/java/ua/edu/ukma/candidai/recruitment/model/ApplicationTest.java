package ua.edu.ukma.candidai.recruitment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTest {

    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-24T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-09-24T14:00:00Z");

    @Test
    @DisplayName("create - should initialize Application with APPLIED status and timestamps")
    void givenValidRequest_create_shouldInitializeApplication() {
        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                VACANCY_ID,
                "Jane Doe",
                "jane@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/jane.pdf"
        );

        Application application = Application.create(request, APPLICATION_ID, NOW);

        assertThat(application.getId()).isEqualTo(APPLICATION_ID);
        assertThat(application.getVacancyId()).isEqualTo(VACANCY_ID);
        assertThat(application.getCandidateName()).isEqualTo("Jane Doe");
        assertThat(application.getEmail()).isEqualTo("jane@example.com");
        assertThat(application.getPhone()).isEqualTo("+380501234567");
        assertThat(application.getResumeUrl()).isEqualTo("https://storage.candidai.ukma.edu.ua/resumes/jane.pdf");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(application.getComment()).isNull();
        assertThat(application.getAppliedAt()).isEqualTo(NOW);
        assertThat(application.getUpdatedAt()).isEqualTo(NOW);
        assertThat(application.isInInterview()).isFalse();
    }

    @Test
    @DisplayName("updateStatus - should transition status and update comment and updatedAt on valid transition")
    void givenValidTransition_updateStatus_shouldUpdateFields() {
        Application application = Application.builder()
                .id(APPLICATION_ID)
                .vacancyId(VACANCY_ID)
                .candidateName("Jane Doe")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(NOW)
                .updatedAt(NOW)
                .build();

        application.updateStatus(ApplicationStatus.SCREENING, "Passed resume screening", LATER);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(application.getComment()).isEqualTo("Passed resume screening");
        assertThat(application.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("updateStatus - should update matchingScore when provided")
    void givenMatchingScore_updateStatus_shouldUpdateMatchingScore() {
        Application application = Application.builder()
                .id(APPLICATION_ID)
                .vacancyId(VACANCY_ID)
                .candidateName("Jane Doe")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(NOW)
                .updatedAt(NOW)
                .build();

        application.updateStatus(ApplicationStatus.SCREENING, 85, "Passed screening", LATER);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(application.getMatchingScore()).isEqualTo(85);
        assertThat(application.getComment()).isEqualTo("Passed screening");
        assertThat(application.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("updateStatus - should retain existing matchingScore when new score is null")
    void givenNullMatchingScore_updateStatus_shouldRetainExistingMatchingScore() {
        Application application = Application.builder()
                .id(APPLICATION_ID)
                .vacancyId(VACANCY_ID)
                .candidateName("Jane Doe")
                .status(ApplicationStatus.APPLIED)
                .matchingScore(90)
                .appliedAt(NOW)
                .updatedAt(NOW)
                .build();

        application.updateStatus(ApplicationStatus.SCREENING, null, "Proceeded to next step", LATER);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(application.getMatchingScore()).isEqualTo(90);
        assertThat(application.getComment()).isEqualTo("Proceeded to next step");
        assertThat(application.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("updateStatus - should retain existing comment when new comment is null")
    void givenNullComment_updateStatus_shouldRetainExistingComment() {
        Application application = Application.builder()
                .id(APPLICATION_ID)
                .vacancyId(VACANCY_ID)
                .candidateName("Jane Doe")
                .status(ApplicationStatus.APPLIED)
                .comment("Initial note")
                .appliedAt(NOW)
                .updatedAt(NOW)
                .build();

        application.updateStatus(ApplicationStatus.SCREENING, null, LATER);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(application.getComment()).isEqualTo("Initial note");
        assertThat(application.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("updateStatus - should throw InvalidStateTransitionException on invalid transition")
    void givenInvalidTransition_updateStatus_shouldThrowInvalidStateTransitionException() {
        Application application = Application.builder()
                .id(APPLICATION_ID)
                .vacancyId(VACANCY_ID)
                .candidateName("Jane Doe")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(NOW)
                .updatedAt(NOW)
                .build();

        assertThatThrownBy(() -> application.updateStatus(ApplicationStatus.OFFER, "Direct offer", LATER))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid status transition from APPLIED to OFFER");
    }

    @Test
    @DisplayName("isInInterview - should return true only when status is INTERVIEW")
    void givenStatuses_isInInterview_shouldReturnExpectedBoolean() {
        Application appliedApp = Application.builder().status(ApplicationStatus.APPLIED).build();
        Application interviewApp = Application.builder().status(ApplicationStatus.INTERVIEW).build();

        assertThat(appliedApp.isInInterview()).isFalse();
        assertThat(interviewApp.isInInterview()).isTrue();
    }
}
