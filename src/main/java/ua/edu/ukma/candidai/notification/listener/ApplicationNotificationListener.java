package ua.edu.ukma.candidai.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.recruitment.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.ApplicationSubmittedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationNotificationListener {

    private final NotificationDispatcher dispatcher;

    @ApplicationModuleListener
    public void on(ApplicationSubmittedEvent event) {
        String subject = "Application received: " + event.candidateName();
        String body = "Hello " + event.candidateName() + ", your application has been successfully submitted.";
        dispatcher.dispatch(event.candidateId(), subject, body);
    }

    @ApplicationModuleListener
    public void on(ApplicationStatusChangedEvent event) {
        String subject = "Application status updated: " + event.newStatus();
        String body = String.format(
                "Your application status has been changed from %s to %s.",
                event.previousStatus(),
                event.newStatus()
        );
        dispatcher.dispatch(event.candidateId(), subject, body);
    }
}
