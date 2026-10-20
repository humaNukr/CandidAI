package ua.edu.ukma.candidai.vacancy.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.web.dto.CreateVacancyForm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class VacancyWebTestResources {

    public static final String VACANCIES_URL = "/vacancies";
    public static final String NEW_VACANCY_URL = "/vacancies/new";

    public static final UUID RECRUITER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final String RECRUITER_EMAIL = "recruiter@candidai.com";

    public static final UserNotificationProfile RECRUITER_PROFILE = new UserNotificationProfile(
            RECRUITER_ID,
            "Anna Recruiter",
            RECRUITER_EMAIL,
            "12345678"
    );

    public static final CreateVacancyForm VALID_FORM = CreateVacancyForm.builder()
            .title("Lead Kotlin Architect")
            .category(JobCategory.ENGINEERING)
            .specialization("Backend Development")
            .seniorityLevel("Lead")
            .minYearsOfExperience(7)
            .description("Leading high-load backend architecture and distributed AI microservices.")
            .requiredSkills("Kotlin, Spring Boot, Kafka, PostgreSQL")
            .preferredSkills("Docker, Kubernetes, AWS")
            .minEnglishLevel(EnglishLevel.C1)
            .salaryMin(new BigDecimal("6000.00"))
            .salaryMax(new BigDecimal("8000.00"))
            .currency("USD")
            .employmentType(EmploymentType.FULL_TIME)
            .locationType(LocationType.REMOTE)
            .location("Kyiv, Ukraine")
            .build();

    public static final CreateVacancyRequest EXPECTED_CREATE_REQUEST = new CreateVacancyRequest(
            RECRUITER_ID,
            RECRUITER_ID,
            null,
            VacancyStatus.OPEN,
            "Lead Kotlin Architect",
            JobCategory.ENGINEERING,
            "Backend Development",
            "Lead",
            7,
            "Leading high-load backend architecture and distributed AI microservices.",
            List.of("Kotlin", "Spring Boot", "Kafka", "PostgreSQL"),
            List.of("Docker", "Kubernetes", "AWS"),
            EnglishLevel.C1,
            new BigDecimal("6000.00"),
            new BigDecimal("8000.00"),
            "USD",
            EmploymentType.FULL_TIME,
            LocationType.REMOTE,
            "Kyiv, Ukraine",
            null
    );

    public static final CreateVacancyForm INVALID_FORM = CreateVacancyForm.builder()
            .title("a")
            .category(null)
            .specialization("")
            .seniorityLevel("")
            .minYearsOfExperience(-1)
            .description("short")
            .requiredSkills("")
            .preferredSkills("")
            .minEnglishLevel(null)
            .salaryMin(new BigDecimal("-100"))
            .salaryMax(new BigDecimal("-200"))
            .currency("U")
            .employmentType(null)
            .locationType(null)
            .location("")
            .build();

    public static final CreateVacancyForm INVALID_SALARY_RANGE_FORM = VALID_FORM.toBuilder()
            .salaryMin(new BigDecimal("7000.00"))
            .salaryMax(new BigDecimal("5000.00"))
            .build();

    public static final CreateVacancyForm XSS_FORM = VALID_FORM.toBuilder()
            .title("XSS <script>alert('title')</script>")
            .description("Safe description with <script>alert(1)</script>")
            .build();

    public static final VacancyResponse VACANCY_RESPONSE_1 = new VacancyResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            RECRUITER_ID,
            RECRUITER_ID,
            null,
            "Senior Java Developer",
            JobCategory.ENGINEERING,
            "Backend",
            "Senior",
            5,
            "Designing modern microservices with Spring Boot.",
            List.of("Java 25", "Spring Boot"),
            List.of("PostgreSQL"),
            EnglishLevel.B2,
            new BigDecimal("4500.00"),
            new BigDecimal("6000.00"),
            "USD",
            EmploymentType.FULL_TIME,
            LocationType.REMOTE,
            "Kyiv",
            VacancyStatus.OPEN,
            Instant.parse("2026-10-01T12:00:00Z"),
            null,
            Instant.parse("2026-10-01T12:00:00Z"),
            Instant.parse("2026-10-01T12:00:00Z")
    );

    public static final VacancyResponse XSS_VACANCY_RESPONSE = new VacancyResponse(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            RECRUITER_ID,
            RECRUITER_ID,
            null,
            "XSS <script>alert('title')</script>",
            JobCategory.ENGINEERING,
            "Security",
            "Middle",
            3,
            "Safe description with <script>alert(1)</script>",
            List.of("Java"),
            List.of("Security"),
            EnglishLevel.B2,
            new BigDecimal("3000.00"),
            new BigDecimal("4000.00"),
            "USD",
            EmploymentType.FULL_TIME,
            LocationType.REMOTE,
            "Kyiv",
            VacancyStatus.OPEN,
            Instant.parse("2026-10-01T12:00:00Z"),
            null,
            Instant.parse("2026-10-01T12:00:00Z"),
            Instant.parse("2026-10-01T12:00:00Z")
    );

    public static final Page<VacancyResponse> SINGLE_PAGE_VACANCIES = new PageImpl<>(
            List.of(VACANCY_RESPONSE_1),
            PageRequest.of(0, 10),
            1
    );

    public static final Page<VacancyResponse> XSS_PAGE_VACANCIES = new PageImpl<>(
            List.of(XSS_VACANCY_RESPONSE),
            PageRequest.of(0, 10),
            1
    );

    private VacancyWebTestResources() {
    }
}
