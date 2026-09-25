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
        String subject = "Application submitted: " + event.candidateName();
        String body = String.format(
                "Hello %s, your application has been successfully submitted and is under review.",
                event.candidateName()
        );
        dispatcher.dispatchDirect(event.email(), null, event.candidateName(), subject, body);
    }

    @ApplicationModuleListener
    public void on(ApplicationStatusChangedEvent event) {
        String subject = "Updated application status: " + event.newStatus();
        String commentPart = (event.comment() != null && !event.comment().isBlank())
                ? "\nComment: " + event.comment()
                : "";
        String body = String.format(
                "Your application status was changed from %s to %s.%s",
                event.previousStatus(), event.newStatus(), commentPart
        );
        dispatcher.dispatchDirect(event.email(), null, "Candidate", subject, body);
    }
}
