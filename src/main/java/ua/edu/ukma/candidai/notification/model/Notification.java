package ua.edu.ukma.candidai.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @EqualsAndHashCode.Include
    private UUID id;

    private UUID recipientId;

    private String recipientEmail;

    private String recipientTelegramChatId;

    private NotificationChannel channel;

    private String subject;

    private String content;

    private NotificationDeliveryStatus status;

    private String errorMessage;

    private Instant createdAt;

    private Instant sentAt;
}
