package ua.edu.ukma.candidai.common.actuator;

import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.health.contributor.Status;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ActuatorTestResources {

    public static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
    public static final String ACTUATOR_LIVENESS_PATH = "/actuator/health/liveness";
    public static final String ACTUATOR_READINESS_PATH = "/actuator/health/readiness";
    public static final String ACTUATOR_RESUME_STORAGE_HEALTH_PATH = "/actuator/health/resumeStorage";
    public static final String ACTUATOR_INFO_PATH = "/actuator/info";
    public static final String ACTUATOR_ENV_PATH = "/actuator/env";
    public static final String ACTUATOR_LOGGERS_PATH = "/actuator/loggers";
    public static final String ACTUATOR_METRICS_PATH = "/actuator/metrics";
    public static final String TARGET_ASSESSMENT_PACKAGE = "ua.edu.ukma.candidai.assessment.service";
    public static final String TARGET_ASSESSMENT_LOGGER_URL = ACTUATOR_LOGGERS_PATH + "/" + TARGET_ASSESSMENT_PACKAGE;
    public static final String RECRUITMENT_PACKAGE = "ua.edu.ukma.candidai.recruitment.service";
    public static final String RECRUITMENT_LOGGER_URL = ACTUATOR_LOGGERS_PATH + "/" + RECRUITMENT_PACKAGE;
    public static final String DEBUG_LEVEL_PAYLOAD = "{\"configuredLevel\":\"DEBUG\"}";
    public static final String TRACE_LEVEL_PAYLOAD = "{\"configuredLevel\":\"TRACE\"}";
    public static final String RESET_LEVEL_PAYLOAD = "{\"configuredLevel\":null}";
    public static final String INVALID_LEVEL_PAYLOAD = "{\"configuredLevel\":\"INVALID_LEVEL\"}";

    public static final String MASKED_VALUE = SanitizableData.SANITIZED_VALUE;

    public static final List<String> SENSITIVE_PROPERTY_KEYS = List.of(
            "candidai.security.jwt.secret",
            "spring.datasource.password",
            "notification.telegram.bot-token",
            "candidai.gemini.api-key",
            "DB_PASSWORD",
            "JWT_SECRET",
            "TELEGRAM_BOT_TOKEN",
            "GEMINI_API_KEY"
    );

    public static final Map<String, String> SAFE_PROPERTIES = Map.of(
            "spring.application.name", "CandidAI",
            "server.port", "8080",
            "candidai.gemini.model", "gemini-3.5-flash-lite"
    );

    public static final MapPropertySource TEST_PROPERTY_SOURCE = new MapPropertySource(
            "test-props",
            Map.of("spring.application.name", "CandidAI")
    );

    private ActuatorTestResources() {
    }

    public static StorageHealthSnapshot expectedUpStorageHealth(Path storagePath) {
        return new StorageHealthSnapshot(
                Status.UP.getCode(),
                storagePath.toAbsolutePath().normalize().toString(),
                true
        );
    }

    public static StorageHealthErrorSnapshot expectedDownStorageHealth(Path storagePath, String reason) {
        return new StorageHealthErrorSnapshot(
                Status.DOWN.getCode(),
                storagePath.toAbsolutePath().normalize().toString(),
                reason
        );
    }

    public static Info expectedCandidAiInfo(
            boolean mailEnabled,
            boolean telegramEnabled,
            boolean geminiConfigured
    ) {
        return new Info.Builder()
                .withDetail("features", Map.of(
                        "mailNotificationsEnabled", mailEnabled,
                        "telegramBotEnabled", telegramEnabled,
                        "aiScreeningConfigured", geminiConfigured
                ))
                .build();
    }

    public record StorageHealthSnapshot(String status, String path, Boolean writable) {
    }

    public record StorageHealthErrorSnapshot(String status, String path, String reason) {
    }

    public record ActuatorInfoPayload(
            ApplicationInfoPayload app,
            FeaturesInfoPayload features
    ) {
    }

    public record ApplicationInfoPayload(String name, String description) {
    }

    public record FeaturesInfoPayload(
            boolean mailNotificationsEnabled,
            boolean telegramBotEnabled,
            boolean aiScreeningConfigured
    ) {
    }

    public record HealthComponentPayload(String status, Map<String, Object> details) {
    }

    public record HealthRootPayload(
            String status,
            Map<String, HealthComponentPayload> components,
            List<String> groups
    ) {
    }

    public record EnvPropertyValue(Object value, String origin) {
    }

    public record EnvPropertySource(String name, Map<String, EnvPropertyValue> properties) {
    }

    public record EnvEndpointPayload(
            List<String> activeProfiles,
            List<String> defaultProfiles,
            List<EnvPropertySource> propertySources
    ) {
    }

    public record LoggersSummaryPayload(
            List<String> levels,
            Map<String, LoggerLevelPayload> loggers,
            Map<String, Object> groups
    ) {
    }

    public record LoggerLevelPayload(String configuredLevel, String effectiveLevel) {
    }
}
