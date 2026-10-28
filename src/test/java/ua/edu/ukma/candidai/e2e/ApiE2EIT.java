package ua.edu.ukma.candidai.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import ua.edu.ukma.candidai.common.annotation.E2ETest;
import ua.edu.ukma.candidai.common.security.dto.AuthResponse;
import ua.edu.ukma.candidai.common.security.dto.LoginRequest;
import ua.edu.ukma.candidai.common.wiremock.ExternalServicesStubConfiguration;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.SEED_COMPANY_ID;

@E2ETest
class ApiE2EIT {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String VACANCIES_URL = "/api/v1/vacancies";
    private static final String APPLICATIONS_URL = "/api/v1/applications";

    private static final String RECRUITER_EMAIL = "recruiter@candidai.com";
    private static final String RECRUITER_PASS = "RecruiterPass123!";

    private static final String CANDIDATE_EMAIL = "candidate@candidai.com";
    private static final String CANDIDATE_PASS = "CandidatePass123!";

    private static final String ADMIN_EMAIL = "admin@candidai.com";
    private static final String ADMIN_PASS = "AdminPass123!";

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        ExternalServicesStubConfiguration.setupDefaultStubs(wireMockServer);
    }

    private String login(String email, String password) {
        AuthResponse response = client.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        return response.accessToken();
    }

    @Test
    void fullRecruitmentCycle_recruiterCreatesVacancy_candidateApplies_recruiterReadsApplication() {
        String recruiterToken = login(RECRUITER_EMAIL, RECRUITER_PASS);
        UUID recruiterId = getUserIdByEmail(RECRUITER_EMAIL);

        CreateVacancyRequest vacancyRequest = buildVacancyRequest(recruiterId, "Senior Cloud Architect");

        VacancyResponse vacancy = client.post().uri(VACANCIES_URL)
                .headers(h -> h.setBearerAuth(recruiterToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(vacancyRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(VacancyResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(vacancy).isNotNull();
        assertThat(vacancy.title()).isEqualTo("Senior Cloud Architect");

        Integer vacancyInDb = jdbcClient.sql("SELECT COUNT(*) FROM vacancies WHERE id = :id")
                .param("id", vacancy.id())
                .query(Integer.class)
                .single();
        assertThat(vacancyInDb).isEqualTo(1);

        String candidateToken = login(CANDIDATE_EMAIL, CANDIDATE_PASS);
        UUID candidateId = getUserIdByEmail(CANDIDATE_EMAIL);

        ApplyForVacancyRequest applyRequest = new ApplyForVacancyRequest(
                vacancy.id(),
                candidateId,
                "Ivan Franko",
                CANDIDATE_EMAIL,
                "+380509876543",
                "https://storage.candidai.ua/resumes/franko.pdf"
        );

        ApplicationResponse application = client.post().uri(APPLICATIONS_URL)
                .headers(h -> h.setBearerAuth(candidateToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(applyRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(application).isNotNull();
        assertThat(application.email()).isEqualTo(CANDIDATE_EMAIL);

        Integer appInDb = jdbcClient.sql("SELECT COUNT(*) FROM applications WHERE id = :id")
                .param("id", application.id())
                .query(Integer.class)
                .single();
        assertThat(appInDb).isEqualTo(1);

        ApplicationResponse fetched = client.get().uri(APPLICATIONS_URL + "/{id}", application.id())
                .headers(h -> h.setBearerAuth(recruiterToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.id()).isEqualTo(application.id());
        assertThat(fetched.candidateName()).isEqualTo("Ivan Franko");
    }

    @Test
    void candidateRole_cannotCreateVacancy_shouldReturn403Forbidden() {
        String candidateToken = login(CANDIDATE_EMAIL, CANDIDATE_PASS);
        UUID candidateId = getUserIdByEmail(CANDIDATE_EMAIL);

        CreateVacancyRequest vacancyRequest = buildVacancyRequest(candidateId, "Unauthorized Vacancy");

        client.post().uri(VACANCIES_URL)
                .headers(h -> h.setBearerAuth(candidateToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(vacancyRequest)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void unauthenticatedRequest_toProtectedEndpoint_shouldReturn401Unauthorized() {
        UUID randomId = UUID.randomUUID();
        CreateVacancyRequest vacancyRequest = buildVacancyRequest(randomId, "Anonymous Vacancy");

        client.post().uri(VACANCIES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(vacancyRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void duplicateApplication_shouldReturn409Conflict() {
        String recruiterToken = login(RECRUITER_EMAIL, RECRUITER_PASS);
        UUID recruiterId = getUserIdByEmail(RECRUITER_EMAIL);

        VacancyResponse vacancy = client.post().uri(VACANCIES_URL)
                .headers(h -> h.setBearerAuth(recruiterToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildVacancyRequest(recruiterId, "Security Lead Engineer"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(VacancyResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(vacancy).isNotNull();

        String candidateToken = login(CANDIDATE_EMAIL, CANDIDATE_PASS);
        UUID candidateId = getUserIdByEmail(CANDIDATE_EMAIL);

        ApplyForVacancyRequest applyRequest = new ApplyForVacancyRequest(
                vacancy.id(),
                candidateId,
                "Lesya Ukrainka",
                CANDIDATE_EMAIL,
                "+380671112233",
                "https://storage.candidai.ua/resumes/lesya.pdf"
        );

        client.post().uri(APPLICATIONS_URL)
                .headers(h -> h.setBearerAuth(candidateToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(applyRequest)
                .exchange()
                .expectStatus().isCreated();

        client.post().uri(APPLICATIONS_URL)
                .headers(h -> h.setBearerAuth(candidateToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(applyRequest)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void adminCanDeleteVacancy_recruiterCannotDeleteVacancy() {
        String recruiterToken = login(RECRUITER_EMAIL, RECRUITER_PASS);
        UUID recruiterId = getUserIdByEmail(RECRUITER_EMAIL);

        VacancyResponse vacancy = client.post().uri(VACANCIES_URL)
                .headers(h -> h.setBearerAuth(recruiterToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildVacancyRequest(recruiterId, "DevOps Lead"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(VacancyResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(vacancy).isNotNull();

        client.delete().uri(VACANCIES_URL + "/{id}", vacancy.id())
                .headers(h -> h.setBearerAuth(recruiterToken))
                .exchange()
                .expectStatus().isForbidden();

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASS);

        client.delete().uri(VACANCIES_URL + "/{id}", vacancy.id())
                .headers(h -> h.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isNoContent();

        Boolean isDeleted = jdbcClient.sql("SELECT deleted FROM vacancies WHERE id = :id")
                .param("id", vacancy.id())
                .query(Boolean.class)
                .single();
        assertThat(isDeleted).isTrue();
    }

    private UUID getUserIdByEmail(String email) {
        return jdbcClient.sql("SELECT id FROM users WHERE email = :email")
                .param("email", email.toLowerCase())
                .query(UUID.class)
                .single();
    }

    private CreateVacancyRequest buildVacancyRequest(UUID authorId, String title) {
        return new CreateVacancyRequest(
                authorId,
                authorId,
                SEED_COMPANY_ID,
                VacancyStatus.OPEN,
                title,
                JobCategory.ENGINEERING,
                "Cloud Systems",
                "Lead",
                5,
                "Leading cloud architectural initiatives and microservices.",
                List.of("Java 25", "Spring Boot", "PostgreSQL"),
                List.of("Docker", "Kubernetes"),
                EnglishLevel.C1,
                new BigDecimal("6000.00"),
                new BigDecimal("8000.00"),
                "USD",
                EmploymentType.FULL_TIME,
                LocationType.REMOTE,
                "Kyiv, Ukraine",
                Instant.now().plus(Duration.ofDays(30))
        );
    }
}
