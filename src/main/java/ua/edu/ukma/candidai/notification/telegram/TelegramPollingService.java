package ua.edu.ukma.candidai.notification.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

@Service
@ConditionalOnProperty(prefix = "notification.telegram", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TelegramPollingService {

    private final NotificationProperties properties;
    private final TelegramBotClient botClient;
    private final TelegramCommandHandler commandHandler;
    private final ObjectMapper objectMapper;

    private long offset;

    @Scheduled(fixedDelayString = "${notification.telegram.polling-interval-ms:5000}")
    public void pollUpdates() {
        try {
            String response = botClient.getUpdates(offset);
            if (response == null || response.isBlank()) {
                return;
            }

            JsonNode root = objectMapper.readTree(response);
            if (root.path("ok").asBoolean()) {
                processUpdates(root.path("result"));
            }
        } catch (Exception e) {
            log.error("Failed to poll Telegram updates: {}", e.getMessage(), e);
        }
    }

    private void processUpdates(JsonNode updates) {
        if (!updates.isArray()) {
            return;
        }
        for (JsonNode update : updates) {
            this.offset = update.path("update_id").asLong() + 1;

            JsonNode message = update.path("message");
            String chatId = message.path("chat").path("id").asText();
            String text = message.path("text").asText("");
            if (!chatId.isBlank()) {
                commandHandler.handleMessage(chatId, text);
            }
        }
    }
}
