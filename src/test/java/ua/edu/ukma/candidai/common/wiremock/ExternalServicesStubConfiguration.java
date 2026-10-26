package ua.edu.ukma.candidai.common.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Заглушка зовнішніх систем Google Gemini AI та Telegram Bot на WireMock.
 * Справжній HTTP-сервер на випадковому порті: застосунок виконує мережеві виклики
 * як у продакшні, а тестовий контекст перевіряє передані заголовки й тіла запитів.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ExternalServicesStubConfiguration {

    public static final String GEMINI_MODEL = "gemini-3.5-flash-lite";
    public static final String GEMINI_API_KEY = "wiremock-test-gemini-key";
    public static final String GEMINI_PATH = "/v1beta/models/" + GEMINI_MODEL + ":generateContent";

    public static final String TELEGRAM_BOT_TOKEN = "test-bot-token";
    public static final String TELEGRAM_PATH = "/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";

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
            registry.add("candidai.gemini.base-url", wireMockServer::baseUrl);
            registry.add("candidai.gemini.api-key", () -> GEMINI_API_KEY);
            registry.add("candidai.gemini.model", () -> GEMINI_MODEL);
            registry.add("notification.telegram.api-url", wireMockServer::baseUrl);
            registry.add("notification.telegram.bot-token", () -> TELEGRAM_BOT_TOKEN);
            registry.add("notification.telegram.enabled", () -> "true");
        };
    }

    public static void setupDefaultStubs(WireMockServer server) {
        stubGeminiSuccess(server, 88, true, "Strong background in Java and Spring Boot.");
        stubTelegramSuccess(server);
    }

    public static void stubGeminiSuccess(
            WireMockServer server,
            int score,
            boolean passed,
            String summary
    ) {
        String payloadJson = """
                {
                  "matchingScore": %d,
                  "passed": %s,
                  "summary": "%s",
                  "strengths": ["Java 25", "Spring Boot", "PostgreSQL"],
                  "weaknesses": ["Limited Kubernetes"],
                  "suggestedQuestions": ["Explain transaction isolation"]
                }
                """.formatted(score, passed, summary);

        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(quoteJsonString(payloadJson));

        server.stubFor(post(urlEqualTo(GEMINI_PATH))
                .withHeader("x-goog-api-key", equalTo(GEMINI_API_KEY))
                .willReturn(okJson(geminiResponseBody)));
    }

    public static void stubGeminiError(WireMockServer server, int statusCode) {
        server.stubFor(post(urlEqualTo(GEMINI_PATH))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "error": { "code": %d, "message": "Service unavailable" } }
                                """.formatted(statusCode))));
    }

    public static void stubTelegramSuccess(WireMockServer server) {
        server.stubFor(post(urlEqualTo(TELEGRAM_PATH))
                .willReturn(okJson("""
                        { "ok": true, "result": { "message_id": 1001 } }
                        """)));
    }

    private static String quoteJsonString(String raw) {
        return "\"" + raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}
