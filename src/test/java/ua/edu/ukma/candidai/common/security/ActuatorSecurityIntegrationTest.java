package ua.edu.ukma.candidai.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.config.DeploymentConfigTestResources;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.common.actuator.ActuatorTestResources.*;
import static ua.edu.ukma.candidai.common.security.SecurityTestResources.*;

@IntegrationTest
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAnonymousRequest_getHealth_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_HEALTH_PATH))
                .andExpect(status().isOk())
                .andReturn();

        DeploymentConfigTestResources.ActuatorRootHealthPayload actual = parseResponse(
                result,
                DeploymentConfigTestResources.ActuatorRootHealthPayload.class
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(DeploymentConfigTestResources.expectedRootHealthPayload());
    }

    @Test
    void givenAnonymousRequest_getLivenessProbe_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_LIVENESS_PATH))
                .andExpect(status().isOk())
                .andReturn();

        DeploymentConfigTestResources.ActuatorProbePayload actual = parseResponse(
                result,
                DeploymentConfigTestResources.ActuatorProbePayload.class
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(DeploymentConfigTestResources.expectedUpProbePayload());
    }

    @Test
    void givenAnonymousRequest_getReadinessProbe_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_READINESS_PATH))
                .andExpect(status().isOk())
                .andReturn();

        DeploymentConfigTestResources.ActuatorProbePayload actual = parseResponse(
                result,
                DeploymentConfigTestResources.ActuatorProbePayload.class
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(DeploymentConfigTestResources.expectedUpProbePayload());
    }

    @Test
    void givenAnonymousRequest_getInfo_shouldReturnOk() throws Exception {
        mockMvc.perform(get(ACTUATOR_INFO_PATH))
                .andExpect(status().isOk());
    }

    @Test
    void givenAnonymousRequest_getLoggers_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(ACTUATOR_LOGGERS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAnonymousRequest_getEnv_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(ACTUATOR_ENV_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAnonymousRequest_getMetrics_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(ACTUATOR_METRICS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void givenCandidateRole_getLoggers_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get(ACTUATOR_LOGGERS_PATH))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void givenRecruiterRole_getLoggers_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get(ACTUATOR_LOGGERS_PATH))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_getLoggers_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_LOGGERS_PATH))
                .andExpect(status().isOk())
                .andReturn();

        LoggersSummaryPayload actual = parseResponse(result, LoggersSummaryPayload.class);

        assertThat(actual.levels()).isNotEmpty();
        assertThat(actual.loggers()).isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_getHealth_shouldIncludeDetails() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_HEALTH_PATH))
                .andExpect(status().isOk())
                .andReturn();

        HealthRootPayload actual = parseResponse(result, HealthRootPayload.class);

        assertThat(actual.status()).isEqualTo("UP");
        assertThat(actual.components()).isNotEmpty();
    }

    @Test
    void givenAdminHttpBasicCredentials_getLoggers_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_LOGGERS_PATH)
                        .with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        LoggersSummaryPayload actual = parseResponse(result, LoggersSummaryPayload.class);

        assertThat(actual.levels()).isNotEmpty();
        assertThat(actual.loggers()).isNotEmpty();
    }

    private <T> T parseResponse(MvcResult result, Class<T> responseType) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), responseType);
    }
}
