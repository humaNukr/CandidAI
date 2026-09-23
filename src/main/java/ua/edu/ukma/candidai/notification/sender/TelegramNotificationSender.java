package ua.edu.ukma.candidai.notification.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import ua.edu.ukma.candidai.notification.model.NotificationChannel;
import ua.edu.ukma.candidai.notification.telegram.TelegramBotClient;
import ua.edu.ukma.candidai.user.UserNotificationProfile;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationSender implements NotificationSender {

    private static final String PARSE_MODE_HTML = "HTML";

    private final TelegramBotClient botClient;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.TELEGRAM;
    }

    @Override
    public boolean supports(UserNotificationProfile recipient) {
        return recipient != null && recipient.telegramChatId() != null && !recipient.telegramChatId().isBlank();
    }

    @Override
    public void send(UserNotificationProfile recipient, String subject, String body) {
        String formattedText = "<b>" + HtmlUtils.htmlEscape(subject) + "</b>\n\n" + HtmlUtils.htmlEscape(body);
        botClient.sendMessage(recipient.telegramChatId(), formattedText, PARSE_MODE_HTML);
    }
}
