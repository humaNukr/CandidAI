package ua.edu.ukma.candidai.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.wiremock.ExternalServicesStubConfiguration;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.ukma.candidai.common.wiremock.ExternalServicesStubConfiguration.GEMINI_API_KEY;
import static ua.edu.ukma.candidai.common.wiremock.ExternalServicesStubConfiguration.GEMINI_PATH;
import static ua.edu.ukma.candidai.common.wiremock.ExternalServicesStubConfiguration.TELEGRAM_PATH;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.SEED_CANDIDATE_ID;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.aValidApplyRequest;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.aValidCreateVacancyRequest;

@IntegrationTest
class ApplicationFlowIntegrationTest {

    private static final String VACANCIES_URL = "/api/v1/vacancies";
    private static final String APPLICATIONS_URL = "/api/v1/applications";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        ExternalServicesStubConfiguration.setupDefaultStubs(wireMockServer);
    }

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void candidateApplies_shouldPersistApplication_triggerScreeningAndNotifyTelegram() throws Exception {
        jdbcClient.sql("UPDATE users SET telegram_chat_id = '123456789' WHERE id = :id")
                .param("id", SEED_CANDIDATE_ID)
                .update();

        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest applyRequest = aValidApplyRequest(vacancyId);

        MvcResult result = mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        ApplicationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        String initialStatus = jdbcClient.sql("SELECT status FROM applications WHERE id = :id")
                .param("id", response.id())
                .query(String.class)
                .single();
        assertThat(initialStatus).isEqualTo(ApplicationStatus.APPLIED.name());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            wireMockServer.verify(postRequestedFor(urlEqualTo(GEMINI_PATH))
                    .withHeader("x-goog-api-key", equalTo(GEMINI_API_KEY)));

            String updatedStatus = jdbcClient.sql("SELECT status FROM applications WHERE id = :id")
                    .param("id", response.id())
                    .query(String.class)
                    .single();
            assertThat(updatedStatus).isEqualTo(ApplicationStatus.SCREENING.name());

            Integer score = jdbcClient.sql("SELECT matching_score FROM applications WHERE id = :id")
                    .param("id", response.id())
                    .query(Integer.class)
                    .optional()
                    .orElse(null);
            assertThat(score).isEqualTo(88);

            wireMockServer.verify(postRequestedFor(urlEqualTo(TELEGRAM_PATH)));
        });
    }

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void geminiError_shouldKeepApplicationAsAppliedSafely() throws Exception {
        ExternalServicesStubConfiguration.stubGeminiError(wireMockServer, 500);

        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest applyRequest = aValidApplyRequest(vacancyId);

        MvcResult result = mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApplicationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        String statusInDb = jdbcClient.sql("SELECT status FROM applications WHERE id = :id")
                .param("id", response.id())
                .query(String.class)
                .single();
        assertThat(statusInDb).isEqualTo(ApplicationStatus.APPLIED.name());

        await().pollDelay(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            wireMockServer.verify(postRequestedFor(urlEqualTo(GEMINI_PATH)));

            String statusAfterFailure = jdbcClient.sql("SELECT status FROM applications WHERE id = :id")
                    .param("id", response.id())
                    .query(String.class)
                    .single();
            assertThat(statusAfterFailure).isEqualTo(ApplicationStatus.APPLIED.name());
        });
    }

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void candidateRejectedByScreening_shouldUpdateStatusToRejected() throws Exception {
        ExternalServicesStubConfiguration.stubGeminiSuccess(
                wireMockServer,
                35,
                false,
                "Candidate skills do not meet minimum seniority requirements."
        );

        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest applyRequest = aValidApplyRequest(vacancyId);

        MvcResult result = mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApplicationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String updatedStatus = jdbcClient.sql("SELECT status FROM applications WHERE id = :id")
                    .param("id", response.id())
                    .query(String.class)
                    .single();
            assertThat(updatedStatus).isEqualTo(ApplicationStatus.REJECTED.name());

            Integer score = jdbcClient.sql("SELECT matching_score FROM applications WHERE id = :id")
                    .param("id", response.id())
                    .query(Integer.class)
                    .optional()
                    .orElse(null);
            assertThat(score).isEqualTo(35);
        });
    }

    private UUID createVacancyAsRecruiter() throws Exception {
        CreateVacancyRequest vacancyRequest = aValidCreateVacancyRequest();

        MvcResult createResult = mockMvc.perform(post(VACANCIES_URL)
                        .with(user("author@candidai.ua").roles("RECRUITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vacancyRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        VacancyResponse vacancyResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                VacancyResponse.class
        );

        return vacancyResponse.id();
    }
}
