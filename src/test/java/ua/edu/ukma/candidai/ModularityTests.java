package ua.edu.ukma.candidai;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void verifyModularity() {
        ApplicationModules.of(CandidAiApplication.class).verify();
    }
}
