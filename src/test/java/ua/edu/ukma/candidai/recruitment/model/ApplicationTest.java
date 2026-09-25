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
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.DEFAULT_APPLICATION_ID;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.DEFAULT_NOW;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.anApplication;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.anApplicationBuilder;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.validApplyForVacancyRequest;

class ApplicationTest {

    private static final Instant UPDATED_NOW = DEFAULT_NOW.plusSeconds(3600);

    @Test
    @DisplayName("create - should initialize Application with APPLIED status, candidateId, and timestamps")
    void givenValidRequest_create_shouldInitializeApplication() {
        ApplyForVacancyRequest request = validApplyForVacancyRequest();
        Application expected = anApplicationBuilder()
                .appliedAt(DEFAULT_NOW)
                .updatedAt(DEFAULT_NOW)
                .build();

        Application actual = Application.create(request, DEFAULT_APPLICATION_ID, DEFAULT_NOW);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("updateStatus - should transition status and update comment and updatedAt on valid transition")
    void givenValidTransition_updateStatus_shouldUpdateFields() {
        Application application = anApplication(ApplicationStatus.APPLIED);
        Application expected = anApplicationBuilder()
                .status(ApplicationStatus.SCREENING)
                .comment("Passed resume screening")
                .updatedAt(UPDATED_NOW)
                .build();

        application.updateStatus(ApplicationStatus.SCREENING, "Passed resume screening", UPDATED_NOW);

        assertThat(application)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("updateStatus - should set matchingScore when using overloaded method")
    void givenMatchingScore_updateStatus_shouldUpdateStatusAndMatchingScore() {
        Application application = anApplication(ApplicationStatus.APPLIED);
        Application expected = anApplicationBuilder()
                .status(ApplicationStatus.SCREENING)
                .matchingScore(85)
                .comment("Passed screening")
                .updatedAt(UPDATED_NOW)
                .build();

        application.updateStatus(ApplicationStatus.SCREENING, 85, "Passed screening", UPDATED_NOW);

        assertThat(application)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("updateStatus - should throw InvalidStateTransitionException on invalid transition")
    void givenInvalidTransition_updateStatus_shouldThrowInvalidStateTransitionException() {
        Application application = anApplication(ApplicationStatus.APPLIED);

        assertThatThrownBy(() -> application.updateStatus(ApplicationStatus.OFFER, "Direct offer", UPDATED_NOW))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid status transition from APPLIED to OFFER");
    }

    @Test
    @DisplayName("isInInterview - should return true when status is INTERVIEW")
    void givenInterviewStatus_isInInterview_shouldReturnTrue() {
        Application application = anApplication(ApplicationStatus.INTERVIEW);

        boolean actual = application.isInInterview();

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("isInInterview - should return false when status is not INTERVIEW")
    void givenNonInterviewStatus_isInInterview_shouldReturnFalse() {
        Application application = anApplication(ApplicationStatus.APPLIED);

        boolean actual = application.isInInterview();

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("equals and hashCode - should be equal when id is identical")
    void givenSameId_equalsAndHashCode_shouldBeEqual() {
        Application app1 = anApplication();
        Application app2 = anApplicationBuilder()
                .status(ApplicationStatus.OFFER)
                .comment("Different comment")
                .build();

        assertThat(app1)
                .isEqualTo(app2)
                .hasSameHashCodeAs(app2);
    }

    @Test
    @DisplayName("equals - should not be equal when id is different")
    void givenDifferentId_equals_shouldNotBeEqual() {
        Application app1 = anApplication();
        Application app2 = anApplicationBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .build();

        assertThat(app1).isNotEqualTo(app2);
    }
}
