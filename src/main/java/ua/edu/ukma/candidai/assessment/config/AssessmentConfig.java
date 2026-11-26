package ua.edu.ukma.candidai.assessment.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AssessmentConfig {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a senior technical recruiter and hiring manager conducting automated candidate screening.
            Evaluate candidates objectively against job requirements and always respond in the requested format.
            """;

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel unavailableChatModel() {
        return prompt -> {
            throw new IllegalStateException("Gemini API key is not configured. AI screening is unavailable.");
        };
    }

    @Bean
    public ChatClient screeningChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }
}
