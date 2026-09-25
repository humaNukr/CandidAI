package ua.edu.ukma.candidai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    @DisplayName("verify - should pass Spring Modulith architectural verification")
    void givenApplicationModules_verify_shouldPassArchitectureValidation() {
        ApplicationModules.of(CandidAiApplication.class).verify();
    }
}
