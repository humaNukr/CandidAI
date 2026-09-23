package ua.edu.ukma.candidai.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;
import ua.edu.ukma.candidai.vacancy.VacancyStatusChangedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class VacancyNotificationListener {

    private final NotificationDispatcher dispatcher;

    @ApplicationModuleListener
    public void on(VacancyStatusChangedEvent event) {
        String subject = "Updated vacancy status: " + event.vacancyTitle();
        String body = String.format(
                "Your vacancy '%s' status was changed from %s to %s.",
                event.vacancyTitle(), event.oldStatus(), event.newStatus()
        );
        dispatcher.dispatch(event.authorId(), subject, body);
    }
}
