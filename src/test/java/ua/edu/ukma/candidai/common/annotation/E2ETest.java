package ua.edu.ukma.candidai.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ua.edu.ukma.candidai.common.testcontainers.TestcontainersConfiguration;
import ua.edu.ukma.candidai.common.wiremock.ExternalServicesStubConfiguration;

/**
 * End-to-end тести: застосунок запущено на справжньому HTTP-порту (RANDOM_PORT),
 * клієнт (RestTestClient або браузер Playwright) звертається до нього через мережу.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestcontainersConfiguration.class, ExternalServicesStubConfiguration.class})
@Sql(scripts = "/sql/reset-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public @interface E2ETest {
}
