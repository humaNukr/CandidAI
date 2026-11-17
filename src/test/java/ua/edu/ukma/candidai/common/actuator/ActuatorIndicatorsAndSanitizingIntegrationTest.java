package ua.edu.ukma.candidai.common.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.common.actuator.ActuatorTestResources.*;

@IntegrationTest
class ActuatorIndicatorsAndSanitizingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_getActuatorHealth_shouldIncludeResumeStorageAndDbComponentsUp() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_HEALTH_PATH))
                .andExpect(status().isOk())
                .andReturn();

        HealthRootPayload payload = parseResponse(result, HealthRootPayload.class);

        assertThat(payload.status()).isEqualTo("UP");
        assertThat(payload.components()).containsKeys("resumeStorage", "db");
        HealthComponentPayload resumeStorage = payload.components().get("resumeStorage");
        assertThat(resumeStorage.status()).isEqualTo("UP");
        assertThat(resumeStorage.details()).containsEntry("writable", true);
        assertThat(resumeStorage.details()).containsKey("freeSpaceBytes");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_getResumeStorageHealthComponent_shouldReturnUpWithDetails() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_RESUME_STORAGE_HEALTH_PATH))
                .andExpect(status().isOk())
                .andReturn();

        HealthComponentPayload payload = parseResponse(result, HealthComponentPayload.class);

        assertThat(payload.status()).isEqualTo("UP");
        assertThat(payload.details()).containsEntry("writable", true);
        assertThat(payload.details()).containsKey("path");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_getActuatorEnv_shouldMaskSensitivePropertiesAndExposeSafeProperties() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_ENV_PATH))
                .andExpect(status().isOk())
                .andReturn();

        EnvEndpointPayload payload = parseResponse(result, EnvEndpointPayload.class);

        assertThat(findPropertyValue(payload, "candidai.security.jwt.secret")).contains(MASKED_VALUE);
        assertThat(findPropertyValue(payload, "spring.datasource.password")).contains(MASKED_VALUE);
        assertThat(findPropertyValue(payload, "spring.application.name")).contains("CandidAI");
    }

    @Test
    void givenAnonymousRequest_getActuatorInfo_shouldReturnApplicationAndFeatureInfo() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_INFO_PATH))
                .andExpect(status().isOk())
                .andReturn();

        ActuatorInfoPayload actual = parseResponse(result, ActuatorInfoPayload.class);

        ActuatorInfoPayload expected = new ActuatorInfoPayload(
                new ApplicationInfoPayload("CandidAI", "Smart HR & AI Candidate Screening Platform"),
                new ApplicationInfoPayload("CandidAI", "Smart HR & AI Candidate Screening Platform"),
                new FeaturesInfoPayload(false, true, true)
        );
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    private Optional<Object> findPropertyValue(EnvEndpointPayload payload, String propertyKey) {
        return payload.propertySources().stream()
                .filter(source -> source.properties() != null && source.properties().containsKey(propertyKey))
                .map(source -> source.properties().get(propertyKey).value())
                .findFirst();
    }

    private <T> T parseResponse(MvcResult result, Class<T> responseType) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), responseType);
    }
}
