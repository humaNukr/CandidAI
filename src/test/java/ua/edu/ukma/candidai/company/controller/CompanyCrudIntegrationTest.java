package ua.edu.ukma.candidai.company.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.*;

@IntegrationTest
class CompanyCrudIntegrationTest {

    private static final String COMPANIES_URL = "/api/v1/companies";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin@candidai.ua", roles = "ADMIN")
    void givenValidPayload_createCompany_shouldReturn201AndPersistInPostgres() throws Exception {
        CreateCompanyRequest request = aValidCreateCompanyRequest();

        MvcResult result = mockMvc.perform(post(COMPANIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        CompanyResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CompanyResponse.class
        );

        Integer rowCount = jdbcClient.sql("SELECT COUNT(*) FROM companies WHERE id = :id AND name = :name")
                .param("id", response.id())
                .param("name", request.name())
                .query(Integer.class)
                .single();

        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin@candidai.ua", roles = "ADMIN")
    void givenInvalidPayload_createCompany_shouldReturn400AndNotChangeRowCount() throws Exception {
        Integer countBefore = jdbcClient.sql("SELECT COUNT(*) FROM companies")
                .query(Integer.class)
                .single();

        CreateCompanyRequest invalidRequest = anInvalidCreateCompanyRequest();

        mockMvc.perform(post(COMPANIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        Integer countAfter = jdbcClient.sql("SELECT COUNT(*) FROM companies")
                .query(Integer.class)
                .single();

        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @WithMockUser(username = "admin@candidai.ua", roles = "ADMIN")
    void givenExistingCompany_getCompanyById_shouldReturn200AndMatchDatabase() throws Exception {
        CreateCompanyRequest request = aValidCreateCompanyRequest();

        MvcResult createResult = mockMvc.perform(post(COMPANIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        CompanyResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                CompanyResponse.class
        );

        MvcResult getResult = mockMvc.perform(get(COMPANIES_URL + "/" + created.id()))
                .andExpect(status().isOk())
                .andReturn();

        CompanyResponse fetched = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                CompanyResponse.class
        );

        String nameFromDb = jdbcClient.sql("SELECT name FROM companies WHERE id = :id")
                .param("id", created.id())
                .query(String.class)
                .single();

        assertThat(fetched.name()).isEqualTo(nameFromDb);
        assertThat(fetched.id()).isEqualTo(created.id());
    }

    @Test
    @WithMockUser(username = "recruiter@candidai.ua", roles = "RECRUITER")
    void givenRecruiterRole_createCompany_shouldReturn403Forbidden() throws Exception {
        CreateCompanyRequest request = aValidCreateCompanyRequest();

        mockMvc.perform(post(COMPANIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM companies WHERE name = :name")
                .param("name", request.name())
                .query(Integer.class)
                .single();

        assertThat(count).isEqualTo(0);
    }
}
