package ua.edu.ukma.candidai.company.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.company.CompanyTestResources;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.service.CompanyService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.company.CompanyTestResources.*;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyService companyService;

    @Test
    @DisplayName("POST /api/v1/companies - should create company and return 201 Created with Location header")
    void givenValidRequest_createCompany_shouldReturn201Created() throws Exception {
        // Setup / Fixtures
        CreateCompanyRequest request = sampleCreateCompanyRequest();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        // Stubbing / Mocking
        when(companyService.createCompany(request)).thenReturn(expectedResponse);

        // Execution / Action
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Assertions / Verification
        CompanyResponse actual = parseResponse(result, CompanyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(BASE_URL + "/" + expectedResponse.id());
    }

    @Test
    @DisplayName("POST /api/v1/companies - should return 400 ProblemDetail when name is blank")
    void givenBlankName_createCompany_shouldReturn400BadRequest() throws Exception {
        // Setup / Fixtures
        CreateCompanyRequest invalidRequest = sampleInvalidCreateCompanyRequest();

        // Stubbing / Mocking

        // Execution / Action
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assertions / Verification
        ProblemDetail problemDetail = parseProblemDetail(result);
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(extractErrors(problemDetail)).containsKey("name");
    }

    @Test
    @DisplayName("POST /api/v1/companies - should return 409 Conflict when name already exists")
    void givenDuplicateName_createCompany_shouldReturn409Conflict() throws Exception {
        // Setup / Fixtures
        CreateCompanyRequest request = sampleCreateCompanyRequest();
        String message = expectedDuplicateCompanyNameMessage(request.name());

        // Stubbing / Mocking
        when(companyService.createCompany(request))
                .thenThrow(new DuplicateResourceException(message));

        // Execution / Action
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        // Assertions / Verification
        ProblemDetail problemDetail = parseProblemDetail(result);
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Conflict");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    @Test
    @DisplayName("GET /api/v1/companies/{id} - should return 200 Ok when company exists")
    void givenExistingId_getCompanyById_shouldReturn200Ok() throws Exception {
        // Setup / Fixtures
        CompanyResponse expectedResponse = sampleCompanyResponse();

        // Stubbing / Mocking
        when(companyService.getCompanyById(DEFAULT_COMPANY_ID)).thenReturn(expectedResponse);

        // Execution / Action
        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + DEFAULT_COMPANY_ID))
                .andExpect(status().isOk())
                .andReturn();

        // Assertions / Verification
        CompanyResponse actual = parseResponse(result, CompanyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("GET /api/v1/companies/{id} - should return 404 ProblemDetail when company not found")
    void givenNonExistentId_getCompanyById_shouldReturn404NotFound() throws Exception {
        // Setup / Fixtures
        String message = expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID);

        // Stubbing / Mocking
        when(companyService.getCompanyById(NON_EXISTENT_COMPANY_ID))
                .thenThrow(new ResourceNotFoundException(message));

        // Execution / Action
        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_COMPANY_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assertions / Verification
        ProblemDetail problemDetail = parseProblemDetail(result);
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    @Test
    @DisplayName("GET /api/v1/companies - should return 200 Ok with all companies when no name param")
    void givenNoNameParam_getCompanies_shouldReturn200OkWithAllCompanies() throws Exception {
        // Setup / Fixtures
        CompanySummaryResponse summary = sampleCompanySummaryResponse();
        List<CompanySummaryResponse> expectedList = List.of(summary);

        // Stubbing / Mocking
        when(companyService.getCompanies(null)).thenReturn(expectedList);

        // Execution / Action
        MvcResult result = mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andReturn();

        // Assertions / Verification
        List<CompanySummaryResponse> actualList = parseResponseList(result, CompanySummaryResponse.class);
        assertThat(actualList).usingRecursiveComparison().isEqualTo(expectedList);
    }

    @Test
    @DisplayName("GET /api/v1/companies - should return 200 Ok with filtered companies when name param provided")
    void givenNameParam_getCompanies_shouldReturn200OkWithFilteredCompanies() throws Exception {
        // Setup / Fixtures
        String name = "Tech";
        CompanySummaryResponse summary = sampleCompanySummaryResponse();
        List<CompanySummaryResponse> expectedList = List.of(summary);

        // Stubbing / Mocking
        when(companyService.getCompanies(name)).thenReturn(expectedList);

        // Execution / Action
        MvcResult result = mockMvc.perform(get(BASE_URL).param("name", name))
                .andExpect(status().isOk())
                .andReturn();

        // Assertions / Verification
        List<CompanySummaryResponse> actualList = parseResponseList(result, CompanySummaryResponse.class);
        assertThat(actualList).usingRecursiveComparison().isEqualTo(expectedList);
    }

    @Test
    @DisplayName("PUT /api/v1/companies/{id} - should return 200 Ok and updated company")
    void givenValidRequest_updateCompany_shouldReturn200Ok() throws Exception {
        // Setup / Fixtures
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();
        CompanyResponse expectedResponse = sampleUpdatedCompanyResponse();

        // Stubbing / Mocking
        when(companyService.updateCompany(DEFAULT_COMPANY_ID, request)).thenReturn(expectedResponse);

        // Execution / Action
        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + DEFAULT_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Assertions / Verification
        CompanyResponse actual = parseResponse(result, CompanyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /api/v1/companies/{id} - should return 400 ProblemDetail when name is blank")
    void givenBlankName_updateCompany_shouldReturn400BadRequest() throws Exception {
        // Setup / Fixtures
        UpdateCompanyRequest invalidRequest = sampleInvalidUpdateCompanyRequest();

        // Stubbing / Mocking

        // Execution / Action
        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + DEFAULT_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assertions / Verification
        ProblemDetail problemDetail = parseProblemDetail(result);
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(extractErrors(problemDetail)).containsKey("name");
    }

    @Test
    @DisplayName("PUT /api/v1/companies/{id} - should return 404 ProblemDetail when company not found")
    void givenNonExistentId_updateCompany_shouldReturn404NotFound() throws Exception {
        // Setup / Fixtures
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();
        String message = expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID);

        // Stubbing / Mocking
        when(companyService.updateCompany(NON_EXISTENT_COMPANY_ID, request))
                .thenThrow(new ResourceNotFoundException(message));

        // Execution / Action
        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + NON_EXISTENT_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assertions / Verification
        ProblemDetail problemDetail = parseProblemDetail(result);
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    @Test
    @DisplayName("DELETE /api/v1/companies/{id} - should return 204 No Content when company deleted")
    void givenExistingId_deleteCompany_shouldReturn204NoContent() throws Exception {
        // Setup / Fixtures

        // Stubbing / Mocking
        doNothing().when(companyService).deleteCompany(DEFAULT_COMPANY_ID);

        // Execution / Action
        mockMvc.perform(delete(BASE_URL + "/" + DEFAULT_COMPANY_ID))
                .andExpect(status().isNoContent());

        // Assertions / Verification
        verify(companyService).deleteCompany(DEFAULT_COMPANY_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/companies/{id} - should return 404 ProblemDetail when company not found")
    void givenNonExistentId_deleteCompany_shouldReturn404NotFound() throws Exception {
        // Setup / Fixtures
        String message = expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID);

        // Stubbing / Mocking
        doThrow(new ResourceNotFoundException(message)).when(companyService).deleteCompany(NON_EXISTENT_COMPANY_ID);

        // Execution / Action
        MvcResult result = mockMvc.perform(delete(BASE_URL + "/" + NON_EXISTENT_COMPANY_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assertions / Verification
        ProblemDetail problemDetail = parseProblemDetail(result);
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    private <T> T parseResponse(MvcResult result, Class<T> clazz) throws Exception {
        return CompanyTestResources.parseResponse(objectMapper, result, clazz);
    }

    private <T> List<T> parseResponseList(MvcResult result, Class<T> elementType) throws Exception {
        return CompanyTestResources.parseResponseList(objectMapper, result, elementType);
    }

    private ProblemDetail parseProblemDetail(MvcResult result) throws Exception {
        return CompanyTestResources.parseProblemDetail(objectMapper, result);
    }
}
