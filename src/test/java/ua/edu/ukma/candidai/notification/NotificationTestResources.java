package ua.edu.ukma.candidai.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.notification.config.NotificationConfig;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;
import ua.edu.ukma.candidai.notification.model.Notification;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.notification.model.NotificationDeliveryStatus;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.VacancyStatusChangedEvent;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;

public final class NotificationTestResources {

    public static final String CANDIDAI_BRAND_NAME = "CandidAI";
    public static final String CANDIDAI_TAGLINE = "Recruitment Platform";
    public static final String NOTIFICATION_OR_ID_NULL_MESSAGE = "Notification and its id must not be null";
    public static final String USER_NOT_FOUND_MESSAGE = "User not found";

    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final UUID DEFAULT_RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID OTHER_RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000088");
    public static final UUID DEFAULT_VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    public static final String DEFAULT_FULL_NAME = "John Doe";
    public static final String DEFAULT_EMAIL = "candidate@example.com";
    public static final String DEFAULT_TELEGRAM_CHAT_ID = "123456789";
    public static final String SECOND_TELEGRAM_CHAT_ID = "112233";

    public static final String DEFAULT_VACANCY_TITLE = "Senior Java Engineer";
    public static final String DEFAULT_SUBJECT = "Статус вашої вакансії змінено";
    public static final String DEFAULT_BODY = "Your interview for Senior Java Engineer has been scheduled.";

    public static final String DEFAULT_TEMPLATE_NAME = "mail/notification-email";
    public static final String DEFAULT_ACTION_URL = "https://candidai.ukma.edu.ua";
    public static final String DEFAULT_RECIPIENT_NAME = "there";

    public static final String DEFAULT_BOT_TOKEN = "test-token";
    public static final String DEFAULT_API_URL = "https://api.telegram.org";
    public static final long DEFAULT_POLLING_INTERVAL_MS = 5000L;
    public static final long DEFAULT_CONNECT_TIMEOUT_MS = 5000L;
    public static final long DEFAULT_READ_TIMEOUT_MS = 10000L;

    public static final String DEFAULT_HTML_CONTENT = "<html>Rendered Content</html>";
    public static final String PARSE_MODE_HTML = "HTML";
    public static final String TELEGRAM_SUCCESS_MESSAGE = "Ваш Telegram успішно прив'язано до CandidAI!";
    public static final String TELEGRAM_USER_NOT_FOUND_MESSAGE = "Користувача з таким ID не знайдено на платформі.";
    public static final String TELEGRAM_INVALID_UUID_MESSAGE = "Невірний формат ідентифікатора.";

    public static final Instant DEFAULT_NOW = Instant.parse("2026-09-20T10:00:00Z");
    public static final Instant SENT_AT = Instant.parse("2026-09-20T10:00:05Z");
    public static final String DEFAULT_ERROR_MESSAGE = "SMTP connection timeout";

    public static final long INITIAL_UPDATE_OFFSET = 0L;
    public static final long NEXT_UPDATE_OFFSET = 102L;
    public static final long DEFAULT_UPDATE_OFFSET = 123L;
    public static final String DEFAULT_MESSAGE_TEXT = "Hello World";
    public static final String TELEGRAM_INVALID_JSON_RESPONSE = "{\"ok\": false}";
    public static final String TELEGRAM_NON_ARRAY_RESULT_JSON = "{\"ok\": true, \"result\": {}}";
    public static final String TELEGRAM_START_VALID_COMMAND = "/start " + DEFAULT_RECIPIENT_ID;
    public static final String TELEGRAM_START_SECOND_COMMAND = "/start " + SECOND_ID;
    public static final String TELEGRAM_START_INVALID_UUID_COMMAND = "/start not-a-valid-uuid";
    public static final String TELEGRAM_START_WITHOUT_PAYLOAD_COMMAND = "/start";
    public static final String TELEGRAM_HELP_COMMAND = "/help";

    public static final String HTML_ESCAPED_SUBJECT = "Status <update> & alerts";
    public static final String HTML_ESCAPED_BODY = "Body with <tags> & 'quotes'";
    public static final String HTML_ESCAPED_FORMATTED_TEXT = "<b>Status &lt;update&gt; &amp; alerts</b>\n\n"
            + "Body with &lt;tags&gt; &amp; &#39;quotes&#39;";

    public static final String BLANK_STRING = "   ";

    public static final String TELEGRAM_UPDATES_JSON = """
            {
              "ok": true,
              "result": [
                {
                  "update_id": 100,
                  "message": {
                    "chat": { "id": "123456789" },
                    "text": "/start 00000000-0000-0000-0000-000000000002"
                  }
                },
                {
                  "update_id": 101,
                  "message": {
                    "chat": { "id": "112233" },
                    "text": "/start 00000000-0000-0000-0000-000000000099"
                  }
                }
              ]
            }
            """;

    public static final String TELEGRAM_UPDATES_WITH_BLANK_CHAT_JSON = """
            {
              "ok": true,
              "result": [
                {
                  "update_id": 50,
                  "message": {
                    "text": "/start something"
                  }
                }
              ]
            }
            """;

    public static final String TELEGRAM_EMPTY_UPDATES_JSON = "{\"ok\": true, \"result\": []}";

    private NotificationTestResources() {
    }

    public static NotificationProperties.MailProperties sampleMailProperties() {
        return new NotificationProperties.MailProperties(
                true,
                DEFAULT_TEMPLATE_NAME,
                DEFAULT_ACTION_URL,
                DEFAULT_RECIPIENT_NAME
        );
    }

    public static NotificationProperties.MailProperties sampleDisabledMailProperties() {
        return new NotificationProperties.MailProperties(
                false,
                DEFAULT_TEMPLATE_NAME,
                DEFAULT_ACTION_URL,
                DEFAULT_RECIPIENT_NAME
        );
    }

    public static NotificationProperties.TelegramProperties sampleTelegramProperties() {
        return new NotificationProperties.TelegramProperties(
                true,
                DEFAULT_BOT_TOKEN,
                DEFAULT_API_URL,
                DEFAULT_POLLING_INTERVAL_MS,
                DEFAULT_CONNECT_TIMEOUT_MS,
                DEFAULT_READ_TIMEOUT_MS
        );
    }

    public static NotificationProperties.TelegramProperties sampleDisabledTelegramProperties() {
        return new NotificationProperties.TelegramProperties(
                false,
                DEFAULT_BOT_TOKEN,
                DEFAULT_API_URL,
                DEFAULT_POLLING_INTERVAL_MS,
                DEFAULT_CONNECT_TIMEOUT_MS,
                DEFAULT_READ_TIMEOUT_MS
        );
    }

    public static NotificationProperties sampleNotificationProperties() {
        return new NotificationProperties(
                sampleMailProperties(),
                sampleTelegramProperties()
        );
    }

    public static NotificationProperties sampleNotificationPropertiesWithTrailingSlash() {
        return new NotificationProperties(
                sampleMailProperties(),
                new NotificationProperties.TelegramProperties(
                        true,
                        DEFAULT_BOT_TOKEN,
                        DEFAULT_API_URL + "/",
                        DEFAULT_POLLING_INTERVAL_MS,
                        DEFAULT_CONNECT_TIMEOUT_MS,
                        DEFAULT_READ_TIMEOUT_MS
                )
        );
    }

    public static VacancyStatusChangedEvent sampleVacancyStatusChangedEvent() {
        return new VacancyStatusChangedEvent(
                DEFAULT_VACANCY_ID,
                DEFAULT_VACANCY_TITLE,
                DEFAULT_RECIPIENT_ID,
                VacancyStatus.OPEN,
                VacancyStatus.CLOSED,
                DEFAULT_NOW
        );
    }
    public static UserNotificationProfile sampleUserNotificationProfile() {
        return new UserNotificationProfile(
                DEFAULT_RECIPIENT_ID,
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_TELEGRAM_CHAT_ID
        );
    }

    public static UserNotificationProfile userProfileWithNullEmail() {
        return new UserNotificationProfile(
                DEFAULT_RECIPIENT_ID,
                DEFAULT_FULL_NAME,
                null,
                null
        );
    }

    public static UserNotificationProfile userProfileWithBlankEmail() {
        return new UserNotificationProfile(
                DEFAULT_RECIPIENT_ID,
                DEFAULT_FULL_NAME,
                BLANK_STRING,
                null
        );
    }

    public static UserNotificationProfile userProfileWithNullTelegramChatId() {
        return new UserNotificationProfile(
                DEFAULT_RECIPIENT_ID,
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                null
        );
    }

    public static UserNotificationProfile userProfileWithBlankTelegramChatId() {
        return new UserNotificationProfile(
                DEFAULT_RECIPIENT_ID,
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                BLANK_STRING
        );
    }

    public static Notification samplePendingNotification() {
        return Notification.pending(
                DEFAULT_ID,
                DEFAULT_RECIPIENT_ID,
                DEFAULT_EMAIL,
                DEFAULT_TELEGRAM_CHAT_ID,
                NotificationChannel.EMAIL,
                DEFAULT_SUBJECT,
                DEFAULT_BODY,
                DEFAULT_NOW
        );
    }

    public static Notification sampleSentNotification() {
        return sampleSentNotification(SENT_AT);
    }

    public static Notification sampleSentNotification(Instant sentAt) {
        return sampleNotification(NotificationChannel.EMAIL, NotificationDeliveryStatus.SENT, null, sentAt);
    }

    public static Notification sampleFailedNotification() {
        return sampleFailedNotification(DEFAULT_ERROR_MESSAGE);
    }

    public static Notification sampleFailedNotification(String errorMessage) {
        return sampleNotification(NotificationChannel.EMAIL, NotificationDeliveryStatus.FAILED, errorMessage, null);
    }

    public static Notification sampleNotification() {
        return sampleNotification(NotificationChannel.EMAIL, NotificationDeliveryStatus.SENT);
    }

    public static Notification sampleNotification(NotificationChannel channel, NotificationDeliveryStatus status) {
        Instant sentAt = status == NotificationDeliveryStatus.SENT ? DEFAULT_NOW : null;
        return sampleNotification(DEFAULT_ID, DEFAULT_RECIPIENT_ID, channel, status, null, sentAt);
    }

    public static Notification sampleNotification(
            NotificationChannel channel,
            NotificationDeliveryStatus status,
            String errorMessage,
            Instant sentAt
    ) {
        return sampleNotification(DEFAULT_ID, DEFAULT_RECIPIENT_ID, channel, status, errorMessage, sentAt);
    }

    public static Notification sampleNotification(
            UUID id,
            UUID recipientId,
            NotificationChannel channel,
            NotificationDeliveryStatus status,
            String errorMessage,
            Instant sentAt
    ) {
        return new Notification(
                id,
                recipientId,
                DEFAULT_EMAIL,
                DEFAULT_TELEGRAM_CHAT_ID,
                channel,
                DEFAULT_SUBJECT,
                DEFAULT_BODY,
                status,
                errorMessage,
                DEFAULT_NOW,
                sentAt
        );
    }

    public static Notification sampleNotification(
            UUID id,
            UUID recipientId,
            NotificationChannel channel,
            NotificationDeliveryStatus status
    ) {
        Instant sentAt = status == NotificationDeliveryStatus.SENT ? DEFAULT_NOW : null;
        return sampleNotification(id, recipientId, channel, status, null, sentAt);
    }

    public static Notification sampleNotification(
            UserNotificationProfile profile,
            NotificationChannel channel,
            NotificationDeliveryStatus status
    ) {
        return sampleNotification(DEFAULT_ID, profile, channel, status, null);
    }

    public static Notification sampleNotification(
            UUID id,
            UserNotificationProfile profile,
            NotificationChannel channel,
            NotificationDeliveryStatus status
    ) {
        return sampleNotification(id, profile, channel, status, null);
    }

    public static Notification sampleNotification(
            UUID id,
            UserNotificationProfile profile,
            NotificationChannel channel,
            NotificationDeliveryStatus status,
            String errorMessage
    ) {
        return new Notification(
                id,
                profile.userId(),
                profile.email(),
                profile.telegramChatId(),
                channel,
                DEFAULT_SUBJECT,
                DEFAULT_BODY,
                status,
                errorMessage,
                DEFAULT_NOW,
                status == NotificationDeliveryStatus.SENT ? DEFAULT_NOW : null
        );
    }

    public static Notification sampleNotificationWithNullId() {
        return sampleNotification(
                null,
                DEFAULT_RECIPIENT_ID,
                NotificationChannel.EMAIL,
                NotificationDeliveryStatus.SENT,
                null,
                null
        );
    }

    public static MessagingException sampleMessagingException() {
        return new MessagingException("Invalid subject");
    }

    public static MessagingException sampleMessagingException(String message) {
        return new MessagingException(message);
    }

    public static ResourceNotFoundException sampleResourceNotFoundException() {
        return new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE);
    }

    public static ResourceNotFoundException sampleResourceNotFoundException(String message) {
        return new ResourceNotFoundException(message);
    }

    public static RuntimeException sampleNetworkException() {
        return new RuntimeException("Network error");
    }

    public static RuntimeException sampleSmtpException() {
        return new RuntimeException(DEFAULT_ERROR_MESSAGE);
    }

    public static MimeMessage sampleMimeMessage() {
        return new MimeMessage((Session) null);
    }

    public static SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }

    public static NotificationConfig sampleNotificationConfig() {
        return new NotificationConfig(sampleNotificationProperties());
    }

    public static NotificationConfig sampleNotificationConfig(NotificationProperties properties) {
        return new NotificationConfig(properties);
    }

    public static ApplicationContextRunner sampleNotificationContextRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(NotificationConfig.class)
                .withPropertyValues(
                        "notification.mail.enabled=true",
                        "notification.mail.template-name=mail/notification-email",
                        "notification.mail.default-action-url=https://candidai.ukma.edu.ua",
                        "notification.mail.default-recipient-name=there",
                        "notification.telegram.enabled=true",
                        "notification.telegram.bot-token=123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11",
                        "notification.telegram.api-url=https://api.telegram.org",
                        "notification.telegram.polling-interval-ms=5000",
                        "notification.telegram.connect-timeout-ms=5000",
                        "notification.telegram.read-timeout-ms=10000"
                );
    }

    public static String expectedTelegramGetUpdatesUrl(long offset) {
        return DEFAULT_API_URL + "/bot" + DEFAULT_BOT_TOKEN + "/getUpdates?offset=" + offset + "&timeout=0";
    }

    public static String expectedTelegramGetUpdatesUrl() {
        return expectedTelegramGetUpdatesUrl(DEFAULT_UPDATE_OFFSET);
    }

    public static String expectedTelegramSendMessageUrl() {
        return DEFAULT_API_URL + "/bot" + DEFAULT_BOT_TOKEN + "/sendMessage";
    }

    public static String expectedHtmlGreetingWithName(String name) {
        return "Hello, <span>" + name + "</span>!";
    }

    public static String expectedHtmlDefaultGreeting() {
        return "Hello, <span>" + DEFAULT_RECIPIENT_NAME + "</span>!";
    }

    public static String expectedVacancyStatusChangedSubject(VacancyStatusChangedEvent event) {
        return "Updated vacancy status: " + event.vacancyTitle();
    }

    public static String expectedVacancyStatusChangedBody(VacancyStatusChangedEvent event) {
        return String.format(
                "Your vacancy '%s' status was changed from %s to %s.",
                event.vacancyTitle(), event.oldStatus(), event.newStatus()
        );
    }

    public static String expectedEmailSendErrorMessage(String email) {
        return "Failed to construct or send email to " + email;
    }

    public static String currentYearString() {
        return String.valueOf(Year.now().getValue());
    }
}
