package ua.edu.ukma.candidai.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.interview.event.InterviewReminderEvent;
import ua.edu.ukma.candidai.interview.event.InterviewScheduledEvent;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewNotificationListener {

    private final NotificationDispatcher dispatcher;

    @ApplicationModuleListener
    public void on(InterviewReminderEvent event) {
        String subject = "Upcoming Interview Reminder: " + event.type();
        String body = String.format(
                "Reminder: You have an upcoming interview scheduled at %s. Meeting link: %s",
                event.scheduledAt(),
                event.meetingLink() != null ? event.meetingLink() : "N/A"
        );
        if (event.interviewerId() != null) {
            dispatcher.dispatch(event.interviewerId(), subject, body);
        }
    }

    @ApplicationModuleListener
    public void on(InterviewScheduledEvent event) {
        String subject = "Interview Scheduled: " + event.type();
        String body = String.format(
                "An interview has been scheduled at %s. Duration: %d minutes.",
                event.scheduledAt(),
                event.durationMinutes()
        );
        if (event.interviewerId() != null) {
            dispatcher.dispatch(event.interviewerId(), subject, body);
        }
    }
}
