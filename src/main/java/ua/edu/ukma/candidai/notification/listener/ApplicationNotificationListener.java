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
        String subject = "CandidAI: Вашу заявку успішно зареєстровано";
        String body = String.format(
                "Шановний(-а) %s! Дякуємо за ваш відгук на вакансію. "
                        + "Ваша заявка успішно передана на первинний скринінг.",
                event.candidateName()
        );
        dispatcher.dispatchDirect(event.email(), null, event.candidateName(), subject, body);
    }

    @ApplicationModuleListener
    public void on(ApplicationStatusChangedEvent event) {
        String subject = "CandidAI: Оновлення статусу вашої заявки";
        String commentPart = (event.comment() != null && !event.comment().isBlank())
                ? "\nКоментар: " + event.comment()
                : "";
        String body = String.format(
                "Статус вашої заявки змінено з %s на %s.%s",
                event.previousStatus(), event.newStatus(), commentPart
        );
        dispatcher.dispatchDirect(event.email(), null, "Кандидат", subject, body);
    }
}
