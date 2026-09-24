# Domain Flow & Business Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve the 6 critical logical gaps and architectural discrepancies identified during the domain audit: candidate notifications, AI matching score integration into recruitment, Company domain representation, Vacancy DRAFT status accessibility, interview feedback status guard, and Application domain entity separation.

**Architecture:** Spring Modulith modular monolith with strict domain boundaries enforced via `package-info.java` and `ApplicationModules.verify()`. Clean separation of domain entities and DTOs, interfaces for cross-module communication, stateless singleton services, and asynchronous event-driven notifications.

**Tech Stack:** Java 25, Spring Boot 4, Spring Modulith, AssertJ, Mockito, Jakarta Validation.

**Spec / Audit Basis:** `E:\work\demo\docs\proposal.md` (Entities, UC-1..UC-6) and `docs/service-layer-architecture.md`.

## Global Constraints
- Naming convention for all tests: `givenX_y_shouldZ` where `y` is the method/action under test. Never use `when` in `y`.
- Assertions: Exclusively AssertJ (`assertThat(...)`). Zero JUnit/Hamcrest assertions.
- Fast testing: Exclusively Mockito (`@ExtendWith(MockitoExtension.class)`) or POJO unit tests for all business services and listeners. Zero `@SpringBootTest`.
- Strict Constructor Injection: Zero `@Autowired` on fields in `src/main/java`.
- Stateless Services: All `@Service` classes must have zero mutable instance state.

---

## Task Breakdown

### Task 1: Application Domain Entity Separation & Feedback Status Guard (Items 2.3 & 2.4)
**Files:**
- Create: `src/main/java/ua/edu/ukma/candidai/recruitment/model/Application.java`
- Create: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationMapper.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/repository/ApplicationRepository.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/repository/InMemoryApplicationRepository.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceImpl.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/recruitment/repository/InMemoryApplicationRepositoryTest.java`

**Description:**
1. Separate domain model from presentation: create domain entity `Application` with internal state transition checks (`updateStatus(status, comment, now)`).
2. Update `ApplicationRepository` to persist `Application` domain entities rather than `ApplicationResponse` DTOs.
3. In `ApplicationServiceImpl.submitFeedback`, add check: `if (application.getStatus() != ApplicationStatus.INTERVIEW)` throw `InvalidStateTransitionException`.

- [ ] **Step 1: Create `Application` domain entity in `recruitment.model`**
- [ ] **Step 2: Create `ApplicationMapper` (or mapping methods) between `Application` and `ApplicationResponse`**
- [ ] **Step 3: Update `ApplicationRepository` and `InMemoryApplicationRepository` to operate on `Application` entity**
- [ ] **Step 4: Update `ApplicationServiceImpl` to use entity and enforce `INTERVIEW` status on `submitFeedback`**
- [ ] **Step 5: Write unit tests in `ApplicationServiceTest` for `submitFeedback` status validation**
- [ ] **Step 6: Run tests and verify all pass**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.recruitment.*`
- [ ] **Step 7: Commit changes**

---

### Task 2: Vacancy DRAFT Status Accessibility (Item 2.2)
**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/dto/request/CreateVacancyRequest.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/model/Vacancy.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/vacancy/service/VacancyServiceTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/vacancy/TestResources.java`

**Description:**
Enable creating vacancies in `DRAFT` status:
1. Add optional `VacancyStatus status` to `CreateVacancyRequest`.
2. In `Vacancy.create(...)`: if `request.status() != null`, use it (allowing `DRAFT` or `OPEN`); otherwise default to `VacancyStatus.OPEN`.
3. If status is `DRAFT`, set `publishedAt` to null.
4. Add unit test verifying vacancy creation in `DRAFT` status and subsequent transition to `OPEN`.

- [x] **Step 1: Add `status` field to `CreateVacancyRequest`**
- [x] **Step 2: Update `Vacancy.create()` to honor `request.status()`**
- [x] **Step 3: Add unit tests in `VacancyServiceTest` for creating DRAFT vacancy**
- [x] **Step 4: Run tests to verify all pass**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.vacancy.*`
- [x] **Step 5: Commit changes**

---

### Task 3: Company Representation in Domain Model (Item 2.1)
**Files:**
- Create: `src/main/java/ua/edu/ukma/candidai/vacancy/model/Company.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/model/Vacancy.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/dto/request/CreateVacancyRequest.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/dto/response/VacancyResponse.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/service/VacancyMapper.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/vacancy/TestResources.java`

**Description:**
In `proposal.md` Entity 2: `Company` (id, name, description, logoUrl, contactEmail).
1. Create `Company` model in `vacancy.model`.
2. Add optional `UUID companyId` to `Vacancy`, `CreateVacancyRequest`, and `VacancyResponse`.
3. Update `VacancyMapper` to map `companyId`.

- [x] **Step 1: Create `Company` record in `ua.edu.ukma.candidai.vacancy.model`**
- [x] **Step 2: Add `companyId` to `Vacancy`, `CreateVacancyRequest`, `VacancyResponse`, and update `VacancyMapper`**
- [x] **Step 3: Update `TestResources` and verify `VacancyServiceTest` and `VacancyControllerTest`**
- [x] **Step 4: Run tests to verify all pass**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.vacancy.*`
- [x] **Step 5: Commit changes**

---

### Task 4: AI Screening `matchingScore` in Application & Funnel (Item 1.2)
**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/model/Application.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/dto/response/ApplicationResponse.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/RecruitmentApi.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/RecruitmentApiImpl.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceImpl.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationService.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/assessment/service/AssessmentServiceImpl.java`
- Modify: Test files for recruitment and assessment.

**Description:**
1. Add `matchingScore` (Integer, nullable) to `Application` and `ApplicationResponse`.
2. Add `updateStatus(UUID applicationId, ApplicationStatus status, Integer matchingScore, String comment)` in `RecruitmentApi` & `ApplicationService`.
3. In `AssessmentServiceImpl.executeScreening`: pass `result.matchingScore()` to `recruitmentApi.updateStatus(...)`.
4. Ensure `GET /api/v1/applications/vacancy/{vacancyId}` returns the calculated `matchingScore`.

- [ ] **Step 1: Add `matchingScore` field to `Application` and `ApplicationResponse`**
- [ ] **Step 2: Add overloaded `updateStatus` method with `matchingScore` in `ApplicationService` & `RecruitmentApi`**
- [ ] **Step 3: Update `AssessmentServiceImpl` to pass `result.matchingScore()` on screening completion**
- [ ] **Step 4: Update test fixtures and unit tests in `ApplicationServiceTest` and `AssessmentServiceImplTest`**
- [ ] **Step 5: Run tests to verify all pass**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.recruitment.* --tests ua.edu.ukma.candidai.assessment.*`
- [ ] **Step 6: Commit changes**

---

### Task 5: Candidate Notification Pipeline (Item 1.1)
**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/package-info.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/notification/service/NotificationDispatcher.java`
- Create: `src/main/java/ua/edu/ukma/candidai/notification/listener/ApplicationNotificationListener.java`
- Create: `src/test/java/ua/edu/ukma/candidai/notification/listener/ApplicationNotificationListenerTest.java`
- Modify: `src/test/java/ua/edu/ukma/candidai/notification/service/NotificationDispatcherTest.java`

**Description:**
1. Update `notification/package-info.java` to allow dependencies on `"recruitment"`, `"recruitment::event"`.
2. In `NotificationDispatcher`, add `dispatchToContact(String email, String telegramChatId, String fullName, String subject, String body)` supporting notifications to applicants without an existing `User` entity.
3. Create `ApplicationNotificationListener`:
   - `@ApplicationModuleListener on(ApplicationSubmittedEvent event)`: sends confirmation to applicant email.
   - `@ApplicationModuleListener on(ApplicationStatusChangedEvent event)`: sends status update to applicant email.
4. Write unit tests with Mockito and AssertJ.

- [ ] **Step 1: Update `notification/package-info.java`**
- [ ] **Step 2: Add `dispatchToContact(...)` in `NotificationDispatcher`**
- [ ] **Step 3: Create `ApplicationNotificationListener` and implement event handlers**
- [ ] **Step 4: Create `ApplicationNotificationListenerTest`**
- [ ] **Step 5: Run notification tests and modularity tests**
  Run: `./gradlew test --tests ua.edu.ukma.candidai.notification.*`
  Run: `./gradlew test --tests ua.edu.ukma.candidai.ModularityTests`
- [ ] **Step 6: Commit changes**

---

### Task 6: Full Verification
**Files:** All codebase

**Description:**
Run full test suite and verify modularity, checkstyle, and code quality.

- [ ] **Step 1: Run full test suite: `./gradlew test`**
- [ ] **Step 2: Verify `ModularityTests` passes**
