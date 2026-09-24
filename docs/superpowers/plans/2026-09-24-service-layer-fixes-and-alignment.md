# Service Layer Fixes & Architecture Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix architectural violations, implement missing course requirements, eliminate interface leakage, and align domain flows (candidate notifications, screening score visibility, and state machine guards) across the CandidAI codebase.

**Architecture:** Spring Modulith modular monolith with strict domain boundaries enforced via `package-info.java` and `ApplicationModules.verify()`. Components interact strictly through Java interfaces, dependency injection is 100% constructor-based, services are stateless singletons, and domain events decouple cross-module side effects asynchronously via `@ApplicationModuleListener`.

**Tech Stack:** Java 25, Spring Boot 4, Spring Modulith, Spring Data, AssertJ, Mockito, Jakarta Validation, Jackson.

**Spec / Audit Basis:** Audit of course criteria (HW Lecture 3) and comparison with `proposal.md` and `docs/service-layer-architecture.md`.

## Global Constraints
- Naming convention for all tests: `givenX_y_shouldZ` where `y` is the method/action under test. Never use `when` in `y`.
- Assertions: Exclusively AssertJ (`assertThat(...)`). Zero JUnit/Hamcrest assertions.
- Fast testing: Exclusively Mockito (`@ExtendWith(MockitoExtension.class)`) or POJO unit tests for all business services and listeners. Zero `@SpringBootTest`.
- Controller slice tests: `@WebMvcTest` only, deserializing responses to DTOs or RFC 9457 `ProblemDetail`.
- Strict Constructor Injection: Zero `@Autowired` on fields in `src/main/java`.
- Stateless Services: All `@Service` classes must have zero mutable instance state.

---

## File Structure & Proposed Changes

```text
├── README.md                                                        [CREATE: Root documentation]
├── src/main/java/ua/edu/ukma/candidai/
│   ├── assessment/
│   │   ├── controller/AssessmentController.java                    [MODIFY: Remove ScreeningResultRepository, use AssessmentService exclusively]
│   │   └── service/
│   │       ├── AssessmentService.java                               [MODIFY: Add getScreeningResult(UUID)]
│   │       └── AssessmentServiceImpl.java                           [MODIFY: Implement getScreeningResult(UUID)]
│   ├── notification/
│   │   ├── package-info.java                                        [MODIFY: Add allowedDependencies "recruitment", "recruitment::event"]
│   │   ├── service/NotificationDispatcher.java                      [MODIFY: Add direct recipient dispatch for candidate notifications]
│   │   ├── listener/ApplicationNotificationListener.java            [CREATE: Listener for ApplicationSubmittedEvent & ApplicationStatusChangedEvent]
│   │   └── telegram/
│   │       ├── TelegramPollingService.java                          [MODIFY: Remove mutable offset field]
│   │       └── TelegramOffsetHolder.java                            [CREATE: Dedicated state holder for polling offset]
│   └── recruitment/
│       ├── dto/response/ApplicationResponse.java                    [MODIFY: Add matchingScore field]
│       ├── RecruitmentApi.java                                      [MODIFY: Add matchingScore parameter or method overload]
│       ├── service/
│       │   ├── ApplicationServiceImpl.java                          [MODIFY: Guard submitFeedback to INTERVIEW status, support matchingScore]
│       │   └── RecruitmentApiImpl.java                              [MODIFY: Update implementation for matchingScore]
└── src/test/java/ua/edu/ukma/candidai/
    ├── ModularityTests.java                                         [MODIFY: Rename test to givenApplicationModules_verify_shouldPassArchitectureValidation]
    ├── assessment/
    │   ├── controller/AssessmentControllerTest.java                 [MODIFY: Update mocks to use AssessmentService exclusively]
    │   └── service/AssessmentServiceImplTest.java                   [MODIFY: Add test for getScreeningResult]
    ├── notification/
    │   ├── listener/ApplicationNotificationListenerTest.java        [CREATE: Unit tests for candidate notification listener]
    │   └── telegram/TelegramPollingServiceTest.java                 [MODIFY: Update tests for offset holder]
    └── recruitment/
        ├── controller/ApplicationControllerTest.java                [MODIFY: Add 409 Conflict & 422 Unprocessable ProblemDetail tests]
        ├── service/
        │   ├── ApplicationServiceTest.java                          [MODIFY: Add tests for submitFeedback INTERVIEW guard & matchingScore]
        │   ├── RecruitmentApiImplTest.java                          [CREATE: Unit tests for RecruitmentApiImpl adapter]
        │   └── listener/ApplicationEventListenerTest.java           [CREATE: Unit tests for logging listener]
```

---

## Task Breakdown

### Task 1: Root `README.md` Documentation
**Files:**
- Create: `README.md`

**Description:**
Create the root `README.md` containing all required sections for the course evaluation:
1. Executive summary of CandidAI & Spring Modulith architecture diagram.
2. Formal Business Rules specification (vacancy uniqueness 409, application uniqueness 409, open vacancy requirement 404, candidate evaluation gate 422).
3. State Transition Matrices with Mermaid diagrams:
   - `VacancyStatus`: `DRAFT`, `OPEN`, `PAUSED`, `CLOSED`, `ARCHIVED`.
   - `ApplicationStatus`: `APPLIED`, `SCREENING`, `INTERVIEW`, `OFFER`, `HIRED`, `REJECTED`.
4. Strategy Pattern documentation: `CandidateEvaluationStrategy` and `NotificationSender` with Spring collection injection without `@Qualifier`.
5. Error Handling: RFC 7807 / RFC 9457 `ProblemDetail` mapping table.
6. Verification & Build instructions (`./gradlew test`, architecture tests).

- [ ] **Step 1: Write `README.md` at repository root**
- [ ] **Step 2: Verify all markdown formatting and Mermaid diagrams render cleanly**
- [ ] **Step 3: Commit `README.md`**

---

### Task 2: Service Contract Abstraction in `AssessmentController`
**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/assessment/service/AssessmentService.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/assessment/service/AssessmentServiceImpl.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/assessment/controller/AssessmentController.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/assessment/controller/AssessmentControllerTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/assessment/service/AssessmentServiceImplTest.java`

**Interfaces:**
- Consumes: `ScreeningResultRepository.findByApplicationId(UUID)` in `AssessmentServiceImpl`.
- Produces: `AssessmentService.getScreeningResult(UUID applicationId)` returning `AiScreeningResult`.

- [ ] **Step 1: Write failing unit test in `AssessmentServiceImplTest` for `getScreeningResult`**
- [ ] **Step 2: Add `getScreeningResult(UUID applicationId)` to `AssessmentService` and implement in `AssessmentServiceImpl`**
- [ ] **Step 3: Refactor `AssessmentController` to inject only `AssessmentService`**
- [ ] **Step 4: Update `AssessmentControllerTest` to mock `AssessmentService.getScreeningResult` instead of repository**
- [ ] **Step 5: Run tests and verify all pass**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.assessment.*`
- [ ] **Step 6: Commit changes**

---

### Task 3: State Machine Guard in `ApplicationServiceImpl.submitFeedback`
**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceImpl.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceTest.java`

**Description:**
Guard that interview feedback can only be submitted if `application.status() == ApplicationStatus.INTERVIEW`. If not, throw `InvalidStateTransitionException`.

- [ ] **Step 1: Write failing unit test in `ApplicationServiceTest`**
  - `givenNonInterviewStatus_submitFeedback_shouldThrowInvalidStateTransitionException`
- [ ] **Step 2: Run test to confirm failure**
- [ ] **Step 3: Implement validation check in `ApplicationServiceImpl.submitFeedback`**
  ```java
  ApplicationResponse application = findApplicationOrThrow(id);
  if (application.status() != ApplicationStatus.INTERVIEW) {
      throw new InvalidStateTransitionException(
              "Cannot submit interview feedback for application " + id + " in status " + application.status()
      );
  }
  ```
- [ ] **Step 4: Run test to verify it passes**
- [ ] **Step 5: Commit changes**

---

### Task 4: Stateless Singleton Refactoring in `TelegramPollingService`
**Files:**
- Create: `src/main/java/ua/edu/ukma/candidai/notification/telegram/TelegramOffsetHolder.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/telegram/TelegramPollingService.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/telegram/TelegramPollingServiceTest.java`

**Description:**
Extract the mutable `long offset` from `TelegramPollingService` into a dedicated thread-safe `@Component` `TelegramOffsetHolder` so `TelegramPollingService` is a pure stateless singleton.

- [ ] **Step 1: Create `TelegramOffsetHolder` component**
  ```java
  @Component
  public class TelegramOffsetHolder {
      private final AtomicLong offset = new AtomicLong(0);
      public long get() { return offset.get(); }
      public void set(long newOffset) { offset.set(newOffset); }
  }
  ```
- [ ] **Step 2: Inject `TelegramOffsetHolder` into `TelegramPollingService` and use it**
- [ ] **Step 3: Update `TelegramPollingServiceTest` to supply `TelegramOffsetHolder` mock or instance**
- [ ] **Step 4: Run tests to verify all pass**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.notification.telegram.*`
- [ ] **Step 5: Commit changes**

---

### Task 5: Missing Test Suites & ProblemDetail Controller Slices
**Files:**
- Create: `src/test/java/ua/edu/ukma/candidai/recruitment/service/listener/ApplicationEventListenerTest.java`
- Create: `src/test/java/ua/edu/ukma/candidai/recruitment/service/RecruitmentApiImplTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/ModularityTests.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/vacancy/controller/VacancyControllerTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/recruitment/controller/ApplicationControllerTest.java`

**Description:**
Add the missing Mockito unit tests and verify HTTP 409 Conflict & 422 Unprocessable Content RFC 9457 `ProblemDetail` responses in MockMvc controller tests. Rename modularity test to adhere to standard.

- [ ] **Step 1: Write `ApplicationEventListenerTest` for both event handlers**
- [ ] **Step 2: Write `RecruitmentApiImplTest` verifying delegation to `ApplicationService`**
- [ ] **Step 3: Add 409 & 422 tests to `VacancyControllerTest` and `ApplicationControllerTest`**
- [ ] **Step 4: Rename test method in `ModularityTests` to `givenApplicationModules_verify_shouldPassArchitectureValidation`**
- [ ] **Step 5: Run full test suite and verify 100% green**
  Run: `./gradlew test`
- [ ] **Step 6: Commit changes**

---

### Task 6: Candidate Notification Pipeline (UC-6 Alignment)
**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/package-info.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/service/NotificationDispatcher.java`
- Create: `src/main/java/ua/edu/ukma/candidai/notification/listener/ApplicationNotificationListener.java`
- Create: `src/test/java/ua/edu/ukma/candidai/notification/listener/ApplicationNotificationListenerTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/service/NotificationDispatcherTest.java`

**Description:**
Enable `notification` module to receive recruitment events (`ApplicationSubmittedEvent` and `ApplicationStatusChangedEvent`). Add direct contact dispatch method in `NotificationDispatcher` and implement `ApplicationNotificationListener` to deliver emails/Telegram updates to applicants.

- [ ] **Step 1: Update `notification/package-info.java` allowedDependencies to include `"recruitment"`, `"recruitment::event"`**
- [ ] **Step 2: Add `dispatchDirect(String email, String telegramChatId, String fullName, String subject, String body)` to `NotificationDispatcher`**
- [ ] **Step 3: Write `ApplicationNotificationListener` handling `ApplicationSubmittedEvent` and `ApplicationStatusChangedEvent`**
- [ ] **Step 4: Write `ApplicationNotificationListenerTest` verifying subject/body formatting and dispatch**
- [ ] **Step 5: Run Modularity tests and notification tests**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.ModularityTests`
  Run: `./gradlew test --tests ua.edu.ukma.candidai.notification.*`
- [ ] **Step 6: Commit changes**

---

### Task 7: AI Screening `matchingScore` Integration into Recruitment (UC-2, UC-3 Alignment)
**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/dto/response/ApplicationResponse.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/RecruitmentApi.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/RecruitmentApiImpl.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceImpl.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/assessment/service/AssessmentServiceImpl.java`
- Modify: Associated test resources and tests.

**Description:**
Add `matchingScore` (Integer, nullable) to `ApplicationResponse`. When `AssessmentServiceImpl` completes screening, pass `result.matchingScore()` to `recruitmentApi.updateStatus(...)`. Ensure `GET /api/v1/applications/vacancy/{vacancyId}` returns the calculated score.

- [ ] **Step 1: Add `matchingScore` field to `ApplicationResponse`**
- [ ] **Step 2: Update `RecruitmentApi.updateStatus` to accept `Integer matchingScore`**
- [ ] **Step 3: Update `AssessmentServiceImpl` to pass `result.matchingScore()` during status transition**
- [ ] **Step 4: Update `ApplicationServiceTest` and `AssessmentServiceImplTest`**
- [ ] **Step 5: Run all tests to verify everything passes cleanly**
  Run: `./gradlew test`
- [ ] **Step 6: Commit changes**
