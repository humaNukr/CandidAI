# Architectural Design Specification: Notification Module Refactoring

**Date:** 2026-09-22  
**Branch:** `feat/notification-email-telgram-with-bot`  
**Status:** Approved by User / Ready for Implementation Planning  

---

## 1. Executive Summary & Goals

The `notification` module in CandidAI requires architectural refactoring to eliminate technical debt, code smells, and design violations present in the current branch. 

### Core Design Principles
1. **SOLID Principles**:
   - **SRP (Single Responsibility)**: Decompose monolithic senders and polling classes into distinct components (clients, handlers, renderers, transports).
   - **OCP (Open-Closed)**: Implement the **Strategy Pattern** for notification delivery channels. Adding a new channel (e.g. SMS, Slack) requires adding a new `NotificationSender` implementation without modifying the dispatcher.
   - **LSP / ISP**: Fine-grained interfaces tailored to client needs without unnecessary dependencies.
   - **DIP**: High-level policies depend on abstractions (e.g., `EmailTransport`), not low-level implementations.
2. **KISS & YAGNI**:
   - Eliminate unnecessary abstractions and bypasses.
   - Remove redundant `NotificationApi` in favor of pure **Spring Modulith Domain Events**.
   - No unnecessary fallback beans when only SMTP is needed.
3. **Design by Contract ("Trust Your System")**:
   - Validate and sanitize data strictly at the system boundaries (REST endpoints, external Telegram updates, `@ConfigurationProperties`).
   - Eliminate defensive programming (`if (x != null)`, `if (user != null)`) inside internal services and domain logic.
   - State-modifying operations fail fast by throwing typed domain exceptions (e.g., `ResourceNotFoundException`) rather than returning boolean status flags.
4. **Immutability & Modern Java 25**:
   - Domain entities/records are immutable.
   - Lifecycle state changes (e.g. `PENDING` -> `SENT` / `FAILED`) produce new immutable instances via domain transition methods (`markSent`, `markFailed`).

---

## 2. Architecture & Module Boundaries (Spring Modulith)

### 2.1 Pure Event-Driven Integration
CandidAI uses Spring Modulith to enforce loose coupling between bounded contexts.
- Other modules (`vacancy`, `recruitment`) **do not depend on `notification`**.
- Cross-module interaction happens asynchronously via `@ApplicationModuleListener` reacting to domain events published by business modules (e.g., `VacancyStatusChangedEvent`).
- No public `NotificationApi` facade is exposed; the module acts purely as an event consumer.

### 2.2 Concurrency & Thread Pool
- With `spring.threads.virtual.enabled: true` in `application.yaml`, Spring Boot configures the `applicationTaskExecutor` to use **Java Virtual Threads (Project Loom)** (`Executors.newVirtualThreadPerTaskExecutor()`).
- All `@ApplicationModuleListener` executions run concurrently on lightweight virtual threads, preventing carrier thread starvation during blocking network I/O (SMTP, Telegram HTTP).

### 2.3 Package Modularity (`package-info.java`)
```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "user", "vacancy"}
)
package ua.edu.ukma.candidai.notification;
```

---

## 3. Detailed Component Architecture

### 3.1 Domain Model & Immutability
`ua.edu.ukma.candidai.notification.model.Notification` is refactored from a Lombok class into an immutable Java `record`:

```java
public record Notification(
        UUID id,
        UUID recipientId,
        String recipientEmail,
        String recipientTelegramChatId,
        NotificationChannel channel,
        String subject,
        String content,
        NotificationDeliveryStatus status,
        String errorMessage,
        Instant createdAt,
        Instant sentAt
) {
    public static Notification pending(
            UUID id,
            UUID recipientId,
            String recipientEmail,
            String recipientTelegramChatId,
            NotificationChannel channel,
            String subject,
            String content,
            Instant createdAt
    ) {
        return new Notification(
                id, recipientId, recipientEmail, recipientTelegramChatId,
                channel, subject, content,
                NotificationDeliveryStatus.PENDING, null, createdAt, null
        );
    }

    public Notification markSent(Instant sentAt) {
        return new Notification(
                id, recipientId, recipientEmail, recipientTelegramChatId,
                channel, subject, content,
                NotificationDeliveryStatus.SENT, null, createdAt, sentAt
        );
    }

    public Notification markFailed(String errorMessage) {
        return new Notification(
                id, recipientId, recipientEmail, recipientTelegramChatId,
                channel, subject, content,
                NotificationDeliveryStatus.FAILED, errorMessage, createdAt, null
        );
    }
}
```

### 3.2 Strategy Pattern for Notification Dispatching
`ua.edu.ukma.candidai.notification.sender.NotificationSender`:
```java
public interface NotificationSender {
    NotificationChannel getChannel();
    boolean supports(UserNotificationProfile recipient);
    void send(UserNotificationProfile recipient, String subject, String body);
}
```

`ua.edu.ukma.candidai.notification.service.NotificationDispatcher`:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final UserApi userApi;
    private final List<NotificationSender> senders;
    private final NotificationRepository notificationRepository;
    private final CommonGenerator generator;

    public void dispatch(UUID recipientId, String subject, String body) {
        userApi.getUserNotificationProfile(recipientId).ifPresentOrElse(
                profile -> dispatchToProfile(profile, subject, body),
                () -> log.warn("Cannot send notification: user profile not found for id: {}", recipientId)
        );
    }

    private void dispatchToProfile(UserNotificationProfile profile, String subject, String body) {
        for (NotificationSender sender : senders) {
            if (sender.supports(profile)) {
                sendAndRecord(sender, profile, subject, body);
            }
        }
    }

    private void sendAndRecord(
            NotificationSender sender,
            UserNotificationProfile profile,
            String subject,
            String body
    ) {
        Instant now = generator.now();
        Notification notification = Notification.pending(
                generator.uuid(),
                profile.userId(),
                profile.email(),
                profile.telegramChatId(),
                sender.getChannel(),
                subject,
                body,
                now
        );

        try {
            sender.send(profile, subject, body);
            Notification sentRecord = notification.markSent(generator.now());
            notificationRepository.save(sentRecord);
            log.info("[NOTIFICATION] Sent {} to recipient {}", sender.getChannel(), profile.userId());
        } catch (Exception e) {
            log.error("Failed to send notification via {}: {}", sender.getChannel(), e.getMessage());
            Notification failedRecord = notification.markFailed(e.getMessage());
            notificationRepository.save(failedRecord);
        }
    }
}
```

### 3.3 Email Subsystem
1. **Dependencies**:
   Add `spring-boot-starter-mail` to `build.gradle.kts`.
2. **`EmailTemplateRenderer`** (`ua.edu.ukma.candidai.notification.email`):
   - Responsible solely for HTML generation via Thymeleaf.
   - Populates context: `recipientName`, `subject`, `body`, `actionUrl`, `year`.
   - Trusts validated `NotificationProperties.mail()`.
3. **`SmtpEmailTransport`** (`ua.edu.ukma.candidai.notification.email`):
   - Injects `JavaMailSender`.
   - Constructs a UTF-8 `MimeMessage` with `MimeMessageHelper`.
   - Dispatches email over SMTP.
4. **`EmailNotificationSender`** (`ua.edu.ukma.candidai.notification.sender`):
   - Implements `NotificationSender`.
   - `supports(recipient)`: returns `recipient.email() != null && !recipient.email().isBlank()`.
   - Coordinates `templateRenderer.render(...)` and `smtpEmailTransport.sendEmail(...)`.

### 3.4 Telegram Subsystem
1. **Configuration Properties**:
   Add configurable timeouts to `NotificationProperties.TelegramProperties`:
   - `connectTimeoutMs` (default: 5000)
   - `readTimeoutMs` (default: 10000)
2. **`TelegramBotClient`** (`ua.edu.ukma.candidai.notification.telegram`):
   - Configures `RestClient` with `JdkClientHttpRequestFactory` applying connect and read timeouts.
   - Single source of truth for Telegram Bot API endpoint URL generation.
   - Methods:
     - `String getUpdates(long offset)`
     - `void sendMessage(String chatId, String text, String parseMode)`
3. **`TelegramCommandHandler`** (`ua.edu.ukma.candidai.notification.telegram`):
   - Encapsulates `/start <UUID>` command handling.
   - Parses UUID at boundary: if invalid, responds with validation error message.
   - Calls `userApi.linkTelegramChatId(userId, chatId)`.
   - Catches `ResourceNotFoundException`: if user not found, responds that user does not exist.
   - On success: sends confirmation message.
4. **`TelegramPollingService`** (`ua.edu.ukma.candidai.notification.telegram`):
   - Scheduled poller: invokes `botClient.getUpdates(offset)`.
   - Updates `offset`.
   - Delegates message payload to `telegramCommandHandler.handleMessage(chatId, text)`.
5. **`TelegramNotificationSender`** (`ua.edu.ukma.candidai.notification.sender`):
   - Implements `NotificationSender`.
   - `supports(recipient)`: returns `recipient.telegramChatId() != null && !recipient.telegramChatId().isBlank()`.
   - Uses `org.springframework.web.util.HtmlUtils.htmlEscape` for safe HTML formatting.
   - Dispatches via `botClient.sendMessage(chatId, text, "HTML")`.

### 3.5 User Subsystem Update
`UserApi` and `UserApiImpl` updated to follow "Design by Contract" (Fail-fast exceptions for state changes):
```java
public interface UserApi {
    Optional<UserNotificationProfile> getUserNotificationProfile(UUID userId);
    void linkTelegramChatId(UUID userId, String telegramChatId);
}
```
If `userRepository.findById(userId)` is empty in `linkTelegramChatId`, it throws:
```java
throw new ResourceNotFoundException("User not found with id: " + userId);
```

### 3.6 Domain Event Listener
`ua.edu.ukma.candidai.notification.listener.VacancyNotificationListener`:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class VacancyNotificationListener {

    private final NotificationDispatcher dispatcher;

    @ApplicationModuleListener
    public void on(VacancyStatusChangedEvent event) {
        String subject = "Оновлення статусу вакансії: " + event.vacancyTitle();
        String body = String.format(
                "Статус вашої вакансії '%s' було змінено з %s на %s.",
                event.vacancyTitle(), event.oldStatus(), event.newStatus()
        );
        dispatcher.dispatch(event.authorId(), subject, body);
    }
}
```

---

## 4. Logging & Privacy Policy
- **INFO level**: Restricted to structured lifecycle events:
  `[NOTIFICATION] Sent {channel} to recipient {recipientId}`
- **DEBUG level**: Message bodies, template HTML, and raw HTTP payloads. No full HTML or private tokens in INFO logs.

---

## 5. Verification Plan

### 5.1 Automated Tests
1. **`NotificationDispatcherTest`**:
   - Verify `supports()` routing.
   - Verify immutable lifecycle transition (`PENDING` -> `SENT` / `FAILED`).
   - Verify error isolation: exception in one sender does not stop remaining senders.
2. **`EmailNotificationSenderTest` & `EmailTemplateRendererTest`**:
   - Verify Thymeleaf HTML generation without inspecting Logback appenders.
   - Verify delegation to `EmailTransport`.
3. **`SmtpEmailTransportTest`**:
   - Mock `JavaMailSender` and verify correct `MimeMessage` structure and dispatch.
4. **`TelegramCommandHandlerTest`**:
   - Test `/start <UUID>` with existing user -> success message sent.
   - Test `/start <UUID>` with non-existing user -> `ResourceNotFoundException` caught, user-not-found message sent.
   - Test `/start <invalid>` -> invalid UUID format message sent.
5. **`TelegramBotClientTest`**:
   - Verify URL construction, timeout configuration, and HTTP POST/GET via `MockRestServiceServer`.
6. **`VacancyNotificationListenerTest`**:
   - Verify domain event correctly triggers `dispatcher.dispatch(...)`.
7. **`ModularityTests`**:
   - Verify Spring Modulith architectural rules via `ApplicationModules.of(CandidAiApplication.class).verify()`.
8. **Static Analysis**:
   - `./gradlew checkstyleMain checkstyleTest` passes with 0 violations.
