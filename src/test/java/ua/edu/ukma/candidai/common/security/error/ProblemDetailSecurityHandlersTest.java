package ua.edu.ukma.candidai.common.security.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.util.CommonGenerator;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailSecurityHandlersTest {

    private static final String TEST_URI = "/api/v1/protected-resource";
    private static final String TEST_TRACE_ID = "trace-12345";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    private ObjectMapper objectMapper;
    private ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private ProblemDetailAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        CommonGenerator commonGenerator = new CommonGenerator();
        authenticationEntryPoint = new ProblemDetailAuthenticationEntryPoint(objectMapper, commonGenerator);
        accessDeniedHandler = new ProblemDetailAccessDeniedHandler(objectMapper, commonGenerator);
        MDC.put(MDC_TRACE_ID_KEY, TEST_TRACE_ID);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("AuthenticationEntryPoint should return 401 RFC 9457 ProblemDetail")
    void givenAuthenticationException_commence_shouldReturn401ProblemDetail() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", TEST_URI);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Bad credentials")
        );

        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType())
                .contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problemDetail = objectMapper.readValue(
                response.getContentAsString(),
                ProblemDetail.class
        );
        assertThat(problemDetail.getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problemDetail.getTitle())
                .isEqualTo("Автентифікація обов'язкова");
        assertThat(problemDetail.getProperties())
                .containsEntry(MDC_TRACE_ID_KEY, TEST_TRACE_ID);
    }

    @Test
    @DisplayName("AccessDeniedHandler should return 403 RFC 9457 ProblemDetail")
    void givenAccessDeniedException_handle_shouldReturn403ProblemDetail() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", TEST_URI);
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException("Access denied")
        );

        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType())
                .contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problemDetail = objectMapper.readValue(
                response.getContentAsString(),
                ProblemDetail.class
        );
        assertThat(problemDetail.getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problemDetail.getTitle())
                .isEqualTo("Доступ заборонено");
        assertThat(problemDetail.getProperties())
                .containsEntry(MDC_TRACE_ID_KEY, TEST_TRACE_ID);
    }
}
