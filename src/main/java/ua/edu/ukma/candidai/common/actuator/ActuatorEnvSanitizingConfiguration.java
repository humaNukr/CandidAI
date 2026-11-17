package ua.edu.ukma.candidai.common.actuator;

import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class ActuatorEnvSanitizingConfiguration {

    private static final List<String> SENSITIVE_KEY_MARKERS = List.of(
            "jwt.secret",
            "spring.datasource.password",
            "telegram.bot.token",
            "telegram.bot-token",
            "gemini.api-key",
            "password",
            "secret",
            "token",
            "api-key",
            "apikey",
            "credential",
            "private-key"
    );

    @Bean
    public SanitizingFunction candidAiSanitizingFunction() {
        return data -> isSensitive(data.getKey())
                ? data.withValue(SanitizableData.SANITIZED_VALUE)
                : data;
    }

    private boolean isSensitive(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT).replace('_', '-');
        return SENSITIVE_KEY_MARKERS.stream().anyMatch(normalizedKey::contains);
    }
}
