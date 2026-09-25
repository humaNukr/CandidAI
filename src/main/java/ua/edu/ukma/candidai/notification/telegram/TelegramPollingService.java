package ua.edu.ukma.candidai.notification.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

@Service
@ConditionalOnProperty(prefix = "notification.telegram", name = "enabled", havingValue = "true")
@Slf4j
public class TelegramPollingService {

    private final NotificationProperties properties;
    private final TelegramBotClient botClient;
    private final TelegramCommandHandler commandHandler;
    private final ObjectMapper objectMapper;
    private final TelegramOffsetHolder offsetHolder;

    public TelegramPollingService(
            NotificationProperties properties,
            TelegramBotClient botClient,
            TelegramCommandHandler commandHandler,
            ObjectMapper objectMapper
    ) {
        this(properties, botClient, commandHandler, objectMapper, new TelegramOffsetHolder());
    }

    @Autowired
    public TelegramPollingService(
            NotificationProperties properties,
            TelegramBotClient botClient,
            TelegramCommandHandler commandHandler,
            ObjectMapper objectMapper,
            TelegramOffsetHolder offsetHolder
    ) {
        this.properties = properties;
        this.botClient = botClient;
        this.commandHandler = commandHandler;
        this.objectMapper = objectMapper;
        this.offsetHolder = offsetHolder;
    }

    @Scheduled(fixedDelayString = "${notification.telegram.polling-interval-ms:5000}")
    public void pollUpdates() {
        try {
            String response = botClient.getUpdates(offsetHolder.getOffset());
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
            offsetHolder.setOffset(update.path("update_id").asLong() + 1);

            JsonNode message = update.path("message");
            String chatId = message.path("chat").path("id").asText();
            String text = message.path("text").asText("");
            if (!chatId.isBlank()) {
                commandHandler.handleMessage(chatId, text);
            }
        }
    }
}
