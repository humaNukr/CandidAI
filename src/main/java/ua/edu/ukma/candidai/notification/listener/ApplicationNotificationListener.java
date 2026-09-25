package ua.edu.ukma.candidai.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.recruitment.event.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationNotificationListener {

    private final NotificationDispatcher dispatcher;

    @ApplicationModuleListener
    public void on(ApplicationSubmittedEvent event) {
        log.info("Processing application submitted notification for candidate: {}", event.email());
        String name = event.candidateName() != null ? event.candidateName() : "Candidate";
        String subject = "Application Received - CandidAI";
        String body = String.format(
                "Hello %s! Thank you for applying. We have received your application and resume. "
                        + "We will notify you once your application status changes.",
                name
        );
        dispatcher.dispatchDirectEmail(name, event.email(), subject, body);
    }

    @ApplicationModuleListener
    public void on(ApplicationStatusChangedEvent event) {
        log.info("Processing application status changed notification for candidate: {} (status: {})",
                event.email(), event.newStatus());
        String name = event.candidateName() != null ? event.candidateName() : "Candidate";
        String subject = "Application Status Update: " + event.newStatus() + " - CandidAI";
        String commentInfo = (event.comment() != null && !event.comment().isBlank())
                ? String.format(" Comment: %s.", event.comment())
                : "";
        String body = String.format(
                "Hello %s! Your application status has been updated from %s to %s.%s",
                name,
                event.previousStatus(),
                event.newStatus(),
                commentInfo
        );
        dispatcher.dispatchDirectEmail(name, event.email(), subject, body);
    }
}
