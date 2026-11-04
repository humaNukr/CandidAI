package ua.edu.ukma.candidai.common.config;

import java.util.List;

public final class DeploymentConfigTestResources {

    public static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
    public static final String ACTUATOR_LIVENESS_PATH = "/actuator/health/liveness";
    public static final String ACTUATOR_READINESS_PATH = "/actuator/health/readiness";
    public static final String ACTUATOR_ENV_PATH = "/actuator/env";
    public static final String LOGIN_PATH = "/login";

    public static final List<String> TRUSTED_PROXY_IPS = List.of(
            "127.0.0.1",
            "::1",
            "10.0.1.5",
            "172.18.0.2",
            "192.168.1.10"
    );

    public static final List<String> UNTRUSTED_PUBLIC_IPS = List.of(
            "8.8.8.8",
            "203.0.113.42",
            "172.32.0.1"
    );

    private DeploymentConfigTestResources() {
    }

    public static ActuatorRootHealthPayload expectedRootHealthPayload() {
        return new ActuatorRootHealthPayload("UP", List.of("liveness", "readiness"));
    }

    public static ActuatorProbePayload expectedUpProbePayload() {
        return new ActuatorProbePayload("UP");
    }

    public static LifecyclePropertiesSnapshot expectedLifecycleProperties() {
        return new LifecyclePropertiesSnapshot(
                "graceful",
                "30s",
                "8080",
                "native",
                "false",
                "health",
                "true",
                "never"
        );
    }

    public static ProdPropertiesSnapshot expectedProdProperties() {
        return new ProdPropertiesSnapshot(
                "::",
                "true",
                "off",
                "false",
                "INFO",
                "INFO"
        );
    }

    public record ActuatorRootHealthPayload(String status, List<String> groups) {
    }

    public record ActuatorProbePayload(String status) {
    }

    public record LifecyclePropertiesSnapshot(
            String shutdownPolicy,
            String shutdownPhaseTimeout,
            String serverPort,
            String forwardHeadersStrategy,
            String sessionCookieSecure,
            String exposedEndpoints,
            String probesEnabled,
            String showDetails
    ) {
    }

    public record ProdPropertiesSnapshot(
            String serverAddress,
            String sessionCookieSecure,
            String bannerMode,
            String jpaShowSql,
            String springBootLogLevel,
            String hikariLogLevel
    ) {
    }
}
