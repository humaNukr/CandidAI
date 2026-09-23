package ua.edu.ukma.candidai.notification.email;

public interface EmailTransport {
    void sendEmail(String to, String subject, String htmlContent);
}
