package ua.edu.ukma.candidai.common.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.actuator.ActuatorTestResources.*;

class ActuatorEnvSanitizingConfigurationTest {

    private final SanitizingFunction sanitizingFunction
            = new ActuatorEnvSanitizingConfiguration().candidAiSanitizingFunction();

    @Test
    void givenSensitivePropertyKeys_apply_shouldMaskValuesWithAsterisks() {
        for (String key : SENSITIVE_PROPERTY_KEYS) {
            SanitizableData input = new SanitizableData(TEST_PROPERTY_SOURCE, key, "raw-secret-value");

            SanitizableData actual = sanitizingFunction.apply(input);

            assertThat(actual.getValue()).isEqualTo(MASKED_VALUE);
        }
    }

    @Test
    void givenNonSensitivePropertyKeys_apply_shouldPreserveOriginalValues() {
        SAFE_PROPERTIES.forEach((key, value) -> {
            SanitizableData input = new SanitizableData(TEST_PROPERTY_SOURCE, key, value);

            SanitizableData actual = sanitizingFunction.apply(input);

            assertThat(actual.getValue()).isEqualTo(value);
        });
    }
}
