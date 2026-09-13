package ua.edu.ukma.candidai.vacancy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

class TestResources {

    static final String BASE_URL = "/api/v1/vacancies";
    static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID DEFAULT_AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID NON_EXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final BigDecimal DEFAULT_SALARY_MIN = BigDecimal.valueOf(3000);
    static final BigDecimal DEFAULT_SALARY_MAX = BigDecimal.valueOf(5000);

    static final String JSON_WITH_UNKNOWN_PROPERTY = """
            {
                "unknownField": "bad"
            }
            """;

    static final String VALIDATION_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/validation",
                "title": "Validation Error",
                "status": 400,
                "detail": "Input validation failed"
            }
            """;

    static final String INVALID_SALARY_RANGE_ERROR_JSON = """
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

    static final String JSON_PARSING_ERROR_JSON = """
            {
                "type": "https://candidai.ukma.edu.ua/errors/bad-request",
                "title": "JSON Parsing Error",
                "status": 400,
                "detail": "Malformed request body or unknown properties"
            }
            """;

    static String notFoundProblemDetailJson(UUID id) {
        return """
                {
                    "type": "https://candidai.ukma.edu.ua/errors/not-found",
                    "title": "Resource Not Found",
                    "status": 404,
                    "detail": "Vacancy not found with id: %s"
                }
                """.formatted(id);
    }

    static CreateVacancyRequestBuilder aCreateVacancyRequest() {
        return new CreateVacancyRequestBuilder();
    }

    static CreateVacancyRequest validCreateVacancyRequest() {
        return aCreateVacancyRequest().build();
    }

    static VacancyResponse aVacancyResponse() {
        return new VacancyResponse(
                DEFAULT_ID,
                DEFAULT_AUTHOR_ID,
                null,
                "Senior Java Engineer",
                JobCategory.ENGINEERING,
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
                VacancyStatus.OPEN,
                Instant.parse("2026-09-12T10:00:00Z"),
                null,
                Instant.parse("2026-09-12T10:00:00Z"),
                Instant.parse("2026-09-12T10:00:00Z")
        );
    }

    static UpdateVacancyStatusRequest validUpdateVacancyStatusRequest() {
        return new UpdateVacancyStatusRequest(VacancyStatus.CLOSED);
    }

    static class CreateVacancyRequestBuilder {
        private UUID authorId = DEFAULT_AUTHOR_ID;
        private UUID assignedRecruiterId;
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

        public CreateVacancyRequest build() {
            return new CreateVacancyRequest(
                    authorId,
                    assignedRecruiterId,
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
