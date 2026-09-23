package ua.edu.ukma.candidai.notification.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.user.UserApi;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_RECIPIENT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.PARSE_MODE_HTML;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_HELP_COMMAND;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_INVALID_UUID_MESSAGE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_START_INVALID_UUID_COMMAND;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_START_VALID_COMMAND;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_START_WITHOUT_PAYLOAD_COMMAND;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_SUCCESS_MESSAGE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_USER_NOT_FOUND_MESSAGE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class TelegramCommandHandlerTest {

    @Mock
    private UserApi userApi;

    @Mock
    private TelegramBotClient botClient;

    private TelegramCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TelegramCommandHandler(userApi, botClient);
    }

    @Test
    @DisplayName("handleMessage should link chat id and send success message when valid UUID and existing user")
    void givenValidUuidAndExistingUser_handleMessage_shouldLinkChatIdAndSendSuccessMessage() {
        handler.handleMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_START_VALID_COMMAND);

        verify(userApi).linkTelegramChatId(DEFAULT_RECIPIENT_ID, DEFAULT_TELEGRAM_CHAT_ID);
        verify(botClient).sendMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_SUCCESS_MESSAGE, PARSE_MODE_HTML);
    }

    @Test
    @DisplayName("handleMessage should send not-found message when user does not exist")
    void givenNonExistentUser_handleMessage_shouldSendNotFoundMessage() {
        doThrow(sampleResourceNotFoundException())
                .when(userApi).linkTelegramChatId(DEFAULT_RECIPIENT_ID, DEFAULT_TELEGRAM_CHAT_ID);

        handler.handleMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_START_VALID_COMMAND);

        verify(userApi).linkTelegramChatId(DEFAULT_RECIPIENT_ID, DEFAULT_TELEGRAM_CHAT_ID);
        verify(botClient).sendMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_USER_NOT_FOUND_MESSAGE, PARSE_MODE_HTML);
    }

    @Test
    @DisplayName("handleMessage should send invalid format message when UUID is invalid")
    void givenInvalidUuid_handleMessage_shouldSendInvalidFormatMessage() {
        handler.handleMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_START_INVALID_UUID_COMMAND);

        verifyNoInteractions(userApi);
        verify(botClient).sendMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_INVALID_UUID_MESSAGE, PARSE_MODE_HTML);
    }

    @Test
    @DisplayName("handleMessage should do nothing when text is null")
    void givenNullText_handleMessage_shouldDoNothing() {
        handler.handleMessage(DEFAULT_TELEGRAM_CHAT_ID, null);

        verifyNoInteractions(userApi, botClient);
    }

    @Test
    @DisplayName("handleMessage should do nothing when text does not start with '/start '")
    void givenNonStartCommand_handleMessage_shouldDoNothing() {
        handler.handleMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_HELP_COMMAND);

        verifyNoInteractions(userApi, botClient);
    }

    @Test
    @DisplayName("handleMessage should do nothing when command has no payload")
    void givenStartCommandWithoutPayload_handleMessage_shouldDoNothing() {
        handler.handleMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_START_WITHOUT_PAYLOAD_COMMAND);

        verifyNoInteractions(userApi, botClient);
    }
}
