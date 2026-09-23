package ua.edu.ukma.candidai.notification.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

import java.time.Year;

@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final SpringTemplateEngine templateEngine;
    private final NotificationProperties properties;

    public String render(String recipientName, String subject, String body) {
        Context context = new Context();
        String name = (recipientName != null && !recipientName.isBlank())
                ? recipientName
                : properties.mail().defaultRecipientName();

        context.setVariable("recipientName", name);
        context.setVariable("subject", subject);
        context.setVariable("body", body);
        context.setVariable("actionUrl", properties.mail().defaultActionUrl());
        context.setVariable("year", Year.now().getValue());

        return templateEngine.process(properties.mail().templateName(), context);
    }
}
