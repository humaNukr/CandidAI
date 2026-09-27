package ua.edu.ukma.candidai;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none"
})
class CandidAiApplicationTest {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("contextLoads - should load application context successfully")
    void givenApplication_contextLoads_shouldStartContext() {
    }
}
