package ua.edu.ukma.candidai.notification.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.BLANK_STRING;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.INITIAL_UPDATE_OFFSET;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.NEXT_UPDATE_OFFSET;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.SECOND_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_EMPTY_UPDATES_JSON;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_INVALID_JSON_RESPONSE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_NON_ARRAY_RESULT_JSON;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_START_SECOND_COMMAND;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_START_VALID_COMMAND;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_UPDATES_JSON;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_UPDATES_WITH_BLANK_CHAT_JSON;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.createObjectMapper;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNetworkException;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationProperties;

@ExtendWith(MockitoExtension.class)
class TelegramPollingServiceTest {

    @Mock
    private TelegramBotClient botClient;

    @Mock
    private TelegramCommandHandler commandHandler;

    private TelegramPollingService pollingService;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = sampleNotificationProperties();
        ObjectMapper objectMapper = createObjectMapper();
        pollingService = new TelegramPollingService(properties, botClient, commandHandler, objectMapper);
    }

    @Test
    @DisplayName("pollUpdates should delegate to command handler and advance offset on valid updates")
    void givenValidUpdates_pollUpdates_shouldDelegateToCommandHandlerAndAdvanceOffset() {
        when(botClient.getUpdates(INITIAL_UPDATE_OFFSET)).thenReturn(TELEGRAM_UPDATES_JSON);
        when(botClient.getUpdates(NEXT_UPDATE_OFFSET)).thenReturn(TELEGRAM_EMPTY_UPDATES_JSON);

        pollingService.pollUpdates();

        verify(commandHandler).handleMessage(DEFAULT_TELEGRAM_CHAT_ID, TELEGRAM_START_VALID_COMMAND);
        verify(commandHandler).handleMessage(SECOND_TELEGRAM_CHAT_ID, TELEGRAM_START_SECOND_COMMAND);

        pollingService.pollUpdates();
        verify(botClient).getUpdates(NEXT_UPDATE_OFFSET);
    }

    @Test
    @DisplayName("pollUpdates should ignore updates with blank or missing chat id")
    void givenUpdatesWithBlankChatId_pollUpdates_shouldIgnoreUpdates() {
        when(botClient.getUpdates(INITIAL_UPDATE_OFFSET)).thenReturn(TELEGRAM_UPDATES_WITH_BLANK_CHAT_JSON);

        pollingService.pollUpdates();

        verifyNoInteractions(commandHandler);
    }

    @Test
    @DisplayName("pollUpdates should not process when response is null, blank, or ok=false")
    void givenInvalidResponse_pollUpdates_shouldDoNothing() {
        when(botClient.getUpdates(INITIAL_UPDATE_OFFSET)).thenReturn(
                null,
                BLANK_STRING,
                TELEGRAM_INVALID_JSON_RESPONSE
        );

        pollingService.pollUpdates();
        pollingService.pollUpdates();
        pollingService.pollUpdates();

        verifyNoInteractions(commandHandler);
    }

    @Test
    @DisplayName("pollUpdates should catch exceptions and not throw")
    void givenBotClientThrowsException_pollUpdates_shouldCatchExceptionAndNotThrow() {
        when(botClient.getUpdates(INITIAL_UPDATE_OFFSET)).thenThrow(sampleNetworkException());

        assertThatCode(() -> pollingService.pollUpdates()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("pollUpdates should do nothing when result is not an array")
    void givenNonArrayUpdatesResult_pollUpdates_shouldDoNothing() {
        when(botClient.getUpdates(INITIAL_UPDATE_OFFSET)).thenReturn(TELEGRAM_NON_ARRAY_RESULT_JSON);

        pollingService.pollUpdates();

        verifyNoInteractions(commandHandler);
    }
}
