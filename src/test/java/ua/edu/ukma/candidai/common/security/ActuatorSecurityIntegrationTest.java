package ua.edu.ukma.candidai.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@DisplayName("Integration tests for Actuator security rules, endpoints isolation, and HTTP Basic authentication")
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health - should be publicly accessible without authentication")
    void health_WhenAnonymous_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/health/liveness - should be publicly accessible for container probes")
    void livenessProbe_WhenAnonymous_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/health/readiness - should be publicly accessible for container probes")
    void readinessProbe_WhenAnonymous_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/info - should be publicly accessible without authentication")
    void info_WhenAnonymous_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/loggers - should return 401 Unauthorized for anonymous requests")
    void loggers_WhenAnonymous_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /actuator/env - should return 401 Unauthorized for anonymous requests")
    void env_WhenAnonymous_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /actuator/metrics - should return 401 Unauthorized for anonymous requests")
    void metrics_WhenAnonymous_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    @DisplayName("GET /actuator/loggers - should return 403 Forbidden for candidate role")
    void loggers_WhenCandidateRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    @DisplayName("GET /actuator/loggers - should return 403 Forbidden for recruiter role")
    void loggers_WhenRecruiterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /actuator/loggers - should return 200 OK for ADMIN role")
    void loggers_WhenAdminRole_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levels").isArray())
                .andExpect(jsonPath("$.loggers").isMap());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /actuator/health - should include component details for ADMIN role")
    void health_WhenAdminRole_ShouldIncludeDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").isMap());
    }

    @Test
    @DisplayName("GET /actuator/loggers - should authenticate seeded admin user via HTTP Basic")
    void loggers_WhenAdminHttpBasicCredentials_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/loggers")
                        .with(httpBasic("admin@candidai.com", "AdminPass123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levels").isArray())
                .andExpect(jsonPath("$.loggers").isMap());
    }
}
