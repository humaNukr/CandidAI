package ua.edu.ukma.candidai.common.testcontainers;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final JdbcClient jdbcClient;

    public void cleanTables() {
        jdbcClient.sql("""
                TRUNCATE TABLE 
                    interview_feedbacks, 
                    interviews, 
                    applications, 
                    vacancy_skills, 
                    vacancies 
                CASCADE;
                """).update();

        jdbcClient.sql("""
                DELETE FROM companies 
                WHERE id != 'c0000000-0000-0000-0000-000000000003';
                """).update();

        jdbcClient.sql("""
                DELETE FROM users 
                WHERE id NOT IN (
                    'a0000000-0000-0000-0000-000000000001', 
                    'c0000000-0000-0000-0000-000000000001'
                );
                """).update();
    }
}
