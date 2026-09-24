# Сервісний шар CandidAI та розподіл завдань для команди (ДЗ Лекція 3)

Цей документ пояснює, як ми переходимо від контролерів та in-memory мап до **повноцінного бізнес-шару (Service Layer)**: як організувати інтерфейси, як працює скінченний автомат переходів статусів (State Machine), як красиво реалізувати патерн «Стратегія» без `@Qualifier`, як працюють асинхронні доменні події у Spring Modulith та як ми рівномірно ділимо це завдання на трьох людей.

---

## 1. Що ми будуємо у 2-му завданні?
У 1-му завданні ми написали веб-шар (REST контролери, DTO, валідацію, RFC 7807/9457 ProblemDetail). Зараз ми оживляємо систему реальною бізнес-логікою:
* **Ізоляція через інтерфейси:** Контролери більше нічого не знають про реалізацію і не тримають мапи — вони звертаються виключно до `*Service` інтерфейсів.
* **Суворі бізнес-правила та перевірка дублікатів:** Захист від повторної подачі заявки на ту саму вакансію (`409 Conflict`), контроль відкритих/закритих вакансій.
* **Формальна State Machine (Скінченний автомат переходів):** Заявка та вакансія не можуть стрибати у статусах як заманеться (наприклад, не можна перевести заявку з `APPLIED` відразу в `OFFER` без `SCREENING`/`INTERVIEW`). Некоректні переходи відхиляються з `422 Unprocessable Entity`.
* **Патерн «Стратегія» (Strategy Pattern):** Варіативні алгоритми (скоринг/оцінка кандидатів або стратегії публікації/валідації вакансій), які автоматично збираються Spring через `List<Strategy>` без використання хардкодних `@Qualifier`.
* **Асинхронні доменні події Spring Modulith (`@ApplicationModuleListener`):** Відправка нотифікацій та фонові дії виконуються асинхронно без уповільнення основного HTTP-запиту.
* **Швидкі модульні тести Mockito:** Тестування сервісів через `@ExtendWith(MockitoExtension.class)` без повільного підняття повного Spring-контексту.

---

## 2. Архітектурні вимоги Лекції 3 (Checklist перед початком)

Кожен клас у сервісному шарі повинен суворо відповідати правилам:
1. **Інтерфейсні контракти:**
   * Контролер інжектить інтерфейс `ApplicationService`, а не конкретний клас `ApplicationServiceImpl`.
   * Сервіс інжектить інтерфейс репозиторію/сховища (`ApplicationRepository`).
2. **Stateless Singleton & Constructor Injection:**
   * Сервіси не містять змінного стану полів (жодних `count++` чи спільних списків у полях екземпляра). Стан живе тільки у сховищі (`Repository` / `ConcurrentHashMap`).
   * Всі залежності — `private final` через конструктор (або Lombok `@RequiredArgsConstructor`). Жодного інжекту через поля (`@Autowired private ...` заборонено!).
3. **Типізовані доменні винятки:**
   * Спроба створити дублікат -> `DuplicateResourceException` (мапиться в `HTTP 409 Conflict`).
   * Спроба невалідного переходу статусу -> `InvalidStateTransitionException` (мапиться в `HTTP 422 Unprocessable Entity`).
   * Відсутність сутності -> `ResourceNotFoundException` (мапиться в `HTTP 404 Not Found`).
4. **Spring Modulith Event-Driven Architecture:**
   * Модуль, де відбулася зміна, викликає `ApplicationEventPublisher.publishEvent(...)`.
   * Модуль слухач обробляє подію через асинхронний `@ApplicationModuleListener` (окрема транзакція/потік, повне дотримання кордонів модулів).
5. **Архітектурний тест:**
   * Тест `ApplicationModules.of(CandidAiApplication.class).verify()` повинен проходити без помилок циркулярних залежностей.

---

## 3. Матриця переходів станів (State Machine)

Одне з головних бізнес-правил — захист воронки відбору та життєвого циклу вакансії від хаосу:

### А. Воронка відбору кандидата (`ApplicationStatus`)
```
                  ┌───────────────┐
                  │    APPLIED    │
                  └───────┬───────┘
                          │
                          ▼
                  ┌───────────────┐
            ┌────►│   SCREENING   ├────┐
            │     └───────┬───────┘    │
            │             │            │
            │             ▼            │
            │     ┌───────────────┐    │
            │     │   INTERVIEW   │    │
            │     └───────┬───────┘    │
            │             │            │
            │      ┌──────┴──────┐     │
            │      ▼             ▼     │
            │ ┌─────────┐   ┌────────┐ │
            │ │  OFFER  │   │REJECTED│◄┘
            │ └────┬────┘   └────────┘
            │      │
            │      ▼
            │ ┌─────────┐
            └─┤  HIRED  │
              └─────────┘
```
* **Дозволені переходи:**
  * `APPLIED` $\rightarrow$ `SCREENING`, `REJECTED`
  * `SCREENING` $\rightarrow$ `INTERVIEW`, `REJECTED`
  * `INTERVIEW` $\rightarrow$ `OFFER`, `REJECTED`
  * `OFFER` $\rightarrow$ `HIRED`, `REJECTED`
  * Будь-який інший перехід (наприклад, `APPLIED` $\rightarrow$ `OFFER` чи спроба змінити статус після `REJECTED` або `HIRED`) викликає `InvalidStateTransitionException` (`422 Unprocessable Entity`).

### Б. Життєвий цикл вакансії (`VacancyStatus`)
* `DRAFT` $\rightarrow$ `OPEN`, `ARCHIVED`
* `OPEN` $\rightarrow$ `PAUSED`, `CLOSED`, `ARCHIVED`
* `PAUSED` $\rightarrow$ `OPEN`, `CLOSED`, `ARCHIVED`
* `CLOSED` $\rightarrow$ `ARCHIVED`

---

## 4. Патерн «Стратегія» без `@Qualifier`

Щоб виконати вимогу лекції: *"Винесення варіативної частини бізнес-логіки в окремі компоненти патерну «Стратегія» з автоматичним впровадженням колекції Spring без `@Qualifier`"*:

Ми створюємо доменний інтерфейс стратегії, який сам знає, для якого типу він підходить:
```java
public interface CandidateEvaluationStrategy {
    boolean supports(JobCategory category);
    EvaluationResult evaluate(Application application, List<InterviewFeedback> feedbacks);
}
```

Конкретні реалізації позначаються як звичайні Spring `@Component`:
1. `EngineeringEvaluationStrategy implements CandidateEvaluationStrategy` (перевіряє хард-скіли, оцінку за кодінг, строгий бал $\ge 4$).
2. `ManagementEvaluationStrategy implements CandidateEvaluationStrategy` (робить акцент на софт-скіли, лідерство, комунікацію).
3. `GeneralEvaluationStrategy implements CandidateEvaluationStrategy` (дефолтна/базова стратегія для інших напрямків).

У сервісі ми просто інжектимо весь список:
```java
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final List<CandidateEvaluationStrategy> evaluationStrategies; // Spring інжектить усі бsearch-компоненти автоматично!

    public EvaluationResult evaluateCandidate(UUID applicationId) {
        Application app = findOrThrow(applicationId);
        JobCategory category = vacancyApi.getCategory(app.getVacancyId());

        CandidateEvaluationStrategy strategy = evaluationStrategies.stream()
                .filter(s -> s.supports(category))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No strategy found for category: " + category));

        return strategy.evaluate(app, feedbacks);
    }
}
```
> **Чистий код:** Жодного `switch-case`, жодного `@Qualifier("engineeringStrategy")`. Нова категорія додається просто створенням нового біна!

---

## 5. Розподіл завдань між 3 членами команди

Розподіл залишається **семантично тим самим**, що й у 1-му ДЗ, але тепер кожен учасник реалізує повноцінний бізнес-шар для своєї частини.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        CandidAI Service & Domain Layer                                 │
├─────────────────────────┬───────────────────────────────┬──────────────────────────────┤
│       Учасник 1         │          Учасник 2            │          Учасник 3           │
│   (Сервіс вакансій та   │   (Сервіс подання заявок та   │  (Воронка статусів, фідбек   │
│   стратегії публікації) │    перевірка дублікатів)      │      та ШІ-оцінювання)       │
│     Модуль: vacancy     │      Модуль: recruitment      │     Модуль: recruitment      │
├─────────────────────────┼───────────────────────────────┼──────────────────────────────┤
│ • VacancyService & Repo │ • ApplicationService & Repo   │ • State Machine переходів    │
│ • Vacancy State Machine │ • Бізнес-правило дублікатів   │ • Патерн «Стратегія» оцінки  │
│ • Публічний VacancyApi  │ • Подія ApplicationSubmitted  │ • Асинхронний Listener подій │
│ • Mockito Unit Tests    │ • Mockito Unit Tests          │ • Mockito Unit Tests         │
└─────────────────────────┴───────────────────────────────┴──────────────────────────────┘
```

---

### 👤 Розробник 1: Сервіс управління вакансіями та їхній життєвий цикл
* **Пакет:** `ua.edu.ukma.candidai.vacancy.service`
* **Що реалізувати:**
  1. **Інтерфейс та реалізація:**
     * `VacancyService` (інтерфейс) $\rightarrow$ `VacancyServiceImpl` (імплементація).
     * Репозиторій: винести роботу зі сховищем в інтерфейс `VacancyRepository` (поки що на базі `ConcurrentHashMap`).
  2. **Два бізнес-правила для вакансій:**
     * **Унікальність:** перевірка, що в компанії/автора немає активної відкритої вакансії з точно таким самим `title` (якщо є $\rightarrow$ викидати `DuplicateResourceException` $\rightarrow$ `409 Conflict`).
     * **State Machine:** валідація переходів статусу вакансії (`DRAFT -> OPEN -> PAUSED -> CLOSED -> ARCHIVED`). Спроба закрити вже архівовану вакансію $\rightarrow$ `InvalidStateTransitionException` (`422 Unprocessable Entity`).
  3. **Публічний інтерфейс модуля для інших (`VacancyApi`):**
     * Створити `public interface VacancyApi` у пакеті `ua.edu.ukma.candidai.vacancy`:
       ```java
       public interface VacancyApi {
           boolean isVacancyOpen(UUID vacancyId);
           JobCategory getVacancyCategory(UUID vacancyId);
       }
       ```
     * Надати реалізацію `VacancyApiImpl` (щоб Розробник 2 та 3 могли викликати його без порушення кордонів Spring Modulith).
  4. **Тести:** `VacancyServiceTest` на базі `@ExtendWith(MockitoExtension.class)` (перевірка бізнес-правил, перевірка винятків `409` та `422`).

---

### 👤 Розробник 2: Сервіс подання заявок та контроль дублікатів
* **Пакет:** `ua.edu.ukma.candidai.recruitment.service`
* **Що реалізувати:**
  1. **Інтерфейс та реалізація:**
     * `ApplicationRegistrationService` (або частина `ApplicationService`) $\rightarrow$ `ApplicationServiceImpl`.
     * Репозиторій: `ApplicationRepository` (контракт та in-memory реалізація).
  2. **Два ключових бізнес-правила:**
     * **Перевірка відкритості вакансії:** через виклик `VacancyApi.isVacancyOpen(vacancyId)`. Якщо вакансія закрита або не існує $\rightarrow$ викидати `ResourceNotFoundException` або бізнес-помилку `VacancyClosedException`.
     * **Захист від дублікатів заявок:** один і той самий кандидат (перевірка за `email + vacancyId`) **не може податися двічі** на одну й ту саму вакансію! Якщо заявка вже є $\rightarrow$ викидати `DuplicateResourceException` (`409 Conflict`).
  3. **Публікація події подання заявки (Event-Driven):**
     * Створити доменну подію `public record ApplicationSubmittedEvent(UUID applicationId, UUID vacancyId, String email, Instant submittedAt) {}`.
     * Інжектувати `ApplicationEventPublisher` і публікувати цю подію після успішного збереження заявки.
  4. **Тести:** `ApplicationServiceTest` на базі `@ExtendWith(MockitoExtension.class)`:
     * Успішне створення заявки та перевірка відправки події (`verify(eventPublisher).publishEvent(...)`).
     * Викидання `409 Conflict` при спробі повторної подачі з тим самим email.
     * Викидання помилки, якщо вакансія закрита.

---

### 👤 Розробник 3: Воронка переходів статусів, патерн «Стратегія» та обробник подій
* **Пакет:** `ua.edu.ukma.candidai.recruitment.service`
* **Що реалізувати:**
  1. **State Machine воронки відбору:**
     * Метод `updateStatus(UUID applicationId, ApplicationStatus newStatus, String comment)`.
     * Сувора валідація матриці переходів:
       `APPLIED -> SCREENING -> INTERVIEW -> OFFER -> HIRED` (або `REJECTED` на будь-якому кроці).
     * Неприпустимий перехід (наприклад, `APPLIED -> OFFER` або зміна після `REJECTED`) $\rightarrow$ викидає `InvalidStateTransitionException` (`422 Unprocessable Entity`).
  2. **Патерн «Стратегія» для оцінювання кандидатів (без `@Qualifier`):**
     * Інтерфейс `CandidateEvaluationStrategy`:
       * Метод `boolean supports(JobCategory category)`.
       * Метод `EvaluationDecision evaluate(...)`.
     * Реалізувати 2–3 конкретні бін-стратегії:
       * `EngineeringEvaluationStrategy`
       * `ManagementEvaluationStrategy`
       * `GeneralEvaluationStrategy`
     * Інжектувати `List<CandidateEvaluationStrategy>` у сервіс фідбеків/оцінки та обирати потрібну стратегію динамічно через `.stream().filter(s -> s.supports(...))`.
  3. **Асинхронний слухач подій (`@ApplicationModuleListener`):**
     * Створити компонент `ApplicationEventListener` (наприклад, для логування аудиту або симуляції відправки email):
       ```java
       @Component
       @RequiredArgsConstructor
       public class ApplicationNotificationListener {
           @ApplicationModuleListener
           public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
               log.info("Async processing new application: {} for email: {}", event.applicationId(), event.email());
           }
       }
       ```
  4. **Тести:**
     * `ApplicationStateMachineTest` (перевірка дозволених та блокування заборонених переходів).
     * `CandidateEvaluationStrategyTest` (перевірка коректного вибору та роботи стратегій).

---

## 6. Спільний фундамент (Робимо ДО розходження по гілках)

Перед стартом роботи в гілку `main` (або базову гілку другого етапу `feat/service-layer`) додаємо спільні класи:

1. **Нові типізовані винятки у `ua.edu.ukma.candidai.common.exception`:**
   * `DuplicateResourceException.java` (для помилок дублювання даних).
   * `InvalidStateTransitionException.java` (для некоректних переходів станів).
2. **Оновлення `GlobalExceptionHandler.java`:**
   ```java
   @ExceptionHandler(DuplicateResourceException.class)
   public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
       ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
       problem.setTitle("Resource Conflict");
       return problem;
   }

   @ExceptionHandler(InvalidStateTransitionException.class)
   public ProblemDetail handleInvalidTransition(InvalidStateTransitionException ex) {
       ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
       problem.setTitle("Invalid State Transition");
       return problem;
   }
   ```
3. **Оновлення `build.gradle.kts` (якщо потрібно):**
   * Переконатися, що підключено `org.springframework.modulith:spring-modulith-starter-core` (для `@ApplicationModuleListener`).

---

## 7. План Git Workflow для 2-го ДЗ

```text
main
 │
 └──> feat/service-layer (Спільна базова гілка 2-го завдання)
       │
       │─── [Коміт 1: Спільні винятки 409/422 та оновлення GlobalExceptionHandler]
       │
       ├───> feat/service-vacancies    (Гілка Розробника 1) ──┐
       ├───> feat/service-applications (Гілка Розробника 2) ──┼──> PRs у feat/service-layer
       └───> feat/service-statemachine (Гілка Розробника 3) ──┘
```

### Як перевіряємо готовність:
1. Кожен пише швидкі тести з **Mockito** (`@ExtendWith(MockitoExtension.class)`):
   ```bash
   ./gradlew test
   ```
   *(Усі тести виконуються за лічені секунди, бо не піднімають важкий Spring ApplicationContext).*
2. Перевірка архітектури Modulith:
   ```java
   @Test
   void verifyModularity() {
       ApplicationModules.of(CandidAiApplication.class).verify();
   }
   ```
3. Перевірка стилю коду:
   ```bash
   ./gradlew checkstyleMain checkstyleTest
   ```
4. Зливаємо всі PR-и в `feat/service-layer`, проганяємо `./gradlew check` і відкриваємо фінальний PR у `main`.
