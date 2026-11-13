package ua.edu.ukma.candidai.common.actuator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;
import ua.edu.ukma.candidai.assessment.service.AssessmentServiceImpl;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.security.SecurityTestResources;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@DisplayName("Integration tests for Actuator dynamic logging via Loggers API")
class ActuatorLoggersIntegrationTest {

    private static final String LOGGERS_BASE_URL = "/actuator/loggers";
    private static final String TARGET_PACKAGE = "ua.edu.ukma.candidai.assessment.service";
    private static final String TARGET_LOGGER_URL = "/actuator/loggers/" + TARGET_PACKAGE;
    private static final String RECRUITMENT_PACKAGE = "ua.edu.ukma.candidai.recruitment.service";
    private static final String RECRUITMENT_LOGGER_URL = "/actuator/loggers/" + RECRUITMENT_PACKAGE;

    private static final String DEBUG_LEVEL_PAYLOAD = "{\"configuredLevel\":\"DEBUG\"}";
    private static final String TRACE_LEVEL_PAYLOAD = "{\"configuredLevel\":\"TRACE\"}";
    private static final String RESET_LEVEL_PAYLOAD = "{\"configuredLevel\":null}";
    private static final String INVALID_LEVEL_PAYLOAD = "{\"configuredLevel\":\"INVALID_LEVEL\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssessmentService assessmentService;

    @AfterEach
    void resetLoggerLevels() throws Exception {
        mockMvc.perform(post(TARGET_LOGGER_URL)
                        .with(httpBasic(SecurityTestResources.ADMIN_EMAIL, SecurityTestResources.ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_LEVEL_PAYLOAD));

        mockMvc.perform(post(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(SecurityTestResources.ADMIN_EMAIL, SecurityTestResources.ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_LEVEL_PAYLOAD));
    }

    @Test
    @DisplayName("GET /actuator/loggers - should return 401 Unauthorized for anonymous request")
    void getLoggers_WhenAnonymous_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(LOGGERS_BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /actuator/loggers/{package} - should return 401 Unauthorized for anonymous request")
    void getLogger_WhenAnonymous_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(TARGET_LOGGER_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /actuator/loggers/{package} - should return 401 Unauthorized for anonymous request")
    void changeLogLevel_WhenAnonymous_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post(TARGET_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    @DisplayName("POST /actuator/loggers/{package} - should return 403 Forbidden for candidate role")
    void changeLogLevel_WhenCandidateRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post(TARGET_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    @DisplayName("POST /actuator/loggers/{package} - should return 403 Forbidden for recruiter role")
    void changeLogLevel_WhenRecruiterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post(TARGET_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /actuator/loggers/{package} - should return default inherited INFO level for admin")
    void getLogger_WhenAdminRole_ShouldReturnDefaultLevel() throws Exception {
        mockMvc.perform(get(TARGET_LOGGER_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveLevel").value("INFO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /actuator/loggers/{package} - should dynamically change level to DEBUG without JVM restart")
    void changeLogLevel_WhenAdminRole_ShouldUpdateConfiguredAndEffectiveLevel() throws Exception {
        mockMvc.perform(post(TARGET_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TARGET_LOGGER_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel").value("DEBUG"))
                .andExpect(jsonPath("$.effectiveLevel").value("DEBUG"));

        mockMvc.perform(post(TARGET_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TARGET_LOGGER_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel").doesNotExist())
                .andExpect(jsonPath("$.effectiveLevel").value("INFO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Dynamic logging scenario - should emit DEBUG log events only after dynamically switching level")
    void dynamicLoggingScenario_WhenLevelSwitchedToDebug_ShouldEmitDebugLogsDuringServiceExecution() throws Exception {
        Logger assessmentLogger = (Logger) LoggerFactory.getLogger(AssessmentServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        assessmentLogger.addAppender(listAppender);

        try {
            UUID testId = UUID.randomUUID();
            assertThrows(ResourceNotFoundException.class, () -> assessmentService.getScreeningResult(testId));

            boolean hasDebugLogBefore = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.DEBUG
                            && event.getFormattedMessage().contains("Querying screening repository"));
            assertThat(hasDebugLogBefore).isFalse();

            mockMvc.perform(post(TARGET_LOGGER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DEBUG_LEVEL_PAYLOAD))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(TARGET_LOGGER_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveLevel").value("DEBUG"));

            assertThrows(ResourceNotFoundException.class, () -> assessmentService.getScreeningResult(testId));

            boolean hasDebugLogAfter = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.DEBUG
                            && event.getFormattedMessage().contains("Querying screening repository"));
            assertThat(hasDebugLogAfter).isTrue();

            mockMvc.perform(post(TARGET_LOGGER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RESET_LEVEL_PAYLOAD))
                    .andExpect(status().isNoContent());

            listAppender.list.clear();
            assertThrows(ResourceNotFoundException.class, () -> assessmentService.getScreeningResult(testId));

            boolean hasDebugLogAfterReset = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.DEBUG);
            assertThat(hasDebugLogAfterReset).isFalse();
        } finally {
            assessmentLogger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    @DisplayName("POST /actuator/loggers/{package} - should configure level via admin HTTP Basic auth")
    void changeLogLevel_WhenAdminHttpBasicCredentials_ShouldSucceed() throws Exception {
        mockMvc.perform(post(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(SecurityTestResources.ADMIN_EMAIL, SecurityTestResources.ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACE_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(SecurityTestResources.ADMIN_EMAIL, SecurityTestResources.ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel").value("TRACE"))
                .andExpect(jsonPath("$.effectiveLevel").value("TRACE"));

        mockMvc.perform(post(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(SecurityTestResources.ADMIN_EMAIL, SecurityTestResources.ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(SecurityTestResources.ADMIN_EMAIL, SecurityTestResources.ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel").doesNotExist())
                .andExpect(jsonPath("$.effectiveLevel").value("INFO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /actuator/loggers/{package} - should return 400 Bad Request for invalid log level")
    void changeLogLevel_WhenInvalidLogLevel_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post(TARGET_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_LEVEL_PAYLOAD))
                .andExpect(status().isBadRequest());
    }
}
