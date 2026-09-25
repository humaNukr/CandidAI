package ua.edu.ukma.candidai.recruitment.dto.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStateMachineTest {

    @ParameterizedTest(name = "from {0} to {1} should be {2}")
    @CsvSource({
            "APPLIED, SCREENING, true",
            "APPLIED, REJECTED, true",
            "APPLIED, INTERVIEW, false",
            "APPLIED, OFFER, false",
            "SCREENING, INTERVIEW, true",
            "SCREENING, REJECTED, true",
            "SCREENING, APPLIED, false",
            "SCREENING, OFFER, false",
            "INTERVIEW, OFFER, true",
            "INTERVIEW, REJECTED, true",
            "INTERVIEW, SCREENING, false",
            "OFFER, HIRED, true",
            "OFFER, REJECTED, true",
            "OFFER, INTERVIEW, false",
            "OFFER, APPLIED, false",
            "HIRED, APPLIED, false",
            "HIRED, SCREENING, false",
            "HIRED, INTERVIEW, false",
            "HIRED, OFFER, false",
            "HIRED, REJECTED, false",
            "REJECTED, APPLIED, false",
            "REJECTED, SCREENING, false",
            "REJECTED, INTERVIEW, false",
            "REJECTED, OFFER, false",
            "REJECTED, HIRED, false"
    })
    @DisplayName("canTransitionTo should strictly enforce recruitment funnel state machine")
    void shouldValidateTransitionsCorrectly(ApplicationStatus from, ApplicationStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    @DisplayName("canTransitionTo should return false when target status is null")
    void givenNullTarget_canTransitionTo_shouldReturnFalse() {
        assertThat(ApplicationStatus.APPLIED.canTransitionTo(null)).isFalse();
    }
}
