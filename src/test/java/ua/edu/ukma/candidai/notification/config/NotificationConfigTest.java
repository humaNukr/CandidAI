package ua.edu.ukma.candidai.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationConfig;

class NotificationConfigTest {

    @Test
    @DisplayName("restClient should build RestClient bean from configuration")
    void givenNotificationConfig_restClient_shouldReturnNonNullRestClient() {
        NotificationConfig config = sampleNotificationConfig();

        RestClient restClient = config.restClient();

        assertThat(restClient).isNotNull();
    }
}
