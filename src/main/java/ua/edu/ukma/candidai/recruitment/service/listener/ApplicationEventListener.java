package ua.edu.ukma.candidai.recruitment.service.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.recruitment.event.ApplicationStatusChangedEvent;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;

@Slf4j
@Component
public class ApplicationEventListener {

    @ApplicationModuleListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Async event received: Application {} submitted for vacancy {} by email {}",
                event.applicationId(), event.vacancyId(), event.email());
    }

    @ApplicationModuleListener
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("Async event received: Application {} status changed from {} to {} (comment: '{}')",
                event.applicationId(), event.previousStatus(), event.newStatus(), event.comment());
    }
}
