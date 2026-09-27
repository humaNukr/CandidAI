package ua.edu.ukma.candidai.company;

import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.model.Company;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CompanyTestResources {

    public static final String BASE_URL = "/api/v1/companies";
    public static final UUID DEFAULT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID NON_EXISTENT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final String DEFAULT_NAME = "TechCorp";
    public static final String DEFAULT_DESCRIPTION = "Leading tech company";
    public static final String DEFAULT_LOGO_URL = "https://techcorp.com/logo.png";
    public static final String DEFAULT_CONTACT_EMAIL = "contact@techcorp.com";
    public static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-09-27T10:00:00Z");

    public static final String UPDATED_NAME = "NewTechCorp";
    public static final String UPDATED_DESCRIPTION = "Innovative tech company";
    public static final String UPDATED_LOGO_URL = "https://newtechcorp.com/logo.png";
    public static final String UPDATED_CONTACT_EMAIL = "info@newtechcorp.com";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CompanyTestResources() {
    }

    public static <T> T parseResponse(ObjectMapper objectMapper, MvcResult result, Class<T> clazz) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), clazz);
    }

    public static <T> T parseResponse(MvcResult result, Class<T> clazz) throws Exception {
        return parseResponse(OBJECT_MAPPER, result, clazz);
    }

    public static <T> List<T> parseResponseList(
            ObjectMapper objectMapper,
            MvcResult result,
            Class<T> elementType
    ) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
        );
    }

    public static <T> List<T> parseResponseList(MvcResult result, Class<T> elementType) throws Exception {
        return parseResponseList(OBJECT_MAPPER, result, elementType);
    }

    public static ProblemDetail parseProblemDetail(
            ObjectMapper objectMapper,
            MvcResult result
    ) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
    }

    public static ProblemDetail parseProblemDetail(MvcResult result) throws Exception {
        return parseProblemDetail(OBJECT_MAPPER, result);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> extractErrors(ProblemDetail problemDetail) {
        return (Map<String, String>) problemDetail.getProperties().get("errors");
    }

    public static CreateCompanyRequest sampleCreateCompanyRequest() {
        return new CreateCompanyRequest(
                DEFAULT_NAME,
                DEFAULT_DESCRIPTION,
                DEFAULT_LOGO_URL,
                DEFAULT_CONTACT_EMAIL
        );
    }

    public static CreateCompanyRequest sampleInvalidCreateCompanyRequest() {
        return new CreateCompanyRequest(
                "",
                DEFAULT_DESCRIPTION,
                DEFAULT_LOGO_URL,
                DEFAULT_CONTACT_EMAIL
        );
    }

    public static UpdateCompanyRequest sampleUpdateCompanyRequest() {
        return new UpdateCompanyRequest(
                UPDATED_NAME,
                UPDATED_DESCRIPTION,
                UPDATED_LOGO_URL,
                UPDATED_CONTACT_EMAIL
        );
    }

    public static UpdateCompanyRequest sampleInvalidUpdateCompanyRequest() {
        return new UpdateCompanyRequest(
                "",
                UPDATED_DESCRIPTION,
                UPDATED_LOGO_URL,
                UPDATED_CONTACT_EMAIL
        );
    }

    public static Company sampleCompany() {
        return sampleCompanyBuilder().build();
    }

    public static Company sampleUpdatedCompany() {
        return sampleCompanyBuilder()
                .name(UPDATED_NAME)
                .description(UPDATED_DESCRIPTION)
                .logoUrl(UPDATED_LOGO_URL)
                .contactEmail(UPDATED_CONTACT_EMAIL)
                .build();
    }

    public static CompanyResponse sampleCompanyResponse() {
        return new CompanyResponse(
                DEFAULT_COMPANY_ID,
                DEFAULT_NAME,
                DEFAULT_DESCRIPTION,
                DEFAULT_LOGO_URL,
                DEFAULT_CONTACT_EMAIL,
                DEFAULT_CREATED_AT,
                0
        );
    }

    public static CompanyResponse sampleUpdatedCompanyResponse() {
        return new CompanyResponse(
                DEFAULT_COMPANY_ID,
                UPDATED_NAME,
                UPDATED_DESCRIPTION,
                UPDATED_LOGO_URL,
                UPDATED_CONTACT_EMAIL,
                DEFAULT_CREATED_AT,
                0
        );
    }

    public static CompanySummaryResponse sampleCompanySummaryResponse() {
        return new CompanySummaryResponse(
                DEFAULT_COMPANY_ID,
                DEFAULT_NAME,
                DEFAULT_DESCRIPTION,
                DEFAULT_LOGO_URL,
                DEFAULT_CONTACT_EMAIL,
                DEFAULT_CREATED_AT
        );
    }

    public static String expectedCompanyNotFoundMessage(UUID id) {
        return "Company not found with id: " + id;
    }

    public static String expectedDuplicateCompanyNameMessage(String name) {
        return "Company with name '" + name + "' already exists";
    }

    public static Company sampleCompanyWithVacancies() {
        Company company = sampleCompany();
        Vacancy openVacancy = Vacancy.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000011"))
                .title("Java Developer")
                .status(VacancyStatus.OPEN)
                .build();
        Vacancy closedVacancy = Vacancy.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000012"))
                .title("Kotlin Developer")
                .status(VacancyStatus.CLOSED)
                .build();
        company.addVacancy(openVacancy);
        company.addVacancy(closedVacancy);
        return company;
    }

    public static Company.CompanyBuilder sampleCompanyBuilder() {
        return Company.builder()
                .id(DEFAULT_COMPANY_ID)
                .name(DEFAULT_NAME)
                .description(DEFAULT_DESCRIPTION)
                .logoUrl(DEFAULT_LOGO_URL)
                .contactEmail(DEFAULT_CONTACT_EMAIL)
                .createdAt(DEFAULT_CREATED_AT)
                .vacancies(new ArrayList<>());
    }

    public static Vacancy sampleVacancy() {
        return Vacancy.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000013"))
                .title("Backend Engineer")
                .status(VacancyStatus.OPEN)
                .build();
    }
}
