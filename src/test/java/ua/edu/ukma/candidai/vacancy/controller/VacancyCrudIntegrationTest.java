package ua.edu.ukma.candidai.vacancy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.*;

@IntegrationTest
class VacancyCrudIntegrationTest {

    private static final String VACANCIES_URL = "/api/v1/vacancies";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenValidPayload_createVacancy_shouldReturn201AndPersistInPostgres() throws Exception {
        CreateVacancyRequest request = aValidCreateVacancyRequest();

        MvcResult result = mockMvc.perform(post(VACANCIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        VacancyResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                VacancyResponse.class
        );

        Integer rowCount = jdbcClient.sql("SELECT COUNT(*) FROM vacancies WHERE id = :id AND title = :title")
                .param("id", response.id())
                .param("title", request.title())
                .query(Integer.class)
                .single();

        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenInvalidPayload_createVacancy_shouldReturn400AndNotChangeRowCount() throws Exception {
        Integer countBefore = jdbcClient.sql("SELECT COUNT(*) FROM vacancies")
                .query(Integer.class)
                .single();

        CreateVacancyRequest invalidRequest = anInvalidCreateVacancyRequest();

        mockMvc.perform(post(VACANCIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        Integer countAfter = jdbcClient.sql("SELECT COUNT(*) FROM vacancies")
                .query(Integer.class)
                .single();

        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenExistingVacancy_getVacancyById_shouldReturn200AndMatchDatabase() throws Exception {
        CreateVacancyRequest request = aValidCreateVacancyRequest();

        MvcResult createResult = mockMvc.perform(post(VACANCIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        VacancyResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                VacancyResponse.class
        );

        MvcResult getResult = mockMvc.perform(get(VACANCIES_URL + "/" + created.id()))
                .andExpect(status().isOk())
                .andReturn();

        VacancyResponse fetched = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                VacancyResponse.class
        );

        String titleFromDb = jdbcClient.sql("SELECT title FROM vacancies WHERE id = :id")
                .param("id", created.id())
                .query(String.class)
                .single();

        assertThat(fetched.title()).isEqualTo(titleFromDb);
        assertThat(fetched.id()).isEqualTo(created.id());
    }

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenExistingVacancy_updateVacancyStatus_shouldReturn200AndPersistStatusInPostgres() throws Exception {
        CreateVacancyRequest request = aValidCreateVacancyRequest();

        MvcResult createResult = mockMvc.perform(post(VACANCIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        VacancyResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                VacancyResponse.class
        );

        UpdateVacancyStatusRequest statusRequest = new UpdateVacancyStatusRequest(VacancyStatus.CLOSED);

        mockMvc.perform(patch(VACANCIES_URL + "/" + created.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());

        String statusFromDb = jdbcClient.sql("SELECT status FROM vacancies WHERE id = :id")
                .param("id", created.id())
                .query(String.class)
                .single();

        assertThat(statusFromDb).isEqualTo(VacancyStatus.CLOSED.name());
    }

    @Test
    @WithMockUser(username = "candidate@candidai.ua", roles = "CANDIDATE")
    void givenCandidateRole_createVacancy_shouldReturn403AndNotPersist() throws Exception {
        CreateVacancyRequest request = aValidCreateVacancyRequest();

        mockMvc.perform(post(VACANCIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Integer rowCount = jdbcClient.sql("SELECT COUNT(*) FROM vacancies WHERE title = :title")
                .param("title", request.title())
                .query(Integer.class)
                .single();

        assertThat(rowCount).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenRecruiterRole_deleteVacancy_shouldReturn403AndKeepActiveInPostgres() throws Exception {
        CreateVacancyRequest request = aValidCreateVacancyRequest();

        MvcResult createResult = mockMvc.perform(post(VACANCIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        VacancyResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                VacancyResponse.class
        );

        mockMvc.perform(delete(VACANCIES_URL + "/" + created.id()))
                .andExpect(status().isForbidden());

        Boolean isDeleted = jdbcClient.sql("SELECT deleted FROM vacancies WHERE id = :id")
                .param("id", created.id())
                .query(Boolean.class)
                .single();

        assertThat(isDeleted).isFalse();
    }

    @Test
    @WithMockUser(username = "admin@candidai.ua", roles = "ADMIN")
    void givenExistingVacancy_deleteVacancy_shouldReturn204AndRemoveOrMarkDeleted() throws Exception {
        CreateVacancyRequest request = aValidCreateVacancyRequest();

        MvcResult createResult = mockMvc.perform(post(VACANCIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        VacancyResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                VacancyResponse.class
        );

        mockMvc.perform(delete(VACANCIES_URL + "/" + created.id()))
                .andExpect(status().isNoContent());

        Boolean isDeleted = jdbcClient.sql("SELECT deleted FROM vacancies WHERE id = :id")
                .param("id", created.id())
                .query(Boolean.class)
                .single();

        assertThat(isDeleted).isTrue();
    }
}
