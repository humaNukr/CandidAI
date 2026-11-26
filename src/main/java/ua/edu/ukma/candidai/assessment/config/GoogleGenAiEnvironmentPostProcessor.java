package ua.edu.ukma.candidai.assessment.config;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class GoogleGenAiEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String CHAT_MODEL_PROPERTY = "spring.ai.model.chat";
    private static final String API_KEY_PROPERTY = "spring.ai.google.genai.api-key";
    private static final String VERTEX_AI_PROPERTY = "spring.ai.google.genai.vertex-ai";
    private static final String PROJECT_ID_PROPERTY = "spring.ai.google.genai.project-id";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StringUtils.hasText(environment.getProperty(CHAT_MODEL_PROPERTY))) {
            return;
        }
        boolean hasApiKey = StringUtils.hasText(environment.getProperty(API_KEY_PROPERTY));
        boolean vertexEnabled = Boolean.parseBoolean(environment.getProperty(VERTEX_AI_PROPERTY, "false"))
                && StringUtils.hasText(environment.getProperty(PROJECT_ID_PROPERTY));

        if (!hasApiKey && !vertexEnabled) {
            environment.getPropertySources().addLast(
                    new MapPropertySource("googleGenAiDisabledFallback", Map.of(CHAT_MODEL_PROPERTY, "none"))
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
