# Notification Module Architecture Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the notification module and related user/vacancy integration into a clean, modular, immutable, event-driven architecture adhering strictly to SOLID, KISS, YAGNI, and "Design by Contract" principles.

**Architecture:** 
- Pure event-driven integration using Spring Modulith `@ApplicationModuleListener` running on Virtual Threads (Project Loom).
- Strategy Pattern for delivery channels (`NotificationSender` with `supports(profile)`).
- Decomposed Email subsystem (`EmailTemplateRenderer` + `SmtpEmailTransport` via `JavaMailSender`).
- Decomposed Telegram subsystem (`TelegramBotClient` + `TelegramCommandHandler` with proper error handling + `TelegramPollingService`).
- Immutable `record Notification` with lifecycle transition methods.
- "Design by Contract": validations at system boundaries only, fail-fast typed exceptions (`ResourceNotFoundException`), zero internal defensive boilerplate.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Modulith 1.3.2, Thymeleaf, Jakarta Mail (`spring-boot-starter-mail`), AssertJ, Mockito.

**Spec:** `docs/superpowers/specs/2026-09-22-notification-refactoring-design.md` (and `NOTIFICATION_REFACTORING_SPEC.md`)

## Global Constraints
- Target Java language version: 25 (`release = 21` options configured in Gradle).
- All domain records and models must be immutable.
- Zero defensive null checks or `if` statements in internal domain logic for trusted data; validate strictly at external boundaries (Telegram inputs, REST, properties).
- Checkstyle rules must pass with 0 errors and 0 warnings (`./gradlew checkstyleMain checkstyleTest`).
- Spring Modulith architecture verification must pass (`ModularityTests`).
- When executing subagent tasks, subagents MUST follow TDD (Red-Green-Refactor). If any subagent lacks context or data, they must ask rather than invent.

---

### Task 1: Configuration & Mail Starter Dependencies

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/config/NotificationProperties.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/config/NotificationConfig.java`

**Interfaces:**
- Produces: `NotificationProperties.TelegramProperties(..., long connectTimeoutMs, long readTimeoutMs)`
- Produces: Spring Boot `JavaMailSender` bean availability via `spring-boot-starter-mail`

- [ ] **Step 1: Update `build.gradle.kts` to add `spring-boot-starter-mail`**
Add `implementation("org.springframework.boot:spring-boot-starter-mail")` to dependencies.

- [ ] **Step 2: Update `application.yaml` with Telegram timeout properties and default mail settings**
```yaml
notification:
  mail:
    template-name: mail/notification-email
    default-action-url: https://candidai.ukma.edu.ua
    default-recipient-name: there
  telegram:
    enabled: ${TELEGRAM_ENABLED:false}
    bot-token: ${TELEGRAM_BOT_TOKEN:}
    api-url: ${TELEGRAM_API_URL:https://api.telegram.org}
    polling-interval-ms: 5000
    connect-timeout-ms: 5000
    read-timeout-ms: 10000
```

- [ ] **Step 3: Update `NotificationProperties.java` record to include timeouts**
Add `long connectTimeoutMs` and `long readTimeoutMs` to `TelegramProperties`.

- [ ] **Step 4: Update `NotificationConfig.java` with timeout-configured `RestClient` builder/bean**
Configure `RestClient` using `JdkClientHttpRequestFactory` with connection and read timeouts.

- [ ] **Step 5: Verify build compiles**
Run: `./gradlew compileJava checkstyleMain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**
```bash
git add build.gradle.kts src/main/resources/application.yaml src/main/java/ua/edu/ukma/candidai/notification/config/
git commit -m "build(notification): add spring-boot-starter-mail and telegram timeout properties"
```

---

### Task 2: Immutable Notification Domain Model & Repository

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/model/Notification.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/repository/InMemoryNotificationRepository.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/repository/InMemoryNotificationRepositoryTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/NotificationTestResources.java`

**Interfaces:**
- Produces: `record Notification(...)` with `Notification.pending(...)`, `markSent(Instant)`, `markFailed(String)`
- Produces: `NotificationRepository` working with the immutable record

- [ ] **Step 1: Write unit tests for `Notification` record lifecycle methods in `InMemoryNotificationRepositoryTest.java`**
Verify `pending()`, `markSent()`, and `markFailed()` create new immutable records with expected fields and statuses.

- [ ] **Step 2: Refactor `Notification.java` into an immutable Java `record`**
Replace Lombok `@Getter`, `@Builder`, `@EqualsAndHashCode` class with `public record Notification(...)`. Include static factory `pending(...)` and transition methods `markSent(Instant sentAt)` and `markFailed(String errorMessage)`.

- [ ] **Step 3: Update `InMemoryNotificationRepository.java` and `NotificationTestResources.java`**
Ensure repository storage uses `Notification.id()` and test resources supply valid sample records.

- [ ] **Step 4: Run repository tests**
Run: `./gradlew test --tests ua.edu.ukma.candidai.notification.repository.*`
Expected: Tests PASS

- [ ] **Step 5: Commit**
```bash
git add src/main/java/ua/edu/ukma/candidai/notification/model/Notification.java src/main/java/ua/edu/ukma/candidai/notification/repository/ src/test/java/ua/edu/ukma/candidai/notification/
git commit -m "refactor(notification): convert Notification model to immutable record with lifecycle transitions"
```

---

### Task 3: Fail-Fast UserApi Contract (Design by Contract)

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/user/UserApi.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/user/service/UserApiImpl.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/user/service/UserServiceTest.java`

**Interfaces:**
- Produces: `void UserApi.linkTelegramChatId(UUID userId, String telegramChatId)` throwing `ResourceNotFoundException` when user does not exist.

- [ ] **Step 1: Write test for `linkTelegramChatId` throwing `ResourceNotFoundException`**
Add test case in `UserServiceTest.java` checking that `linkTelegramChatId` throws `ResourceNotFoundException` if `findById` returns empty.

- [ ] **Step 2: Update `UserApi` interface**
Change method signature from `boolean linkTelegramChatId(UUID userId, String telegramChatId);` to `void linkTelegramChatId(UUID userId, String telegramChatId);`.

- [ ] **Step 3: Update `UserApiImpl` implementation**
Remove `if (userId == null) return false;`. If user is found, update `telegramChatId` and save. If not found, throw `new ResourceNotFoundException("User not found with id: " + userId);`.

- [ ] **Step 4: Run user service tests**
Run: `./gradlew test --tests ua.edu.ukma.candidai.user.*`
Expected: Tests PASS

- [ ] **Step 5: Commit**
```bash
git add src/main/java/ua/edu/ukma/candidai/user/ src/test/java/ua/edu/ukma/candidai/user/
git commit -m "refactor(user): enforce fail-fast ResourceNotFoundException in UserApi.linkTelegramChatId"
```

---

### Task 4: Email Subsystem Decomposition (Renderer + SMTP Transport + Sender)

**Files:**
- Create: `src/main/java/ua/edu/ukma/candidai/notification/email/EmailTemplateRenderer.java`
- Create: `src/main/java/ua/edu/ukma/candidai/notification/email/EmailTransport.java`
- Create: `src/main/java/ua/edu/ukma/candidai/notification/email/SmtpEmailTransport.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/sender/NotificationSender.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/sender/EmailNotificationSender.java`
- Create: `src/test/java/ua/edu/ukma/candidai/notification/email/EmailTemplateRendererTest.java`
- Create: `src/test/java/ua/edu/ukma/candidai/notification/email/SmtpEmailTransportTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/sender/EmailNotificationSenderTest.java`

**Interfaces:**
- Produces: `boolean NotificationSender.supports(UserNotificationProfile recipient)`
- Produces: `EmailTemplateRenderer.render(String recipientName, String subject, String body)`
- Produces: `EmailTransport.sendEmail(String to, String subject, String htmlContent)`

- [ ] **Step 1: Write unit tests for `EmailTemplateRendererTest` and `SmtpEmailTransportTest`**
- [ ] **Step 2: Create `EmailTemplateRenderer`**
Extract Thymeleaf rendering from `EmailNotificationSender`. Set context variables: `recipientName` (fallback to `defaultRecipientName`), `subject`, `body`, `actionUrl`, `year` (`Year.now().getValue()`).
- [ ] **Step 3: Create `EmailTransport` interface and `SmtpEmailTransport` implementation**
Inject `JavaMailSender`. Build MIME message with `MimeMessageHelper(mimeMessage, true, "UTF-8")`, set `setTo(to)`, `setSubject(subject)`, `setText(htmlContent, true)`, and call `mailSender.send(mimeMessage)`.
- [ ] **Step 4: Update `NotificationSender` and refactor `EmailNotificationSender`**
Implement `supports(recipient)`: returns `recipient != null && recipient.email() != null && !recipient.email().isBlank()`.
Implement `send(...)`: renders HTML using `EmailTemplateRenderer`, then dispatches using `EmailTransport`.
- [ ] **Step 5: Update `EmailNotificationSenderTest`**
Eliminate Logback `ListAppender` assertions. Verify `emailTransport.sendEmail(...)` via Mockito.
- [ ] **Step 6: Run email subsystem tests**
Run: `./gradlew test --tests ua.edu.ukma.candidai.notification.email.* --tests ua.edu.ukma.candidai.notification.sender.EmailNotificationSenderTest`
Expected: Tests PASS
- [ ] **Step 7: Commit**
```bash
git add src/main/java/ua/edu/ukma/candidai/notification/email/ src/main/java/ua/edu/ukma/candidai/notification/sender/ src/test/java/ua/edu/ukma/candidai/notification/
git commit -m "feat(notification): decompose email subsystem into renderer, SMTP transport, and strategy sender"
```

---

### Task 5: Telegram Subsystem Decomposition & Bug Fix

**Files:**
- Create: `src/main/java/ua/edu/ukma/candidai/notification/telegram/TelegramBotClient.java`
- Create: `src/main/java/ua/edu/ukma/candidai/notification/telegram/TelegramCommandHandler.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/telegram/TelegramPollingService.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/sender/TelegramNotificationSender.java`
- Create: `src/test/java/ua/edu/ukma/candidai/notification/telegram/TelegramCommandHandlerTest.java`
- Create: `src/test/java/ua/edu/ukma/candidai/notification/telegram/TelegramBotClientTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/sender/TelegramNotificationSenderTest.java`

**Interfaces:**
- Produces: `TelegramBotClient` (HTTP calls with timeouts)
- Produces: `TelegramCommandHandler` (processes `/start <UUID>`, handles `ResourceNotFoundException` accurately)
- Produces: `TelegramNotificationSender` (implements `supports()`, sends via client with HTML escaping)

- [ ] **Step 1: Write unit tests for `TelegramCommandHandlerTest` and `TelegramBotClientTest`**
Test valid UUID + user found -> confirmation sent.
Test valid UUID + user not found -> user-not-found sent.
Test invalid UUID payload -> invalid-format sent.
- [ ] **Step 2: Implement `TelegramBotClient`**
Consolidate URL formatting (`apiUrl.replaceAll("/+$", "") + "/bot" + token + "/" + path`). Implement `getUpdates(long offset)` and `sendMessage(String chatId, String text, String parseMode)`.
- [ ] **Step 3: Implement `TelegramCommandHandler`**
Handle `/start <UUID>`. Call `userApi.linkTelegramChatId(userId, chatId)`. Catch `ResourceNotFoundException` and send accurate user-not-found message.
- [ ] **Step 4: Refactor `TelegramPollingService`**
Only responsible for `@Scheduled` loop, calling `botClient.getUpdates()`, updating `offset`, and delegating messages to `TelegramCommandHandler`.
- [ ] **Step 5: Refactor `TelegramNotificationSender`**
Implement `supports(recipient)`: returns `recipient != null && recipient.telegramChatId() != null && !recipient.telegramChatId().isBlank()`.
Format message using `org.springframework.web.util.HtmlUtils.htmlEscape` and send via `botClient.sendMessage(...)`.
- [ ] **Step 6: Run telegram subsystem tests**
Run: `./gradlew test --tests ua.edu.ukma.candidai.notification.telegram.* --tests ua.edu.ukma.candidai.notification.sender.TelegramNotificationSenderTest`
Expected: Tests PASS
- [ ] **Step 7: Commit**
```bash
git add src/main/java/ua/edu/ukma/candidai/notification/telegram/ src/main/java/ua/edu/ukma/candidai/notification/sender/TelegramNotificationSender.java src/test/java/ua/edu/ukma/candidai/notification/
git commit -m "refactor(notification): decompose telegram subsystem and fix user linking error handling"
```

---

### Task 6: Strategy Dispatcher, Domain Event Listener & Package Modularity

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/service/NotificationDispatcher.java`
- Create: `src/main/java/ua/edu/ukma/candidai/notification/listener/VacancyNotificationListener.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/package-info.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/service/NotificationDispatcherTest.java`
- Create: `src/test/java/ua/edu/ukma/candidai/notification/listener/VacancyNotificationListenerTest.java`

**Interfaces:**
- Produces: `NotificationDispatcher.dispatch(UUID recipientId, String subject, String body)` with OCP strategy dispatching.
- Produces: `VacancyNotificationListener` with `@ApplicationModuleListener` handling `VacancyStatusChangedEvent`.

- [ ] **Step 1: Write unit tests in `NotificationDispatcherTest` and `VacancyNotificationListenerTest`**
Test `NotificationDispatcher` iterating through senders checking `sender.supports(profile)`.
Test `VacancyNotificationListener` mapping `VacancyStatusChangedEvent` to `dispatcher.dispatch(...)`.
- [ ] **Step 2: Refactor `NotificationDispatcher.java`**
Remove `switch (channel)` and direct profile field checking. Iterate through `senders`, checking `if (sender.supports(profile))`.
Save `Notification.pending(...)`, execute `sender.send(...)`, update to `markSent(...)` or `markFailed(...)`, and save.
- [ ] **Step 3: Implement `VacancyNotificationListener.java`**
Annotate method with `@ApplicationModuleListener public void on(VacancyStatusChangedEvent event)`.
- [ ] **Step 4: Update `notification/package-info.java`**
Set `allowedDependencies = {"common", "user", "vacancy"}`.
- [ ] **Step 5: Run tests**
Run: `./gradlew test --tests ua.edu.ukma.candidai.notification.service.* --tests ua.edu.ukma.candidai.notification.listener.*`
Expected: Tests PASS
- [ ] **Step 6: Commit**
```bash
git add src/main/java/ua/edu/ukma/candidai/notification/ src/test/java/ua/edu/ukma/candidai/notification/
git commit -m "refactor(notification): implement strategy dispatching and vacancy domain event listener"
```

---

### Task 7: Full System Verification & Code Quality Polish

**Files:**
- Review and verify all modified files across `ua.edu.ukma.candidai.notification` and `ua.edu.ukma.candidai.user`.

- [ ] **Step 1: Run Modularity verification**
Run: `./gradlew test --tests ua.edu.ukma.candidai.ModularityTests`
Expected: PASS with 0 modularity violations.
- [ ] **Step 2: Run entire test suite**
Run: `./gradlew test`
Expected: All tests PASS.
- [ ] **Step 3: Run Checkstyle verification**
Run: `./gradlew checkstyleMain checkstyleTest`
Expected: 0 warnings, 0 errors.
- [ ] **Step 4: Final verification commit if any formatting adjustments are needed**
```bash
git status
```
