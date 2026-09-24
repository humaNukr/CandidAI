package ua.edu.ukma.candidai.vacancy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TestResources {

    public static final String BASE_URL = "/api/v1/vacancies";
    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DEFAULT_AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID DEFAULT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID NON_EXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final BigDecimal DEFAULT_SALARY_MIN = BigDecimal.valueOf(3000);
    public static final BigDecimal DEFAULT_SALARY_MAX = BigDecimal.valueOf(5000);
    public static final Instant DEFAULT_NOW = Instant.parse("2026-09-12T10:00:00Z");

    public static final String JSON_WITH_UNKNOWN_PROPERTY = """
            {
                "unknownField": "bad"
            }
            """;

    public static final String VALIDATION_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/validation",
                "title": "Validation Error",
                "status": 400,
                "detail": "Input validation failed"
            }
            """;

    public static final String INVALID_SALARY_RANGE_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/validation",
                "title": "Validation Error",
                "status": 400,
                "detail": "Input validation failed",
                "errors": {
                    "salaryMin": "Minimum salary cannot be greater than maximum salary"
                }
            }
            """;

    public static final String JSON_PARSING_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/bad-request",
                "title": "JSON Parsing Error",
                "status": 400,
                "detail": "Malformed request body or unknown properties"
            }
            """;

    public static String notFoundProblemDetailJson(UUID id) {
        return """
                {
                    "type": "https://candidai.ukma.edu.ua/errors/not-found",
                    "title": "Resource Not Found",
                    "status": 404,
                    "detail": "Vacancy not found with id: %s"
                }
                """.formatted(id);
    }

    public static CreateVacancyRequestBuilder aCreateVacancyRequest() {
        return new CreateVacancyRequestBuilder();
    }

    public static CreateVacancyRequest validCreateVacancyRequest() {
        return aCreateVacancyRequest().build();
    }

    public static CreateVacancyRequest validCreateDraftVacancyRequest() {
        return aCreateVacancyRequest().status(VacancyStatus.DRAFT).build();
    }

    public static Vacancy aVacancy() {
        return aVacancyBuilder().build();
    }

    public static Vacancy aDraftVacancy() {
        return aVacancy(VacancyStatus.DRAFT);
    }

    public static Vacancy aVacancy(VacancyStatus status) {
        return aVacancyBuilder()
                .status(status)
                .publishedAt(status == VacancyStatus.DRAFT ? null : DEFAULT_NOW)
                .build();
    }

    public static Vacancy aVacancy(VacancyStatus status, Instant updatedAt) {
        return aVacancyBuilder()
                .status(status)
                .publishedAt(status == VacancyStatus.DRAFT ? null : DEFAULT_NOW)
                .updatedAt(updatedAt)
                .build();
    }

    public static Vacancy aDeletedVacancy() {
        return aVacancyBuilder().deleted(true).deletedAt(DEFAULT_NOW).build();
    }

    public static Vacancy aDeletedVacancy(Instant deletedAt) {
        return aVacancyBuilder().deleted(true).deletedAt(deletedAt).updatedAt(deletedAt).build();
    }

    public static Vacancy.VacancyBuilder aVacancyBuilder() {
        return Vacancy.builder()
                .id(DEFAULT_ID)
                .authorId(DEFAULT_AUTHOR_ID)
                .assignedRecruiterId(null)
                .companyId(DEFAULT_COMPANY_ID)
                .title("Senior Java Engineer")
                .category(JobCategory.ENGINEERING)
                .specialization("Backend")
                .seniorityLevel("Senior")
                .minYearsOfExperience(5)
                .description("Great opportunity for Java and Spring Boot developers")
                .requiredSkills(List.of("Java"))
                .preferredSkills(List.of("Docker"))
                .minEnglishLevel(EnglishLevel.B2)
                .salaryMin(DEFAULT_SALARY_MIN)
                .salaryMax(DEFAULT_SALARY_MAX)
                .currency("USD")
                .employmentType(EmploymentType.FULL_TIME)
                .locationType(LocationType.REMOTE)
                .location("Kyiv, Ukraine")
                .status(VacancyStatus.OPEN)
                .deleted(false)
                .publishedAt(DEFAULT_NOW)
                .expiresAt(null)
                .createdAt(DEFAULT_NOW)
                .updatedAt(DEFAULT_NOW);
    }

    public static VacancyResponse aVacancyResponse() {
        return aVacancyResponse(VacancyStatus.OPEN, DEFAULT_NOW);
    }

    public static VacancyResponse aVacancyResponse(VacancyStatus status) {
        return aVacancyResponse(status, DEFAULT_NOW);
    }

    public static VacancyResponse aDraftVacancyResponse() {
        return aVacancyResponse(VacancyStatus.DRAFT);
    }

    public static VacancyResponse aVacancyResponse(JobCategory category) {
        return aVacancyResponse(VacancyStatus.OPEN, category, DEFAULT_NOW);
    }

    public static VacancyResponse aVacancyResponse(VacancyStatus status, Instant updatedAt) {
        return aVacancyResponse(status, JobCategory.ENGINEERING, updatedAt);
    }

    public static VacancyResponse aVacancyResponse(VacancyStatus status, JobCategory category, Instant updatedAt) {
        return new VacancyResponse(
                DEFAULT_ID,
                DEFAULT_AUTHOR_ID,
                null,
                DEFAULT_COMPANY_ID,
                "Senior Java Engineer",
                category,
                "Backend",
                "Senior",
                5,
                "Great opportunity for Java and Spring Boot developers",
                List.of("Java"),
                List.of("Docker"),
                EnglishLevel.B2,
                DEFAULT_SALARY_MIN,
                DEFAULT_SALARY_MAX,
                "USD",
                EmploymentType.FULL_TIME,
                LocationType.REMOTE,
                "Kyiv, Ukraine",
                status,
                status == VacancyStatus.DRAFT ? null : DEFAULT_NOW,
                null,
                DEFAULT_NOW,
                updatedAt
        );
    }

    public static Page<Vacancy> aVacancyPage(List<Vacancy> content, Pageable pageable) {
        return new PageImpl<>(content, pageable, content.size());
    }

    public static Page<VacancyResponse> aVacancyResponsePage(List<VacancyResponse> content, Pageable pageable) {
        return new PageImpl<>(content, pageable, content.size());
    }

    public static UpdateVacancyStatusRequest validUpdateVacancyStatusRequest() {
        return new UpdateVacancyStatusRequest(VacancyStatus.CLOSED);
    }

    public static class CreateVacancyRequestBuilder {
        private UUID authorId = DEFAULT_AUTHOR_ID;
        private UUID assignedRecruiterId;
        private UUID companyId = DEFAULT_COMPANY_ID;
        private VacancyStatus status = VacancyStatus.OPEN;
        private String title = "Senior Java Engineer";
        private JobCategory category = JobCategory.ENGINEERING;
        private String specialization = "Backend";
        private String seniorityLevel = "Senior";
        private Integer minYearsOfExperience = 5;
        private String description = "Great opportunity for Java and Spring Boot developers";
        private List<String> requiredSkills = List.of("Java");
        private List<String> preferredSkills = List.of("Docker");
        private EnglishLevel minEnglishLevel = EnglishLevel.B2;
        private BigDecimal salaryMin = DEFAULT_SALARY_MIN;
        private BigDecimal salaryMax = DEFAULT_SALARY_MAX;
        private String currency = "USD";
        private EmploymentType employmentType = EmploymentType.FULL_TIME;
        private LocationType locationType = LocationType.REMOTE;
        private String location = "Kyiv, Ukraine";
        private Instant expiresAt;

        public CreateVacancyRequestBuilder authorId(UUID authorId) {
            this.authorId = authorId;
            return this;
        }

        public CreateVacancyRequestBuilder assignedRecruiterId(UUID assignedRecruiterId) {
            this.assignedRecruiterId = assignedRecruiterId;
            return this;
        }

        public CreateVacancyRequestBuilder companyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }

        public CreateVacancyRequestBuilder status(VacancyStatus status) {
            this.status = status;
            return this;
        }

        public CreateVacancyRequestBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CreateVacancyRequestBuilder salaryMin(BigDecimal salaryMin) {
            this.salaryMin = salaryMin;
            return this;
        }

        public CreateVacancyRequestBuilder salaryMax(BigDecimal salaryMax) {
            this.salaryMax = salaryMax;
            return this;
        }

        public CreateVacancyRequestBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public CreateVacancyRequest build() {
            return new CreateVacancyRequest(
                    authorId,
                    assignedRecruiterId,
                    companyId,
                    status,
                    title,
                    category,
                    specialization,
                    seniorityLevel,
                    minYearsOfExperience,
                    description,
                    requiredSkills,
                    preferredSkills,
                    minEnglishLevel,
                    salaryMin,
                    salaryMax,
                    currency,
                    employmentType,
                    locationType,
                    location,
                    expiresAt
            );
        }
    }
}
