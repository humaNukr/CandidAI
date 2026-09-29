package ua.edu.ukma.candidai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none"
})
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
