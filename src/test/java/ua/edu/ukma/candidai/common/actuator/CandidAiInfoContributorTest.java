package ua.edu.ukma.candidai.common.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.actuator.ActuatorTestResources.*;

class CandidAiInfoContributorTest {

    @Test
    void givenConfiguredEnvironment_contribute_shouldPopulateApplicationAndFeatureDetails() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("notification.mail.enabled", "true")
                .withProperty("notification.telegram.enabled", "false")
                .withProperty("candidai.gemini.api-key", "test-gemini-key");
        CandidAiInfoContributor contributor = new CandidAiInfoContributor(environment);
        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);

        assertThat(builder.build())
                .usingRecursiveComparison()
                .isEqualTo(expectedCandidAiInfo(true, false, true));
    }

    @Test
    void givenBlankGeminiKey_contribute_shouldReportAiScreeningNotConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("notification.mail.enabled", "false")
                .withProperty("notification.telegram.enabled", "true")
                .withProperty("candidai.gemini.api-key", "");
        CandidAiInfoContributor contributor = new CandidAiInfoContributor(environment);
        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);

        assertThat(builder.build())
                .usingRecursiveComparison()
                .isEqualTo(expectedCandidAiInfo(false, true, false));
    }
}
