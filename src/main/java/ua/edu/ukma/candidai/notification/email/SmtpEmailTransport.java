package ua.edu.ukma.candidai.notification.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.notification.config.NotificationProperties;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailTransport implements EmailTransport {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void sendEmail(String to, String subject, String htmlContent) {
        if (!properties.mail().enabled()) {
            log.info("[EMAIL-DEV] Mail is disabled (notification.mail.enabled=false). "
                    + "Simulated email to: {} | Subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.debug("Sent MIME email to {}", to);
        } catch (MessagingException e) {
            throw new MailSendException("Failed to construct or send email to " + to, e);
        }
    }
}
