package ua.edu.ukma.candidai.common.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CandidAiInfoContributor implements InfoContributor {

    private final Environment environment;

    public CandidAiInfoContributor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void contribute(Info.Builder builder) {
        String geminiApiKey = environment.getProperty("candidai.gemini.api-key", "");
        boolean aiScreeningConfigured = !geminiApiKey.isBlank();
        boolean mailEnabled = Boolean.parseBoolean(
                environment.getProperty("notification.mail.enabled", "false")
        );
        boolean telegramEnabled = Boolean.parseBoolean(
                environment.getProperty("notification.telegram.enabled", "false")
        );

        builder.withDetail("application", Map.of(
                "name", "CandidAI",
                "description", "Smart HR & AI Candidate Screening Platform"
        ));
        builder.withDetail("features", Map.of(
                "mailNotificationsEnabled", mailEnabled,
                "telegramBotEnabled", telegramEnabled,
                "aiScreeningConfigured", aiScreeningConfigured
        ));
    }
}
