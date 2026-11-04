package ua.edu.ukma.candidai.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ForwardedHeadersAndDeploymentTest {

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("deploymentConfig - should have graceful shutdown and timeout configured")
    void deploymentConfig_shouldHaveGracefulShutdownConfigured() {
        String shutdownPolicy = environment.getProperty("server.shutdown");
        String shutdownTimeout = environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase");

        assertThat(shutdownPolicy).isEqualTo("graceful");
        assertThat(shutdownTimeout).isEqualTo("30s");
    }

    @Test
    @DisplayName("deploymentConfig - should configure native forwarded headers strategy")
    void deploymentConfig_shouldConfigureForwardedHeadersStrategy() {
        String forwardedStrategy = environment.getProperty("server.forward-headers-strategy");
        String internalProxies = environment.getProperty("server.tomcat.remoteip.internal-proxies");

        assertThat(forwardedStrategy).isEqualTo("native");
        assertThat(internalProxies).isNotBlank();
    }

    @Test
    @DisplayName("http - should accept requests with X-Forwarded headers without errors")
    void http_shouldAcceptRequestsWithForwardedHeaders() throws Exception {
        mockMvc.perform(get("/login")
                        .header("X-Forwarded-For", "203.0.113.195")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Port", "443")
                        .header("Host", "candidai.example.com"))
                .andExpect(status().isOk());
    }
}
