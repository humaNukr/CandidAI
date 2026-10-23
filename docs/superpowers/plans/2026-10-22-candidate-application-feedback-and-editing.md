# Candidate Application Feedback and Editing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide candidate feedback on vacancy listings (visual badge and edit button when already applied) and enable candidates to review and update contact details and resume for applications in `APPLIED` status.

**Architecture:** 
1. Use an SPI interface (`CandidateApplicationLookup`) in the `common` module to allow `vacancy.web` to query candidate application statuses without violating Spring Modulith unidirectional dependency rules (`recruitment` -> `vacancy`, never circular).
2. Enhance `recruitment` service layer to allow updating `phone` and `resumeUrl` on existing applications while in `APPLIED` status.
3. Update `ApplicationWebController` and `apply-form.html` to detect existing applications, pre-fill form fields, toggle edit mode, and update rather than reject with `DuplicateResourceException`.
4. Update `VacancyWebController`, `list.html`, and `cards.html` to show a status badge (e.g. `[APPLIED] Редагувати`) for already applied vacancies.

**Tech Stack:** Java 25, Spring Boot 3.5, Spring Security, Spring Modulith, Thymeleaf, AssertJ, Mockito, MockMvc.

**Spec:** Based on candidate UX requirements discussed in chat.

## Global Constraints

- Follow Spring Modulith dependency graph: `vacancy` may depend on `common` and `user`, `recruitment` may depend on `common`, `vacancy`, `user`. Never add `recruitment` to `vacancy/package-info.java`.
- Adhere to Testing Standards:
  - Test naming convention: strictly `givenX_y_shouldZ` (never use `when` in `y`).
  - AssertJ assertions only (`assertThat(...)`). No JUnit/Hamcrest assertions.
  - Full object comparison with `usingRecursiveComparison()` where applicable.
  - Logical test phases separated by a single blank line, no phase comments (`// Setup`, `// Stubbing` etc.).
  - Static star imports for test APIs (`import static org.assertj.core.api.Assertions.*;`, `import static org.mockito.Mockito.*;`).
- Max line length is 120 characters across all Java and configuration files.

---

### Task 1: Candidate Application Lookup SPI in Common and Implementation in Recruitment

**Files:**
- Create: `src/main/java/ua/edu/ukma/candidai/common/candidate/CandidateApplicationLookup.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/repository/ApplicationRepository.java`
- Create: `src/main/java/ua/edu/ukma/candidai/recruitment/service/CandidateApplicationLookupImpl.java`
- Test: `src/test/java/ua/edu/ukma/candidai/recruitment/service/CandidateApplicationLookupImplTest.java`

**Interfaces:**
- Produces: `CandidateApplicationLookup` interface in `ua.edu.ukma.candidai.common.candidate`:
  ```java
  public interface CandidateApplicationLookup {
      Map<UUID, String> findAppliedVacancyStatusesByCandidateId(UUID candidateId);
  }
  ```

- [ ] **Step 1: Write the failing test for CandidateApplicationLookupImpl**

Create `src/test/java/ua/edu/ukma/candidai/recruitment/service/CandidateApplicationLookupImplTest.java`:
Test that given a candidate ID with applications, `findAppliedVacancyStatusesByCandidateId` returns a map of `vacancyId -> status.name()`.
Test that given candidate ID with no applications or null, it returns an empty map.

- [ ] **Step 2: Run test to observe failure (RED)**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.recruitment.service.CandidateApplicationLookupImplTest"
```

- [ ] **Step 3: Create SPI interface and repository query**

Create `src/main/java/ua/edu/ukma/candidai/common/candidate/CandidateApplicationLookup.java`.
Add query to `ApplicationRepository.java`:
```java
List<Application> findByCandidateId(UUID candidateId);
Optional<Application> findByVacancyIdAndCandidateId(UUID vacancyId, UUID candidateId);
```

- [ ] **Step 4: Implement CandidateApplicationLookupImpl**

Create `src/main/java/ua/edu/ukma/candidai/recruitment/service/CandidateApplicationLookupImpl.java` implementing `CandidateApplicationLookup` with `@Service`.

- [ ] **Step 5: Run tests to verify they pass (GREEN)**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.recruitment.service.CandidateApplicationLookupImplTest"
```

- [ ] **Step 6: Commit Task 1**

```powershell
git add src/main src/test
git commit -m "feat(recruitment): add CandidateApplicationLookup SPI and implementation"
```

---

### Task 2: Application Domain & Service Update Capability

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/model/Application.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationService.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceImpl.java`
- Test: `src/test/java/ua/edu/ukma/candidai/recruitment/service/ApplicationServiceTest.java`
- Test: `src/test/java/ua/edu/ukma/candidai/recruitment/model/ApplicationTest.java`

**Interfaces:**
- Produces in `ApplicationService`:
  ```java
  ApplicationResponse updateApplication(UUID vacancyId, UUID candidateId, ApplyForVacancyRequest request);
  ```

- [ ] **Step 1: Write failing unit test in ApplicationTest for updateCandidateDetails**

Test `givenAppliedStatus_updateCandidateDetails_shouldUpdatePhoneResumeAndTimestamp`.
Test `givenNonAppliedStatus_updateCandidateDetails_shouldThrowInvalidStateTransitionException`.

- [ ] **Step 2: Run test to observe failure (RED)**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.recruitment.model.ApplicationTest"
```

- [ ] **Step 3: Implement updateCandidateDetails in Application.java**

Add method:
```java
public void updateCandidateDetails(String phone, String resumeUrl, Instant now) {
    if (this.status != ApplicationStatus.APPLIED) {
        throw new InvalidStateTransitionException(
                "Cannot update application in status " + this.status + ". Only APPLIED applications can be updated."
        );
    }
    this.phone = phone;
    this.resumeUrl = resumeUrl;
    this.updatedAt = now;
}
```

- [ ] **Step 4: Write failing unit test in ApplicationServiceTest for updateApplication**

Add tests:
- `givenExistingAppliedApplication_updateApplication_shouldPersistAndReturnUpdatedResponse`
- `givenApplicationInNonAppliedStatus_updateApplication_shouldThrowInvalidStateTransitionException`
- `givenNonExistentApplication_updateApplication_shouldThrowResourceNotFoundException`

- [ ] **Step 5: Implement updateApplication in ApplicationService and ApplicationServiceImpl**

Fetch by `findByVacancyIdAndCandidateId(vacancyId, candidateId)`, call `application.updateCandidateDetails(request.phone(), request.resumeUrl(), now)`, save and return mapped `ApplicationResponse`.

- [ ] **Step 6: Run tests and verify GREEN**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.recruitment.service.ApplicationServiceTest"
```

- [ ] **Step 7: Commit Task 2**

```powershell
git add src/main src/test
git commit -m "feat(recruitment): add application update capability in domain and service"
```

---

### Task 3: Web Flow for Application Edit and Re-submit

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/recruitment/web/ApplicationWebController.java`
- Modify: `src/main/resources/templates/applications/apply-form.html`
- Modify: `src/main/resources/messages.properties`
- Modify: `src/main/resources/messages_uk.properties`
- Modify: `src/main/resources/messages_en.properties`
- Test: `src/test/java/ua/edu/ukma/candidai/recruitment/web/ApplicationWebControllerTest.java`

- [x] **Step 1: Write failing tests in ApplicationWebControllerTest**

Add tests:
- `givenExistingApplication_showApplyForm_shouldPopulateExistingDataAndEditFlag`: verifies model has `isEdit=true`, form contains existing phone and resume, `applicationStatus="APPLIED"`, `canEdit=true`.
- `givenExistingNonAppliedApplication_showApplyForm_shouldSetCanEditFalse`: verifies `canEdit=false` when status is `REJECTED` or `INTERVIEW_SCHEDULED`.
- `givenExistingApplication_submitApplication_shouldCallUpdateApplicationAndRedirect`: verifies submitting form for already applied vacancy calls `applicationService.updateApplication` and redirects with flash message `application.apply.updated`.

- [x] **Step 2: Run tests to observe failure (RED)**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.recruitment.web.ApplicationWebControllerTest"
```

- [x] **Step 3: Update ApplicationWebController**

In `showApplyForm`:
- Look up existing application for candidate on this vacancy.
- If present: populate `form` with existing candidate phone and resumeUrl, set `isEdit=true`, `applicationStatus=app.status()`, `canEdit=(app.status() == APPLIED)`.
- If absent: keep initial empty form flow.

In `submitApplication`:
- Check if application already exists for this candidate and vacancy.
- If exists: call `applicationService.updateApplication(...)` and set flash message `application.apply.updated`.
- If absent: call `applicationService.apply(...)` with flash message `application.apply.success`.

- [x] **Step 4: Update messages and templates**

Add keys to `messages*.properties`:
- `application.apply.heading.edit`: "Редагування заявки на вакансію" / "Edit Application"
- `application.btn.update`: "Оновити заявку" / "Update Application"
- `application.apply.updated`: "Вашу заявку успішно оновлено!" / "Your application has been successfully updated!"
- `application.apply.locked`: "Заявка розглядається (статус: {0}) і більше не може бути змінена." / "Application is in review (status: {0}) and can no longer be edited."

In `apply-form.html`:
- Toggle title/heading and button text if `isEdit`.
- If `!canEdit`: render informative alert and disable inputs / hide submit button.
- If `canEdit` and `isEdit`: show badge `Поточний статус: APPLIED`.

- [x] **Step 5: Run tests and verify GREEN**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.recruitment.web.ApplicationWebControllerTest"
```

- [x] **Step 6: Commit Task 3**

```powershell
git add src/main src/test
git commit -m "feat(recruitment-web): support editing submitted applications"
```

---

### Task 4: Vacancy UI Integration for Applied Badges

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/web/VacancyWebController.java`
- Modify: `src/main/resources/templates/vacancies/list.html`
- Modify: `src/main/resources/templates/vacancies/cards.html`
- Modify: `src/main/resources/messages.properties`
- Modify: `src/main/resources/messages_uk.properties`
- Modify: `src/main/resources/messages_en.properties`
- Test: `src/test/java/ua/edu/ukma/candidai/vacancy/web/VacancyWebControllerTest.java`
- Test: `src/test/java/ua/edu/ukma/candidai/ModularityTests.java`

- [x] **Step 1: Write failing tests in VacancyWebControllerTest**

Add tests:
- `givenCandidateWithAppliedVacancies_vacancies_shouldPopulateAppliedVacanciesInModel`: verifies `model.getAttribute("appliedVacancies")` contains map of `vacancyId -> status`.
- `givenCandidateWithAppliedVacancies_vacancies_shouldRenderAppliedBadgeAndEditButton`: verifies rendered HTML contains `btn-applied-{id}` with status badge and edit link.

- [x] **Step 2: Run tests to observe failure (RED)**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.vacancy.web.VacancyWebControllerTest"
```

- [x] **Step 3: Update VacancyWebController**

Inject `Optional<CandidateApplicationLookup> candidateApplicationLookup`.
In `vacancies(...)` and `filterVacancies(...)`:
If `principal != null`:
Look up candidate profile via `userApi.getUserNotificationProfileByEmail(principal.getName())`.
If profile exists, get `candidateApplicationLookup.map(l -> l.findAppliedVacancyStatusesByCandidateId(profile.id())).orElse(Map.of())` and add to model as `appliedVacancies`.

- [x] **Step 4: Update templates & localization**

In `messages*.properties`:
- `vacancies.btn.editApplication`: "Редагувати" / "Edit"

In `list.html` and `cards.html`:
For `sec:authorize="hasRole('CANDIDATE')"`:
```html
<th:block sec:authorize="hasRole('CANDIDATE')">
    <a th:if="${appliedVacancies != null and appliedVacancies.containsKey(vac.id)}"
       th:href="@{/vacancies/{id}/apply(id=${vac.id})}"
       class="btn btn-sm btn-outline-primary"
       th:id="'btn-applied-' + ${vac.id}">
        <span class="badge bg-primary me-1" th:text="${appliedVacancies.get(vac.id)}">APPLIED</span>
        <span th:text="#{vacancies.btn.editApplication}">Редагувати</span>
    </a>
    <a th:unless="${appliedVacancies != null and appliedVacancies.containsKey(vac.id)}"
       th:href="@{/vacancies/{id}/apply(id=${vac.id})}"
       class="btn btn-sm btn-outline-success"
       th:id="'btn-apply-' + ${vac.id}"
       th:text="#{vacancies.btn.apply}">Подати заявку</a>
</th:block>
```

- [x] **Step 5: Run tests and verify GREEN**

Run:
```powershell
./gradlew.bat :test --tests "ua.edu.ukma.candidai.vacancy.web.VacancyWebControllerTest"
./gradlew.bat :test --tests "ua.edu.ukma.candidai.ModularityTests"
```

- [x] **Step 6: Commit Task 4**

```powershell
git add src/main src/test
git commit -m "feat(vacancy-web): display applied status badges and edit links for candidate"
```

---

### Task 5: Verification & Full Build

**Files:** All modified files across project.

- [ ] **Step 1: Run Checkstyle verification**

```powershell
./gradlew.bat checkstyleMain checkstyleTest
```

- [ ] **Step 2: Run full build and test suite**

```powershell
./gradlew.bat check
```

- [ ] **Step 3: Push changes to remote branch**

```powershell
git push origin feat/CA-0005-practice-7-report
```
