package ua.edu.ukma.candidai.company.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

import java.net.URI;
import java.util.List;
import java.util.Map;

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
        CreateCompanyRequest request = sampleCreateCompanyRequest();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        when(companyService.createCompany(request)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        CompanyResponse actual = parseResponse(result, CompanyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(BASE_URL + "/" + expectedResponse.id());
    }

    @Test
    @DisplayName("POST /api/v1/companies - should return 400 ProblemDetail when name is blank")
    void givenBlankName_createCompany_shouldReturn400BadRequest() throws Exception {
        CreateCompanyRequest invalidRequest = sampleInvalidCreateCompanyRequest();
        ProblemDetail expectedProblemDetail = expectedValidationProblemDetail(Map.of("name", "must not be blank"));

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(result);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("properties.timestamp")
                .isEqualTo(expectedProblemDetail);
    }

    @Test
    @DisplayName("POST /api/v1/companies - should return 409 Conflict when name already exists")
    void givenDuplicateName_createCompany_shouldReturn409Conflict() throws Exception {
        CreateCompanyRequest request = sampleCreateCompanyRequest();
        String message = expectedDuplicateCompanyNameMessage(request.name());
        ProblemDetail expectedProblemDetail = expectedConflictProblemDetail(request.name());

        when(companyService.createCompany(request))
                .thenThrow(new DuplicateResourceException(message));

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(result);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("properties.timestamp")
                .isEqualTo(expectedProblemDetail);
    }

    @Test
    @DisplayName("GET /api/v1/companies/{id} - should return 200 Ok when company exists")
    void givenExistingId_getCompanyById_shouldReturn200Ok() throws Exception {
        CompanyResponse expectedResponse = sampleCompanyResponse();

        when(companyService.getCompanyById(DEFAULT_COMPANY_ID)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + DEFAULT_COMPANY_ID))
                .andExpect(status().isOk())
                .andReturn();

        CompanyResponse actual = parseResponse(result, CompanyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("GET /api/v1/companies/{id} - should return 404 ProblemDetail when company not found")
    void givenNonExistentId_getCompanyById_shouldReturn404NotFound() throws Exception {
        String message = expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID);
        ProblemDetail expectedProblemDetail = expectedNotFoundProblemDetail(NON_EXISTENT_COMPANY_ID);

        when(companyService.getCompanyById(NON_EXISTENT_COMPANY_ID))
                .thenThrow(new ResourceNotFoundException(message));

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_COMPANY_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(result);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("properties.timestamp")
                .isEqualTo(expectedProblemDetail);
    }

    @Test
    @DisplayName("GET /api/v1/companies - should return 200 Ok with all companies when no name param")
    void givenNoNameParam_getCompanies_shouldReturn200OkWithAllCompanies() throws Exception {
        CompanySummaryResponse summary = sampleCompanySummaryResponse();
        List<CompanySummaryResponse> expectedList = List.of(summary);

        when(companyService.getCompanies(null)).thenReturn(expectedList);

        MvcResult result = mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andReturn();

        List<CompanySummaryResponse> actualList = parseResponseList(result, CompanySummaryResponse.class);
        assertThat(actualList).usingRecursiveComparison().isEqualTo(expectedList);
    }

    @Test
    @DisplayName("GET /api/v1/companies - should return 200 Ok with filtered companies when name param provided")
    void givenNameParam_getCompanies_shouldReturn200OkWithFilteredCompanies() throws Exception {
        String name = "Tech";
        CompanySummaryResponse summary = sampleCompanySummaryResponse();
        List<CompanySummaryResponse> expectedList = List.of(summary);

        when(companyService.getCompanies(name)).thenReturn(expectedList);

        MvcResult result = mockMvc.perform(get(BASE_URL).param("name", name))
                .andExpect(status().isOk())
                .andReturn();

        List<CompanySummaryResponse> actualList = parseResponseList(result, CompanySummaryResponse.class);
        assertThat(actualList).usingRecursiveComparison().isEqualTo(expectedList);
    }

    @Test
    @DisplayName("PUT /api/v1/companies/{id} - should return 200 Ok and updated company")
    void givenValidRequest_updateCompany_shouldReturn200Ok() throws Exception {
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();
        CompanyResponse expectedResponse = sampleUpdatedCompanyResponse();

        when(companyService.updateCompany(DEFAULT_COMPANY_ID, request)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + DEFAULT_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        CompanyResponse actual = parseResponse(result, CompanyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /api/v1/companies/{id} - should return 400 ProblemDetail when name is blank")
    void givenBlankName_updateCompany_shouldReturn400BadRequest() throws Exception {
        UpdateCompanyRequest invalidRequest = sampleInvalidUpdateCompanyRequest();
        ProblemDetail expectedProblemDetail = expectedValidationProblemDetail(
                URI.create(BASE_URL + "/" + DEFAULT_COMPANY_ID),
                Map.of("name", "must not be blank")
        );

        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + DEFAULT_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(result);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("properties.timestamp")
                .isEqualTo(expectedProblemDetail);
    }

    @Test
    @DisplayName("PUT /api/v1/companies/{id} - should return 404 ProblemDetail when company not found")
    void givenNonExistentId_updateCompany_shouldReturn404NotFound() throws Exception {
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();
        String message = expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID);
        ProblemDetail expectedProblemDetail = expectedNotFoundProblemDetail(NON_EXISTENT_COMPANY_ID);

        when(companyService.updateCompany(NON_EXISTENT_COMPANY_ID, request))
                .thenThrow(new ResourceNotFoundException(message));

        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + NON_EXISTENT_COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(result);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("properties.timestamp")
                .isEqualTo(expectedProblemDetail);
    }

    @Test
    @DisplayName("DELETE /api/v1/companies/{id} - should return 204 No Content when company deleted")
    void givenExistingId_deleteCompany_shouldReturn204NoContent() throws Exception {
        doNothing().when(companyService).deleteCompany(DEFAULT_COMPANY_ID);

        mockMvc.perform(delete(BASE_URL + "/" + DEFAULT_COMPANY_ID))
                .andExpect(status().isNoContent());

        verify(companyService).deleteCompany(DEFAULT_COMPANY_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/companies/{id} - should return 404 ProblemDetail when company not found")
    void givenNonExistentId_deleteCompany_shouldReturn404NotFound() throws Exception {
        String message = expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID);
        ProblemDetail expectedProblemDetail = expectedNotFoundProblemDetail(NON_EXISTENT_COMPANY_ID);

        doThrow(new ResourceNotFoundException(message)).when(companyService).deleteCompany(NON_EXISTENT_COMPANY_ID);

        MvcResult result = mockMvc.perform(delete(BASE_URL + "/" + NON_EXISTENT_COMPANY_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(result);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("properties.timestamp")
                .isEqualTo(expectedProblemDetail);
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
