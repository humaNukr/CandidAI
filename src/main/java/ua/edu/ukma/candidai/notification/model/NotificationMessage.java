package ua.edu.ukma.candidai.notification.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationMessage(
        UUID recipientId,
        String subject,
        String body
) {}
