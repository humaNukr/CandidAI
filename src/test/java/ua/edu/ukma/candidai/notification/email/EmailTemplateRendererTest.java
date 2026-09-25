package ua.edu.ukma.candidai.notification.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.BLANK_STRING;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.CANDIDAI_BRAND_NAME;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.CANDIDAI_TAGLINE;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_ACTION_URL;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_BODY;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_FULL_NAME;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.DEFAULT_SUBJECT;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.createTemplateEngine;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.currentYearString;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedHtmlDefaultGreeting;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.expectedHtmlGreetingWithName;
import static ua.edu.ukma.candidai.notification.NotificationTestResources.sampleNotificationProperties;

class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine templateEngine = createTemplateEngine();
        NotificationProperties properties = sampleNotificationProperties();
        renderer = new EmailTemplateRenderer(templateEngine, properties);
    }

    @Test
    @DisplayName("render should generate HTML containing provided recipient name and details")
    void givenRecipientNameAndDetails_render_shouldGenerateHtmlWithProvidedName() {
        String html = renderer.render(DEFAULT_FULL_NAME, DEFAULT_SUBJECT, DEFAULT_BODY);

        assertThat(html)
                .contains(expectedHtmlGreetingWithName(DEFAULT_FULL_NAME))
                .contains(DEFAULT_SUBJECT)
                .contains(DEFAULT_BODY)
                .contains(DEFAULT_ACTION_URL)
                .contains(currentYearString())
                .contains(CANDIDAI_BRAND_NAME)
                .contains(CANDIDAI_TAGLINE);
    }

    @Test
    @DisplayName("render should fallback to default recipient name when recipient name is blank")
    void givenBlankRecipientName_render_shouldFallbackToDefaultRecipientName() {
        String html = renderer.render(BLANK_STRING, DEFAULT_SUBJECT, DEFAULT_BODY);

        assertThat(html).contains(expectedHtmlDefaultGreeting());
    }

    @Test
    @DisplayName("render should fallback to default recipient name when recipient name is null")
    void givenNullRecipientName_render_shouldFallbackToDefaultRecipientName() {
        String html = renderer.render(null, DEFAULT_SUBJECT, DEFAULT_BODY);

        assertThat(html).contains(expectedHtmlDefaultGreeting());
    }
}
