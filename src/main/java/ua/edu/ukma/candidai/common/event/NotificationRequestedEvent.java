package ua.edu.ukma.candidai.common.event;

import java.util.UUID;

public record NotificationRequestedEvent(
        UUID recipientId,
        String subject,
        String body
) {}
