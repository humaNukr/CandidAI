package ua.edu.ukma.candidai.vacancy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.Skill;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public static final UUID SKILL_1_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID SKILL_2_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    public static final String SKILL_JAVA = "Java";
    public static final String SKILL_DOCKER = "Docker";

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

    public static <T> T parseResponse(ObjectMapper objectMapper, MvcResult result, Class<T> clazz) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), clazz);
    }

    public static ProblemDetail parseProblemDetail(ObjectMapper objectMapper, MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
    }

    public static <T> List<T> parsePagedContent(
            ObjectMapper objectMapper,
            MvcResult result,
            Class<T> elementType
    ) throws Exception {
        tools.jackson.databind.JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        tools.jackson.databind.JsonNode contentNode = root.get("content");
        return objectMapper.treeToValue(
                contentNode,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
        );
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> extractErrors(ProblemDetail problemDetail) {
        return (Map<String, String>) problemDetail.getProperties().get("errors");
    }

    public static ProblemDetail expectedValidationProblemDetail(Map<String, String> errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Input validation failed"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://candidai.ukma.edu.ua/errors/validation"));
        problemDetail.setInstance(URI.create(BASE_URL));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", Instant.EPOCH);
        return problemDetail;
    }

    public static ProblemDetail expectedConflictProblemDetail(String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                detail
        );
        problemDetail.setTitle("Resource Conflict");
        problemDetail.setType(URI.create("https://candidai.ukma.edu.ua/errors/conflict"));
        problemDetail.setInstance(URI.create(BASE_URL));
        problemDetail.setProperty("timestamp", Instant.EPOCH);
        return problemDetail;
    }

    public static ProblemDetail expectedNotFoundProblemDetail(UUID id) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Vacancy not found with id: " + id
        );
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://candidai.ukma.edu.ua/errors/not-found"));
        problemDetail.setInstance(URI.create(BASE_URL + "/" + id));
        problemDetail.setProperty("timestamp", Instant.EPOCH);
        return problemDetail;
    }

    public static ProblemDetail expectedJsonParsingProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed request body or unknown properties"
        );
        problemDetail.setTitle("JSON Parsing Error");
        problemDetail.setType(URI.create("https://candidai.ukma.edu.ua/errors/bad-request"));
        problemDetail.setInstance(URI.create(BASE_URL));
        problemDetail.setProperty("timestamp", Instant.EPOCH);
        return problemDetail;
    }

    public static ProblemDetail expectedInvalidStateTransitionProblemDetail(String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                detail
        );
        problemDetail.setTitle("Invalid State Transition");
        problemDetail.setType(URI.create("https://candidai.ukma.edu.ua/errors/invalid-state-transition"));
        problemDetail.setInstance(URI.create(BASE_URL + "/" + DEFAULT_ID + "/status"));
        problemDetail.setProperty("timestamp", Instant.EPOCH);
        return problemDetail;
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

    public static Vacancy aDraftVacancy() {
        return aVacancy(VacancyStatus.DRAFT);
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
                .skills(new ArrayList<>(List.of(aSkillJava())))
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
                List.of(),
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

    public static VacancyResponse aDraftVacancyResponse() {
        return aVacancyResponse(VacancyStatus.DRAFT);
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

    public static Skill aSkillJava() {
        return Skill.builder()
                .id(SKILL_1_ID)
                .name(SKILL_JAVA)
                .build();
    }

    public static Skill aSkillDocker() {
        return Skill.builder()
                .id(SKILL_2_ID)
                .name(SKILL_DOCKER)
                .build();
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
        private List<String> preferredSkills = List.of();
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

        public CreateVacancyRequestBuilder requiredSkills(List<String> requiredSkills) {
            this.requiredSkills = requiredSkills;
            return this;
        }

        public CreateVacancyRequestBuilder preferredSkills(List<String> preferredSkills) {
            this.preferredSkills = preferredSkills;
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
