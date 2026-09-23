package ua.edu.ukma.candidai.notification.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.user.UserApi;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramCommandHandler {

    private static final String START_COMMAND = "/start ";
    private static final String PARSE_MODE_HTML = "HTML";
    private static final String INVALID_UUID_MESSAGE = "Невірний формат ідентифікатора.";
    private static final String USER_NOT_FOUND_MESSAGE = "Користувача з таким ID не знайдено на платформі.";
    private static final String SUCCESS_MESSAGE = "Ваш Telegram успішно прив'язано до CandidAI!";

    private final UserApi userApi;
    private final TelegramBotClient botClient;

    public void handleMessage(String chatId, String text) {
        if (text == null || !text.startsWith(START_COMMAND)) {
            return;
        }

        String payload = text.substring(START_COMMAND.length()).trim();
        UUID userId;
        try {
            userId = UUID.fromString(payload);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in Telegram /start command payload: {}", payload);
            botClient.sendMessage(chatId, INVALID_UUID_MESSAGE, PARSE_MODE_HTML);
            return;
        }

        try {
            userApi.linkTelegramChatId(userId, chatId);
            botClient.sendMessage(chatId, SUCCESS_MESSAGE, PARSE_MODE_HTML);
        } catch (ResourceNotFoundException e) {
            log.warn("User not found when linking Telegram chat id for user ID {}: {}", userId, e.getMessage());
            botClient.sendMessage(chatId, USER_NOT_FOUND_MESSAGE, PARSE_MODE_HTML);
        }
    }
}
