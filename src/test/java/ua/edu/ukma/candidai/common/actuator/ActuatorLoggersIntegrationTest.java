package ua.edu.ukma.candidai.common.actuator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;
import ua.edu.ukma.candidai.assessment.service.AssessmentServiceImpl;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.common.actuator.ActuatorTestResources.*;
import static ua.edu.ukma.candidai.common.security.SecurityTestResources.*;

@IntegrationTest
class ActuatorLoggersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssessmentService assessmentService;

    @AfterEach
    void resetLoggerLevels() throws Exception {
        mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                .with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESET_LEVEL_PAYLOAD));

        mockMvc.perform(post(RECRUITMENT_LOGGER_URL)
                .with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESET_LEVEL_PAYLOAD));
    }

    @Test
    void givenAnonymousRequest_getLoggers_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(ACTUATOR_LOGGERS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAnonymousRequest_getLogger_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(TARGET_ASSESSMENT_LOGGER_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAnonymousRequest_changeLogLevel_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void givenCandidateRole_changeLogLevel_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void givenRecruiterRole_changeLogLevel_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_getLogger_shouldReturnDefaultLevel() throws Exception {
        MvcResult result = mockMvc.perform(get(TARGET_ASSESSMENT_LOGGER_URL))
                .andExpect(status().isOk())
                .andReturn();

        LoggerLevelPayload actual = parseResponse(result, LoggerLevelPayload.class);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(new LoggerLevelPayload(null, "INFO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_changeLogLevel_shouldUpdateConfiguredAndEffectiveLevel() throws Exception {
        mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEBUG_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        MvcResult updatedResult = mockMvc.perform(get(TARGET_ASSESSMENT_LOGGER_URL))
                .andExpect(status().isOk())
                .andReturn();

        LoggerLevelPayload updatedPayload = parseResponse(updatedResult, LoggerLevelPayload.class);
        assertThat(updatedPayload)
                .usingRecursiveComparison()
                .isEqualTo(new LoggerLevelPayload("DEBUG", "DEBUG"));

        mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        MvcResult resetResult = mockMvc.perform(get(TARGET_ASSESSMENT_LOGGER_URL))
                .andExpect(status().isOk())
                .andReturn();

        LoggerLevelPayload resetPayload = parseResponse(resetResult, LoggerLevelPayload.class);
        assertThat(resetPayload)
                .usingRecursiveComparison()
                .isEqualTo(new LoggerLevelPayload(null, "INFO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenDynamicDebugSwitch_executeService_shouldEmitDebugLogsOnlyWhileDebugEnabled() throws Exception {
        Logger assessmentLogger = (Logger) LoggerFactory.getLogger(AssessmentServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        assessmentLogger.addAppender(listAppender);

        try {
            UUID testId = UUID.randomUUID();
            assertThatThrownBy(() -> assessmentService.getScreeningResult(testId))
                    .isInstanceOf(ResourceNotFoundException.class);

            boolean hasDebugLogBefore = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.DEBUG
                            && event.getFormattedMessage().contains("Querying screening repository"));
            assertThat(hasDebugLogBefore).isFalse();

            mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(DEBUG_LEVEL_PAYLOAD))
                    .andExpect(status().isNoContent());

            MvcResult debugResult = mockMvc.perform(get(TARGET_ASSESSMENT_LOGGER_URL))
                    .andExpect(status().isOk())
                    .andReturn();
            LoggerLevelPayload debugPayload = parseResponse(debugResult, LoggerLevelPayload.class);
            assertThat(debugPayload)
                    .usingRecursiveComparison()
                    .isEqualTo(new LoggerLevelPayload("DEBUG", "DEBUG"));

            assertThatThrownBy(() -> assessmentService.getScreeningResult(testId))
                    .isInstanceOf(ResourceNotFoundException.class);

            boolean hasDebugLogAfter = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.DEBUG
                            && event.getFormattedMessage().contains("Querying screening repository"));
            assertThat(hasDebugLogAfter).isTrue();

            mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(RESET_LEVEL_PAYLOAD))
                    .andExpect(status().isNoContent());

            listAppender.list.clear();
            assertThatThrownBy(() -> assessmentService.getScreeningResult(testId))
                    .isInstanceOf(ResourceNotFoundException.class);

            boolean hasDebugLogAfterReset = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.DEBUG);
            assertThat(hasDebugLogAfterReset).isFalse();
        } finally {
            assessmentLogger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    void givenAdminHttpBasicCredentials_changeLogLevel_shouldSucceed() throws Exception {
        mockMvc.perform(post(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACE_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        MvcResult updatedResult = mockMvc.perform(get(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        LoggerLevelPayload updatedPayload = parseResponse(updatedResult, LoggerLevelPayload.class);
        assertThat(updatedPayload)
                .usingRecursiveComparison()
                .isEqualTo(new LoggerLevelPayload("TRACE", "TRACE"));

        mockMvc.perform(post(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_LEVEL_PAYLOAD))
                .andExpect(status().isNoContent());

        MvcResult resetResult = mockMvc.perform(get(RECRUITMENT_LOGGER_URL)
                        .with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        LoggerLevelPayload resetPayload = parseResponse(resetResult, LoggerLevelPayload.class);
        assertThat(resetPayload)
                .usingRecursiveComparison()
                .isEqualTo(new LoggerLevelPayload(null, "INFO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenInvalidLogLevel_changeLogLevel_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post(TARGET_ASSESSMENT_LOGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_LEVEL_PAYLOAD))
                .andExpect(status().isBadRequest());
    }

    private <T> T parseResponse(MvcResult result, Class<T> responseType) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), responseType);
    }
}
