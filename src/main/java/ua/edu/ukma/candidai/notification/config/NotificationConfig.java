package ua.edu.ukma.candidai.notification.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
@EnableScheduling
@RequiredArgsConstructor
public class NotificationConfig {

    private final NotificationProperties properties;

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.telegram().connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.telegram().readTimeoutMs()));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
