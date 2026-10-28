package ua.edu.ukma.candidai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CandidAiApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("contextLoads - should load application context successfully")
    void givenApplication_contextLoads_shouldStartContext() {
        boolean isRunning = applicationContext.getId() != null;

        assertThat(isRunning).isTrue();
        assertThat(applicationContext).isNotNull();
    }
}
