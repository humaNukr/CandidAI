package ua.edu.ukma.candidai.recruitment.controller;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.*;

@IntegrationTest
class ApplicationCrudIntegrationTest {

    private static final String VACANCIES_URL = "/api/v1/vacancies";
    private static final String APPLICATIONS_URL = "/api/v1/applications";
    private static final UUID NON_EXISTENT_VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void givenValidPayload_applyForVacancy_shouldReturn201AndPersistInPostgres() throws Exception {
        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest request = aValidApplyRequest(vacancyId);

        MvcResult result = mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        ApplicationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        Integer rowCount = jdbcClient.sql("""
                SELECT COUNT(*) FROM applications 
                WHERE id = :id AND vacancy_id = :vacancyId AND email = :email
                """)
                .param("id", response.id())
                .param("vacancyId", vacancyId)
                .param("email", request.email())
                .query(Integer.class)
                .single();

        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void givenInvalidPayload_applyForVacancy_shouldReturn400AndNotChangeRowCount() throws Exception {
        Integer countBefore = jdbcClient.sql("SELECT COUNT(*) FROM applications")
                .query(Integer.class)
                .single();

        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest invalidRequest = anInvalidApplyRequest(vacancyId);

        mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        Integer countAfter = jdbcClient.sql("SELECT COUNT(*) FROM applications")
                .query(Integer.class)
                .single();

        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void givenDuplicateApplication_applyForVacancy_shouldReturn409AndNotDuplicateInPostgres() throws Exception {
        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest request = aValidApplyRequest(vacancyId);

        mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        Integer rowCount = jdbcClient.sql("""
                SELECT COUNT(*) FROM applications 
                WHERE vacancy_id = :vacancyId AND email = :email
                """)
                .param("vacancyId", vacancyId)
                .param("email", request.email())
                .query(Integer.class)
                .single();

        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void givenNonExistentVacancy_applyForVacancy_shouldReturn404AndNotPersistInPostgres() throws Exception {
        ApplyForVacancyRequest request = aValidApplyRequest(NON_EXISTENT_VACANCY_ID);

        mockMvc.perform(post(APPLICATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        Integer rowCount = jdbcClient.sql("""
                SELECT COUNT(*) FROM applications 
                WHERE vacancy_id = :vacancyId
                """)
                .param("vacancyId", NON_EXISTENT_VACANCY_ID)
                .query(Integer.class)
                .single();

        assertThat(rowCount).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenExistingApplication_getApplicationById_shouldReturn200AndMatchDatabase() throws Exception {
        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest request = aValidApplyRequest(vacancyId);

        MvcResult applyResult = mockMvc.perform(post(APPLICATIONS_URL)
                        .with(user("candidate@candidai.ua").roles("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApplicationResponse created = objectMapper.readValue(
                applyResult.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        MvcResult getResult = mockMvc.perform(get(APPLICATIONS_URL + "/" + created.id()))
                .andExpect(status().isOk())
                .andReturn();

        ApplicationResponse fetched = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        String emailFromDb = jdbcClient.sql("SELECT email FROM applications WHERE id = :id")
                .param("id", created.id())
                .query(String.class)
                .single();

        assertThat(fetched.email()).isEqualTo(emailFromDb);
        assertThat(fetched.id()).isEqualTo(created.id());
    }

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenExistingApplication_updateApplicationStatus_shouldReturn200AndPersistStatusInPostgres()
            throws Exception {
        UUID vacancyId = createVacancyAsRecruiter();
        ApplyForVacancyRequest request = aValidApplyRequest(vacancyId);

        MvcResult applyResult = mockMvc.perform(post(APPLICATIONS_URL)
                        .with(user("candidate@candidai.ua").roles("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApplicationResponse created = objectMapper.readValue(
                applyResult.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        UpdateApplicationStatusRequest statusRequest = new UpdateApplicationStatusRequest(
                ApplicationStatus.SCREENING,
                "Passed automated resume screening",
                88
        );

        mockMvc.perform(patch(APPLICATIONS_URL + "/" + created.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());

        String statusFromDb = jdbcClient.sql("SELECT status FROM applications WHERE id = :id")
                .param("id", created.id())
                .query(String.class)
                .single();

        Integer scoreFromDb = jdbcClient.sql("SELECT matching_score FROM applications WHERE id = :id")
                .param("id", created.id())
                .query(Integer.class)
                .single();

        assertThat(statusFromDb).isEqualTo(ApplicationStatus.SCREENING.name());
        assertThat(scoreFromDb).isEqualTo(88);
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
