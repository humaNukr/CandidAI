package ua.edu.ukma.candidai.common.config;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.common.config.DeploymentConfigTestResources.*;

@IntegrationTest
class DeploymentLifecycleAndHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("loadConfig - should configure graceful shutdown, forwarded headers, and actuator health exposure")
    void givenDefaultProfile_loadLifecycleProperties_shouldMatchProductionReadyDefaults() {
        LifecyclePropertiesSnapshot actual = new LifecyclePropertiesSnapshot(
                environment.getProperty("server.shutdown"),
                environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase"),
                environment.getProperty("server.port"),
                environment.getProperty("server.forward-headers-strategy"),
                environment.getProperty("server.servlet.session.cookie.secure"),
                environment.getProperty("management.endpoints.web.exposure.include"),
                environment.getProperty("management.endpoint.health.probes.enabled"),
                environment.getProperty("management.endpoint.health.show-details")
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedLifecycleProperties());
    }

    @Test
    @DisplayName("internalProxies - should trust loopback and RFC 1918 networks and reject public IPs")
    void givenInternalProxiesRegex_matchAddresses_shouldAllowTrustedSubnetsAndRejectPublicIps() {
        String regex = environment.getProperty("server.tomcat.remoteip.internal-proxies");
        assertThat(regex).isNotBlank();

        Pattern pattern = Pattern.compile(regex);
        assertThat(TRUSTED_PROXY_IPS).allMatch(ip -> pattern.matcher(ip).matches());
        assertThat(UNTRUSTED_PUBLIC_IPS).noneMatch(ip -> pattern.matcher(ip).matches());
    }

    @Test
    @DisplayName("loadProdConfig - should configure dual-stack binding, secure cookies, and shutdown logging")
    void givenProdProfileResource_loadProdProperties_shouldMatchProductionHardeningDefaults() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(
                "application-prod",
                new ClassPathResource("application-prod.yaml")
        );
        StandardEnvironment prodEnv = new StandardEnvironment();
        sources.forEach(prodEnv.getPropertySources()::addFirst);

        ProdPropertiesSnapshot actual = new ProdPropertiesSnapshot(
                prodEnv.getProperty("server.address"),
                prodEnv.getProperty("server.servlet.session.cookie.secure"),
                prodEnv.getProperty("spring.main.banner-mode"),
                prodEnv.getProperty("spring.jpa.show-sql"),
                prodEnv.getProperty("logging.level.org.springframework.boot"),
                prodEnv.getProperty("logging.level.com.zaxxer.hikari")
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedProdProperties());
    }

    @Test
    @DisplayName("getHealth - should allow anonymous access to actuator health endpoint and return UP with groups")
    void givenAnonymousRequest_getActuatorHealth_shouldReturn200AndUpPayload() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_HEALTH_PATH))
                .andExpect(status().isOk())
                .andReturn();

        ActuatorRootHealthPayload actual = parseResponse(result, ActuatorRootHealthPayload.class);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedRootHealthPayload());
    }

    @Test
    @DisplayName("getProbes - should allow anonymous access to liveness and readiness probes")
    void givenAnonymousRequest_getActuatorProbes_shouldReturn200AndUpPayload() throws Exception {
        MvcResult livenessResult = mockMvc.perform(get(ACTUATOR_LIVENESS_PATH))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult readinessResult = mockMvc.perform(get(ACTUATOR_READINESS_PATH))
                .andExpect(status().isOk())
                .andReturn();

        ActuatorProbePayload actualLiveness = parseResponse(livenessResult, ActuatorProbePayload.class);
        ActuatorProbePayload actualReadiness = parseResponse(readinessResult, ActuatorProbePayload.class);

        assertThat(actualLiveness)
                .usingRecursiveComparison()
                .isEqualTo(expectedUpProbePayload());
        assertThat(actualReadiness)
                .usingRecursiveComparison()
                .isEqualTo(expectedUpProbePayload());
    }

    @Test
    @DisplayName("getActuatorEnv - should not expose sensitive actuator endpoints anonymously and return 401")
    void givenAnonymousRequest_getUnexposedActuatorEnv_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(ACTUATOR_ENV_PATH))
                .andExpect(status().isUnauthorized());
    }

    private <T> T parseResponse(MvcResult result, Class<T> responseType) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                responseType
        );
    }
}
