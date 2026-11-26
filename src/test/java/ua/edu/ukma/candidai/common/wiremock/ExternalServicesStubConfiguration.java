package ua.edu.ukma.candidai.common.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@TestConfiguration(proxyBeanMethods = false)
public class ExternalServicesStubConfiguration {

    public static final String TEST_SPRING_AI_KEY = "test-spring-ai-key";
    public static final String TELEGRAM_BOT_TOKEN = "test-bot-token";
    public static final String TELEGRAM_PATH = "/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";

    private static final int DEFAULT_SCORE = 88;
    private static final String DEFAULT_SUMMARY = "Strong background in Java and Spring Boot.";

    @Bean(destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        setupDefaultStubs(server);
        return server;
    }

    @Bean
    public DynamicPropertyRegistrar externalServicesProperties(WireMockServer wireMockServer) {
        return registry -> {
            registry.add("spring.ai.model.chat", () -> "none");
            registry.add("spring.ai.google.genai.api-key", () -> TEST_SPRING_AI_KEY);
            registry.add("notification.telegram.api-url", wireMockServer::baseUrl);
            registry.add("notification.telegram.bot-token", () -> TELEGRAM_BOT_TOKEN);
            registry.add("notification.telegram.enabled", () -> "true");
        };
    }

    @Bean
    @Primary
    public TestStubChatModel stubChatModel() {
        return new TestStubChatModel();
    }

    public static void setupDefaultStubs(WireMockServer server) {
        stubTelegramSuccess(server);
    }

    public static void stubTelegramSuccess(WireMockServer server) {
        server.stubFor(post(urlEqualTo(TELEGRAM_PATH))
                .willReturn(okJson("""
                        { "ok": true, "result": { "message_id": 1001 } }
                        """)));
    }

    public static class TestStubChatModel implements ChatModel {

        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicReference<String> nextPayload = new AtomicReference<>(defaultJson());
        private final AtomicReference<RuntimeException> nextException = new AtomicReference<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            callCount.incrementAndGet();
            RuntimeException exception = nextException.get();
            if (exception != null) {
                throw exception;
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(nextPayload.get()))));
        }

        public void stubSuccess(int score, boolean passed, String summary) {
            nextException.set(null);
            nextPayload.set(formatJson(score, passed, summary));
        }

        public void stubError(RuntimeException exception) {
            nextException.set(exception);
        }

        public void resetToDefault() {
            callCount.set(0);
            nextException.set(null);
            nextPayload.set(defaultJson());
        }

        public int getCallCount() {
            return callCount.get();
        }

        private static String defaultJson() {
            return formatJson(DEFAULT_SCORE, true, DEFAULT_SUMMARY);
        }

        private static String formatJson(int score, boolean passed, String summary) {
            return """
                    {
                      "matchingScore": %d,
                      "passed": %s,
                      "summary": "%s",
                      "strengths": ["Java 25", "Spring Boot", "PostgreSQL"],
                      "weaknesses": ["Limited Kubernetes"],
                      "suggestedQuestions": ["Explain transaction isolation"]
                    }
                    """.formatted(score, passed, summary);
        }
    }
}
