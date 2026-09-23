package ua.edu.ukma.candidai.notification.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotClient {

    private final RestClient restClient;
    private final NotificationProperties properties;

    public String getUpdates(long offset) {
        if (!properties.telegram().enabled()) {
            return null;
        }
        return restClient.get()
                .uri(buildBotUrl("getUpdates?offset=" + offset + "&timeout=0"))
                .retrieve()
                .body(String.class);
    }

    public void sendMessage(String chatId, String text, String parseMode) {
        if (!properties.telegram().enabled()) {
            log.info("[TELEGRAM-DEV] Telegram bot is disabled (notification.telegram.enabled=false). "
                    + "Simulated message to chat {}: {}", chatId, text);
            return;
        }

        Map<String, String> requestBody = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", parseMode
        );

        restClient.post()
                .uri(buildBotUrl("sendMessage"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    private String buildBotUrl(String path) {
        String apiUrl = properties.telegram().apiUrl().replaceAll("/+$", "");
        return apiUrl + "/bot" + properties.telegram().botToken() + "/" + path;
    }
}
