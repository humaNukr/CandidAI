package ua.edu.ukma.candidai.notification.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_MESSAGE_TEXT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_UPDATE_OFFSET;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.PARSE_MODE_HTML;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.TELEGRAM_EMPTY_UPDATES_JSON;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedTelegramGetUpdatesUrl;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedTelegramSendMessageUrl;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationProperties;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationPropertiesWithTrailingSlash;

class TelegramBotClientTest {

    private TelegramBotClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = sampleNotificationProperties();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramBotClient(builder.build(), properties);
    }

    @Test
    @DisplayName("getUpdates should perform GET request and return body string")
    void givenOffset_getUpdates_shouldPerformGetRequestAndReturnBody() {
        server.expect(requestTo(expectedTelegramGetUpdatesUrl(DEFAULT_UPDATE_OFFSET)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TELEGRAM_EMPTY_UPDATES_JSON, MediaType.APPLICATION_JSON));

        String actualResponse = client.getUpdates(DEFAULT_UPDATE_OFFSET);

        assertThat(actualResponse).isEqualTo(TELEGRAM_EMPTY_UPDATES_JSON);
        server.verify();
    }

    @Test
    @DisplayName("sendMessage should perform POST request with JSON payload")
    void givenChatIdAndMessage_sendMessage_shouldPerformPostRequestWithJsonPayload() {
        server.expect(requestTo(expectedTelegramSendMessageUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chat_id").value(DEFAULT_TELEGRAM_CHAT_ID))
                .andExpect(jsonPath("$.text").value(DEFAULT_MESSAGE_TEXT))
                .andExpect(jsonPath("$.parse_mode").value(PARSE_MODE_HTML))
                .andRespond(withSuccess());

        client.sendMessage(DEFAULT_TELEGRAM_CHAT_ID, DEFAULT_MESSAGE_TEXT, PARSE_MODE_HTML);

        server.verify();
    }

    @Test
    @DisplayName("getUpdates should normalize api url with trailing slash")
    void givenApiUrlWithTrailingSlash_getUpdates_shouldNormalizeUrl() {
        NotificationProperties properties = sampleNotificationPropertiesWithTrailingSlash();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer customServer = MockRestServiceServer.bindTo(builder).build();
        TelegramBotClient customClient = new TelegramBotClient(builder.build(), properties);

        customServer.expect(requestTo(expectedTelegramGetUpdatesUrl(DEFAULT_UPDATE_OFFSET)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TELEGRAM_EMPTY_UPDATES_JSON, MediaType.APPLICATION_JSON));

        String actualResponse = customClient.getUpdates(DEFAULT_UPDATE_OFFSET);

        assertThat(actualResponse).isEqualTo(TELEGRAM_EMPTY_UPDATES_JSON);
        customServer.verify();
    }
}
