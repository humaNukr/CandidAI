# Vacancy Service Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the business/service layer for the Vacancy module in CandidAI per Lecture 3 requirements, featuring interface-based service isolation, moving in-memory storage & query/paging logic into repository abstraction, domain state machine transitions, duplicate checking, Mockito unit tests without Spring context, and README.md documentation.

**Architecture:**
- `VacancyController` injects `VacancyService` interface.
- `VacancyServiceImpl` implements `VacancyService` with constructor injection of `VacancyRepository`, `VacancyMapper`, and `CommonGenerator`.
- In-memory storage (`ConcurrentHashMap`), filtering by status/category, sorting by `createdAt` desc, and pagination are moved from `VacancyService` into `InMemoryVacancyRepository` implementing `VacancyRepository`.
- `VacancyStatus` defines allowed transitions via `canTransitionTo`, while `Vacancy.updateStatus` enforces them, throwing `InvalidStateTransitionException` (HTTP 422).
- Duplicate active vacancies for the same author and title are rejected via `DuplicateResourceException` (HTTP 409).
- Fast Mockito unit tests cover all business service logic without loading Spring context.
- Spring Modulith boundaries remain strictly verified with `ModularityTests`.
- Project documentation is added to `README.md` describing business rules and the state machine transition matrix.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Modulith, Lombok, MapStruct, JUnit 5, Mockito, AssertJ.

**Spec:** `docs/service-layer-architecture.md` and Lecture 3 instructions.

## Global Constraints

- Every file MUST end with a newline character (`\n`) at EOF.
- NO wildcard/star imports for regular classes. Static star imports for test utilities are allowed (`import static org.mockito.Mockito.*;`, `import static org.assertj.core.api.Assertions.*;`, `import static ua.edu.ukma.candidai.vacancy.TestResources.*;`).
- NO inline fully-qualified class names (e.g. `throw new ua.edu...`). All imports must be declared at the top of the file.
- Max line length is 120 characters (Checkstyle `LineLength`).
- Inner classes must be at the bottom of the enclosing class (`InnerTypeLast`).
- Test method naming convention: `given<Condition>_<action/method>_should<ExpectedResult>`. Method regex: `^[a-z](_?[a-zA-Z0-9]+)*$` (no double underscores `__`).
- Tests must use `TestResources` factory methods and `usingRecursiveComparison()` for full object comparisons.
- NO `any()` matchers in tests. Use exact expected objects or `verifyNoMoreInteractions(...)` on failure paths.
- All test object & page creation boilerplate must reside in `TestResources` so test methods remain declarative and clean.
- Subagents will implement the code to satisfy the tests; no service/repository implementation bodies are prescribed here.

---

### Task 1: Vacancy Status State Machine & Domain Validation

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/model/VacancyStatus.java`
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/model/Vacancy.java`
- Test: `src/test/java/ua/edu/ukma/candidai/vacancy/model/VacancyStateMachineTest.java`

**Contract Requirements:**
- `VacancyStatus`: add method `public boolean canTransitionTo(VacancyStatus target)`.
  - Matrix:
    - `DRAFT` -> `OPEN`, `ARCHIVED`
    - `OPEN` -> `PAUSED`, `CLOSED`, `ARCHIVED`
    - `PAUSED` -> `OPEN`, `CLOSED`, `ARCHIVED`
    - `CLOSED` -> `ARCHIVED`
    - `ARCHIVED` -> none (terminal state)
    - Identity transition (`this == target`) returns `true`.
- `Vacancy.updateStatus(VacancyStatus status, Instant updatedAt)`:
  - Validates `this.status.canTransitionTo(status)`.
  - If invalid, throws `InvalidStateTransitionException` with message `"Cannot transition vacancy status from " + this.status + " to " + status`.
  - If valid, updates `this.status` and `this.updatedAt`.

- [ ] **Step 1: Write `VacancyStateMachineTest.java`**

```java
package ua.edu.ukma.candidai.vacancy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

class VacancyStateMachineTest {

    @ParameterizedTest(name = "from {0} to {1} should be valid")
    @CsvSource({
            "DRAFT, OPEN",
            "DRAFT, ARCHIVED",
            "OPEN, PAUSED",
            "OPEN, CLOSED",
            "OPEN, ARCHIVED",
            "PAUSED, OPEN",
            "PAUSED, CLOSED",
            "PAUSED, ARCHIVED",
            "CLOSED, ARCHIVED",
            "OPEN, OPEN"
    })
    void givenAllowedTransition_canTransitionTo_shouldReturnTrue(VacancyStatus from, VacancyStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "from {0} to {1} should be invalid")
    @CsvSource({
            "DRAFT, PAUSED",
            "DRAFT, CLOSED",
            "CLOSED, OPEN",
            "CLOSED, PAUSED",
            "ARCHIVED, OPEN",
            "ARCHIVED, DRAFT",
            "ARCHIVED, CLOSED"
    })
    void givenForbiddenTransition_canTransitionTo_shouldReturnFalse(VacancyStatus from, VacancyStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    @DisplayName("updateStatus with invalid transition should throw InvalidStateTransitionException")
    void givenForbiddenTransition_updateStatus_shouldThrowInvalidStateTransitionException() {
        Vacancy vacancy = aVacancy(VacancyStatus.CLOSED);

        assertThatThrownBy(() -> vacancy.updateStatus(VacancyStatus.OPEN, DEFAULT_NOW))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition vacancy status from CLOSED to OPEN");
    }

    @Test
    @DisplayName("updateStatus with valid transition should update status and timestamp")
    void givenAllowedTransition_updateStatus_shouldUpdateStatusAndTimestamp() {
        Vacancy vacancy = aVacancy();

        vacancy.updateStatus(VacancyStatus.PAUSED, DEFAULT_NOW);

        Vacancy expected = aVacancy(VacancyStatus.PAUSED, DEFAULT_NOW);
        assertThat(vacancy)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
```

- [ ] **Step 2: Subagent implements `VacancyStatus.canTransitionTo` and `Vacancy.updateStatus`**

Subagent updates `VacancyStatus.java` and `Vacancy.java` to fulfill the contract and pass all tests.

- [ ] **Step 3: Verify test and Checkstyle**

Run: `./gradlew test --tests "ua.edu.ukma.candidai.vacancy.model.VacancyStateMachineTest"`
Run: `./gradlew checkstyleMain checkstyleTest`
Expected: PASS.

---

### Task 2: Move Storage & Query Logic to Repository (`VacancyRepository` & `InMemoryVacancyRepository`)

**Files:**
- Create: `src/main/java/ua/edu/ukma/candidai/vacancy/repository/VacancyRepository.java`
- Create: `src/main/java/ua/edu/ukma/candidai/vacancy/repository/InMemoryVacancyRepository.java`

**Contract Requirements:**
- `VacancyRepository` interface:
  ```java
  package ua.edu.ukma.candidai.vacancy.repository;

  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.Pageable;
  import ua.edu.ukma.candidai.vacancy.model.JobCategory;
  import ua.edu.ukma.candidai.vacancy.model.Vacancy;
  import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

  import java.util.Optional;
  import java.util.UUID;

  public interface VacancyRepository {

      Vacancy save(Vacancy vacancy);

      Optional<Vacancy> findById(UUID id);

      Page<Vacancy> findAll(VacancyStatus status, JobCategory category, Pageable pageable);

      boolean existsActiveByAuthorIdAndTitle(UUID authorId, String title);
  }
  ```
- `InMemoryVacancyRepository`:
  - Package-private `@Repository`.
  - Takes over the `ConcurrentHashMap<UUID, Vacancy>` from `VacancyService`.
  - Implements `save(Vacancy vacancy)`.
  - Implements `findById(UUID id)`.
  - Implements `findAll(VacancyStatus status, JobCategory category, Pageable pageable)`: filters `!v.isDeleted()`, matches `status` and `category` if provided, sorts by `createdAt` desc (or per `pageable.getSort()`), and applies pagination offset/limit, returning `Page<Vacancy>`.
  - Implements `existsActiveByAuthorIdAndTitle(UUID authorId, String title)`: checks `!v.isDeleted() && v.getStatus() == VacancyStatus.OPEN && authorId.equals(v.getAuthorId()) && v.getTitle().equalsIgnoreCase(title.trim())`.

- [ ] **Step 1: Subagent implements `VacancyRepository` and `InMemoryVacancyRepository`**

Subagent creates the repository interface and the in-memory implementation moving the data storage, sorting, filtering, and pagination from `VacancyService`.

- [ ] **Step 2: Verify compilation and Checkstyle**

Run: `./gradlew compileJava checkstyleMain`
Expected: PASS.

---

### Task 3: Interface Isolation & Mockito Service Testing (`VacancyService` & `VacancyServiceImpl`)

**Files:**
- Modify: `src/main/java/ua/edu/ukma/candidai/vacancy/service/VacancyService.java` (interface)
- Create: `src/main/java/ua/edu/ukma/candidai/vacancy/service/VacancyServiceImpl.java` (implementation)
- Verify: `src/main/java/ua/edu/ukma/candidai/vacancy/service/VacancyApiImpl.java`
- Verify: `src/main/java/ua/edu/ukma/candidai/vacancy/controller/VacancyController.java`
- Test: `src/test/java/ua/edu/ukma/candidai/vacancy/service/VacancyServiceTest.java`

**Contract Requirements:**
- `VacancyService`:
  ```java
  package ua.edu.ukma.candidai.vacancy.service;

  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.Pageable;
  import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
  import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
  import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
  import ua.edu.ukma.candidai.vacancy.model.JobCategory;
  import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

  import java.util.UUID;

  public interface VacancyService {

      VacancyResponse createVacancy(CreateVacancyRequest request);

      Page<VacancyResponse> getAllVacancies(VacancyStatus status, JobCategory category, Pageable pageable);

      VacancyResponse getVacancyById(UUID id);

      VacancyResponse updateVacancyStatus(UUID id, UpdateVacancyStatusRequest request);

      void deleteVacancy(UUID id);
  }
  ```
- `VacancyServiceImpl`:
  - Package-private `@Service`, `@RequiredArgsConstructor`.
  - Injects `VacancyRepository`, `VacancyMapper`, `CommonGenerator`.
  - `createVacancy`: checks `vacancyRepository.existsActiveByAuthorIdAndTitle(request.authorId(), request.title())`. Throws `DuplicateResourceException` if true. Otherwise calls `generator.uuid()`, `generator.now()`, creates vacancy, saves in repository, maps to response.
  - `getVacancyById`: finds in repository, throws `ResourceNotFoundException` if absent or deleted, maps to response.
  - `updateVacancyStatus`: finds in repository, calls `vacancy.updateStatus(request.status(), generator.now())`, saves in repository, maps to response.
  - `deleteVacancy`: finds in repository, calls `vacancy.softDelete(generator.now())`, saves in repository.
  - `getAllVacancies`: calls `vacancyRepository.findAll(status, category, pageable).map(vacancyMapper::toResponse)`.

- [ ] **Step 1: Write `VacancyServiceTest.java`**

```java
package ua.edu.ukma.candidai.vacancy.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.repository.VacancyRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private VacancyMapper vacancyMapper;

    @Mock
    private CommonGenerator generator;

    @InjectMocks
    private VacancyServiceImpl vacancyService;

    @Test
    @DisplayName("createVacancy with unique title should save vacancy and return response")
    void givenUniqueTitle_createVacancy_shouldSaveAndReturnResponse() {
        CreateVacancyRequest request = validCreateVacancyRequest();
        Vacancy vacancy = aVacancy();
        VacancyResponse expectedResponse = aVacancyResponse();

        when(vacancyRepository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, request.title())).thenReturn(false);
        when(generator.uuid()).thenReturn(DEFAULT_ID);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(vacancyRepository.save(vacancy)).thenReturn(vacancy);
        when(vacancyMapper.toResponse(vacancy)).thenReturn(expectedResponse);

        VacancyResponse actual = vacancyService.createVacancy(request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
        verify(vacancyRepository).save(vacancy);
    }

    @Test
    @DisplayName("createVacancy with duplicate title for author should throw DuplicateResourceException")
    void givenDuplicateTitleForAuthor_createVacancy_shouldThrowDuplicateResourceException() {
        CreateVacancyRequest request = validCreateVacancyRequest();
        when(vacancyRepository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, request.title())).thenReturn(true);

        assertThatThrownBy(() -> vacancyService.createVacancy(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Active vacancy with title 'Senior Java Engineer' already exists");

        verifyNoMoreInteractions(vacancyRepository);
    }

    @Test
    @DisplayName("getVacancyById with existing active vacancy should return response")
    void givenExistingId_getVacancyById_shouldReturnResponse() {
        Vacancy vacancy = aVacancy();
        VacancyResponse expectedResponse = aVacancyResponse();

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(vacancy));
        when(vacancyMapper.toResponse(vacancy)).thenReturn(expectedResponse);

        VacancyResponse actual = vacancyService.getVacancyById(DEFAULT_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("getVacancyById with non-existent id should throw ResourceNotFoundException")
    void givenNonExistentId_getVacancyById_shouldThrowResourceNotFoundException() {
        when(vacancyRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacancyService.getVacancyById(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found with id: " + NON_EXISTENT_ID);
    }

    @Test
    @DisplayName("getVacancyById with soft-deleted vacancy should throw ResourceNotFoundException")
    void givenDeletedVacancy_getVacancyById_shouldThrowResourceNotFoundException() {
        Vacancy deletedVacancy = aDeletedVacancy();
        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(deletedVacancy));

        assertThatThrownBy(() -> vacancyService.getVacancyById(DEFAULT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found with id: " + DEFAULT_ID);
    }

    @Test
    @DisplayName("updateVacancyStatus with valid transition should update status and save")
    void givenValidStateTransition_updateVacancyStatus_shouldUpdateAndReturnResponse() {
        Vacancy vacancy = aVacancy();
        UpdateVacancyStatusRequest request = new UpdateVacancyStatusRequest(VacancyStatus.PAUSED);
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(vacancy));
        when(generator.now()).thenReturn(updatedAt);

        Vacancy expectedSavedVacancy = aVacancy(VacancyStatus.PAUSED, updatedAt);
        VacancyResponse expectedResponse = aVacancyResponse(VacancyStatus.PAUSED, updatedAt);

        when(vacancyRepository.save(expectedSavedVacancy)).thenReturn(expectedSavedVacancy);
        when(vacancyMapper.toResponse(expectedSavedVacancy)).thenReturn(expectedResponse);

        VacancyResponse actual = vacancyService.updateVacancyStatus(DEFAULT_ID, request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
        verify(vacancyRepository).save(expectedSavedVacancy);
    }

    @Test
    @DisplayName("updateVacancyStatus with invalid transition should throw InvalidStateTransitionException")
    void givenInvalidStateTransition_updateVacancyStatus_shouldThrowInvalidStateTransitionException() {
        Vacancy closedVacancy = aVacancy(VacancyStatus.CLOSED);
        UpdateVacancyStatusRequest request = new UpdateVacancyStatusRequest(VacancyStatus.OPEN);

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(closedVacancy));

        assertThatThrownBy(() -> vacancyService.updateVacancyStatus(DEFAULT_ID, request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition vacancy status from CLOSED to OPEN");

        verifyNoMoreInteractions(vacancyRepository);
    }

    @Test
    @DisplayName("deleteVacancy with existing id should mark as deleted and save")
    void givenExistingId_deleteVacancy_shouldSoftDelete() {
        Vacancy vacancy = aVacancy();
        Instant deletedAt = DEFAULT_NOW.plusSeconds(7200);

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(vacancy));
        when(generator.now()).thenReturn(deletedAt);

        Vacancy expectedDeletedVacancy = aDeletedVacancy(deletedAt);

        vacancyService.deleteVacancy(DEFAULT_ID);

        verify(vacancyRepository).save(expectedDeletedVacancy);
    }

    @Test
    @DisplayName("deleteVacancy with non-existent id should throw ResourceNotFoundException")
    void givenNonExistentId_deleteVacancy_shouldThrowResourceNotFoundException() {
        when(vacancyRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacancyService.deleteVacancy(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found with id: " + NON_EXISTENT_ID);

        verifyNoMoreInteractions(vacancyRepository);
    }

    @Test
    @DisplayName("getAllVacancies should return mapped page from repository")
    void givenFiltersAndPageable_getAllVacancies_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Vacancy vacancy = aVacancy();
        VacancyResponse expectedResponse = aVacancyResponse();
        Page<Vacancy> vacancyPage = aVacancyPage(List.of(vacancy), pageable);

        when(vacancyRepository.findAll(VacancyStatus.OPEN, JobCategory.ENGINEERING, pageable))
                .thenReturn(vacancyPage);
        when(vacancyMapper.toResponse(vacancy)).thenReturn(expectedResponse);

        Page<VacancyResponse> actual = vacancyService.getAllVacancies(
                VacancyStatus.OPEN,
                JobCategory.ENGINEERING,
                pageable
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(aVacancyResponsePage(List.of(expectedResponse), pageable));
    }
}
```

- [ ] **Step 2: Subagent extracts `VacancyService` interface and implements `VacancyServiceImpl`**

Subagent converts `VacancyService.java` into an interface and implements `VacancyServiceImpl.java` to fulfill the contract and pass `VacancyServiceTest`.

- [ ] **Step 3: Run controller and service tests**

Run: `./gradlew test --tests "ua.edu.ukma.candidai.vacancy.controller.VacancyControllerTest"`
Run: `./gradlew test --tests "ua.edu.ukma.candidai.vacancy.service.VacancyServiceTest"`
Expected: PASS.

- [ ] **Step 4: Verify Checkstyle**

Run: `./gradlew checkstyleMain checkstyleTest`
Expected: PASS.

---

### Task 4: Documentation in `README.md`

**Files:**
- Create/Modify: `README.md`

**Requirements:**
Per teacher criteria: *"Оформлення документації у файлі README.md з описом бізнес-правил та матриці переходів станів"*.
- Document the service layer architecture (Controller -> Service interface -> Repository interface -> In-memory implementation).
- Document business rules:
  - Duplicate check (authorId + title for open active vacancies -> HTTP 409 Conflict).
  - Soft-delete strategy.
  - Inter-module contract `VacancyApi` and encapsulation.
- Document the formal state machine matrix for `VacancyStatus` with allowed and forbidden transitions.

- [ ] **Step 1: Write `README.md`**

Create `README.md` documenting architecture, business rules, and state machine transitions.

- [ ] **Step 2: Verify Checkstyle and git status**

Run: `./gradlew check`
Expected: PASS.

---

### Task 5: Full Verification & Modularity Tests

**Files:**
- Architecture test: `ModularityTests.java`
- Entire build: `./gradlew check`

- [ ] **Step 1: Execute full checkstyle and test suite**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL with 0 Checkstyle violations and 100% tests passing.

- [ ] **Step 2: Re-run tests with cache disabled**

Run: `./gradlew test --rerun-tasks`
Expected: BUILD SUCCESSFUL without loading full Spring context in unit tests.
