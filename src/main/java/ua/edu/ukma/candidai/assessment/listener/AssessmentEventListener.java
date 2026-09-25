package ua.edu.ukma.candidai.assessment.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;
import ua.edu.ukma.candidai.recruitment.ApplicationSubmittedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssessmentEventListener {

    private final AssessmentService assessmentService;

    @ApplicationModuleListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Async event received: Application {} submitted for vacancy {}. Running screening.",
                event.applicationId(), event.vacancyId());
        try {
            assessmentService.executeScreening(event.applicationId());
        } catch (Exception ex) {
            log.error("Failed to execute screening for application {}: {}",
                    event.applicationId(), ex.getMessage(), ex);
        }
    }
}
