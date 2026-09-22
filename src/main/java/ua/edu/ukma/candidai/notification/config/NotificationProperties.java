package ua.edu.ukma.candidai.notification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        @Valid @NotNull MailProperties mail,
        @Valid @NotNull TelegramProperties telegram
) {
    public record MailProperties(
            @NotBlank String templateName,
            @NotBlank String defaultActionUrl,
            @NotBlank String defaultRecipientName
    ) {}

    public record TelegramProperties(
            boolean enabled,
            @NotNull String botToken,
            @NotBlank String apiUrl,
            long pollingIntervalMs,
            long connectTimeoutMs,
            long readTimeoutMs
    ) {}
}
