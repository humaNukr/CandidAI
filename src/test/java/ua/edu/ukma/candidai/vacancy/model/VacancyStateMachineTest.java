package ua.edu.ukma.candidai.vacancy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

class VacancyStateMachineTest {

    @ParameterizedTest(name = "from {0} to {1} should be valid")
    @CsvSource({
            "DRAFT, OPEN",
            "DRAFT, ARCHIVED",
            "OPEN, PAUSED",
            "OPEN, CLOSED",
            "OPEN, ARCHIVED",
            "PAUSED, OPEN",
            "PAUSED, CLOSED",
            "PAUSED, ARCHIVED",
            "CLOSED, ARCHIVED",
            "OPEN, OPEN"
    })
    void givenAllowedTransition_canTransitionTo_shouldReturnTrue(VacancyStatus from, VacancyStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "from {0} to {1} should be invalid")
    @CsvSource({
            "DRAFT, PAUSED",
            "DRAFT, CLOSED",
            "CLOSED, OPEN",
            "CLOSED, PAUSED",
            "ARCHIVED, OPEN",
            "ARCHIVED, DRAFT",
            "ARCHIVED, CLOSED"
    })
    void givenForbiddenTransition_canTransitionTo_shouldReturnFalse(VacancyStatus from, VacancyStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    @DisplayName("updateStatus with invalid transition should throw InvalidStateTransitionException")
    void givenForbiddenTransition_updateStatus_shouldThrowInvalidStateTransitionException() {
        Vacancy vacancy = aVacancy(VacancyStatus.CLOSED);

        assertThatThrownBy(() -> vacancy.updateStatus(VacancyStatus.OPEN, DEFAULT_NOW))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition vacancy status from CLOSED to OPEN");
    }

    @Test
    @DisplayName("updateStatus with valid transition should update status and timestamp")
    void givenAllowedTransition_updateStatus_shouldUpdateStatusAndTimestamp() {
        Vacancy vacancy = aVacancy();

        vacancy.updateStatus(VacancyStatus.PAUSED, DEFAULT_NOW);

        Vacancy expected = aVacancy(VacancyStatus.PAUSED, DEFAULT_NOW);
        assertThat(vacancy)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("updateStatus from DRAFT to OPEN should set publishedAt and updatedAt")
    void givenDraftVacancy_updateStatusToOpen_shouldSetPublishedAtAndUpdatedAt() {
        Vacancy vacancy = aDraftVacancy();
        Instant publishTime = DEFAULT_NOW.plusSeconds(3600);

        vacancy.updateStatus(VacancyStatus.OPEN, publishTime);

        assertThat(vacancy.getStatus()).isEqualTo(VacancyStatus.OPEN);
        assertThat(vacancy.getPublishedAt()).isEqualTo(publishTime);
        assertThat(vacancy.getUpdatedAt()).isEqualTo(publishTime);
    }

    @Test
    @DisplayName("create with DRAFT status should set publishedAt to null")
    void givenDraftStatus_create_shouldSetPublishedAtToNull() {
        CreateVacancyRequest request = validCreateDraftVacancyRequest();

        Vacancy vacancy = Vacancy.create(request, DEFAULT_ID, DEFAULT_NOW);

        assertThat(vacancy.getStatus()).isEqualTo(VacancyStatus.DRAFT);
        assertThat(vacancy.getPublishedAt()).isNull();
        assertThat(vacancy.getCompanyId()).isEqualTo(DEFAULT_COMPANY_ID);
    }

    @Test
    @DisplayName("create with OPEN status should set publishedAt to now")
    void givenOpenStatus_create_shouldSetPublishedAtToNow() {
        CreateVacancyRequest request = validCreateVacancyRequest();

        Vacancy vacancy = Vacancy.create(request, DEFAULT_ID, DEFAULT_NOW);

        assertThat(vacancy.getStatus()).isEqualTo(VacancyStatus.OPEN);
        assertThat(vacancy.getPublishedAt()).isEqualTo(DEFAULT_NOW);
        assertThat(vacancy.getCompanyId()).isEqualTo(DEFAULT_COMPANY_ID);
    }

    @Test
    @DisplayName("create with null status should default to OPEN and set publishedAt to now")
    void givenNullStatus_create_shouldDefaultToOpen() {
        CreateVacancyRequest request = aCreateVacancyRequest().status(null).build();

        Vacancy vacancy = Vacancy.create(request, DEFAULT_ID, DEFAULT_NOW);

        assertThat(vacancy.getStatus()).isEqualTo(VacancyStatus.OPEN);
        assertThat(vacancy.getPublishedAt()).isEqualTo(DEFAULT_NOW);
    }

    @Test
    @DisplayName("create with invalid status should throw InvalidStateTransitionException")
    void givenInvalidStatus_create_shouldThrowInvalidStateTransitionException() {
        CreateVacancyRequest request = aCreateVacancyRequest().status(VacancyStatus.CLOSED).build();

        assertThatThrownBy(() -> Vacancy.create(request, DEFAULT_ID, DEFAULT_NOW))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Initial vacancy status must be DRAFT or OPEN, got: CLOSED");
    }
}
