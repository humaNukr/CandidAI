package ua.edu.ukma.candidai.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.common.event.NotificationRequestedEvent;
import ua.edu.ukma.candidai.notification.model.NotificationMessage;
import ua.edu.ukma.candidai.notification.service.NotificationDispatcher;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestedEventListener {

    private final NotificationDispatcher dispatcher;

    @ApplicationModuleListener
    public void on(NotificationRequestedEvent event) {
        NotificationMessage message = NotificationMessage.builder()
                .recipientId(event.recipientId())
                .subject(event.subject())
                .body(event.body())
                .build();
        dispatcher.dispatch(message);
    }
}
