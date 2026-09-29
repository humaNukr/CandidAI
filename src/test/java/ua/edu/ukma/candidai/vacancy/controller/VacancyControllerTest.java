package ua.edu.ukma.candidai.vacancy.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

@WebMvcTest(VacancyController.class)
@EnableSpringDataWebSupport
class VacancyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VacancyService vacancyService;

    @Test
    @DisplayName("POST /api/v1/vacancies - should create vacancy and return 201 with Location header")
    void givenValidRequest_createVacancy_shouldReturn201Created() throws Exception {
        CreateVacancyRequest request = validCreateVacancyRequest();
        VacancyResponse expectedResponse = aVacancyResponse();

        when(vacancyService.createVacancy(request)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + BASE_URL + "/" + expectedResponse.id()))
                .andReturn();

        VacancyResponse actual = parseResponse(objectMapper, result, VacancyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("POST /api/v1/vacancies - should return 400 ProblemDetail when title is blank")
    void givenBlankTitle_createVacancy_shouldReturn400BadRequest() throws Exception {
        CreateVacancyRequest invalidRequest = aCreateVacancyRequest().title("").build();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(objectMapper, result);
        assertThat(actual.getStatus()).isEqualTo(400);
        assertThat(actual.getTitle()).isEqualTo("Validation Error");
        assertThat(actual.getType()).isEqualTo(URI.create("https://candidai.ukma.edu.ua/errors/validation"));
        assertThat(extractErrors(actual)).containsKey("title");
    }

    @Test
    @DisplayName("POST /api/v1/vacancies - should return 400 ProblemDetail when salary min is greater than max")
    void givenInvalidSalaryRange_createVacancy_shouldReturn400BadRequest() throws Exception {
        CreateVacancyRequest invalidRequest = aCreateVacancyRequest()
                .salaryMin(BigDecimal.valueOf(6000))
                .salaryMax(BigDecimal.valueOf(3000))
                .build();
        ProblemDetail expected = expectedValidationProblemDetail(
                Map.of("salaryMin", "Minimum salary cannot be greater than maximum salary")
        );

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(objectMapper, result);
        assertThat(actual).usingRecursiveComparison().ignoringFields("properties.timestamp").isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /api/v1/vacancies - should return 400 ProblemDetail when unknown property is provided")
    void givenUnknownProperty_createVacancy_shouldReturn400BadRequest() throws Exception {
        ProblemDetail expected = expectedJsonParsingProblemDetail();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_WITH_UNKNOWN_PROPERTY))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(objectMapper, result);
        assertThat(actual).usingRecursiveComparison().ignoringFields("properties.timestamp").isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /api/v1/vacancies/{id} - should return 200 and vacancy when exists")
    void givenExistingId_getVacancyById_shouldReturn200Ok() throws Exception {
        VacancyResponse expectedResponse = aVacancyResponse();

        when(vacancyService.getVacancyById(DEFAULT_ID)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + DEFAULT_ID))
                .andExpect(status().isOk())
                .andReturn();

        VacancyResponse actual = parseResponse(objectMapper, result, VacancyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("GET /api/v1/vacancies/{id} - should return 404 ProblemDetail when vacancy not found")
    void givenNonExistentId_getVacancyById_shouldReturn404NotFound() throws Exception {
        ProblemDetail expected = expectedNotFoundProblemDetail(NON_EXISTENT_ID);

        when(vacancyService.getVacancyById(NON_EXISTENT_ID))
                .thenThrow(new ResourceNotFoundException("Vacancy not found with id: " + NON_EXISTENT_ID));

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(objectMapper, result);
        assertThat(actual).usingRecursiveComparison().ignoringFields("properties.timestamp").isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /api/v1/vacancies - should return 200 with paged content")
    void givenValidParams_getAllVacancies_shouldReturn200OkWithPagedContent() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        VacancyResponse expectedResponse = aVacancyResponse();
        PageImpl<VacancyResponse> expectedPage = new PageImpl<>(List.of(expectedResponse));

        when(vacancyService.getAllVacancies(VacancyStatus.OPEN, JobCategory.ENGINEERING, pageable))
                .thenReturn(expectedPage);

        MvcResult result = mockMvc.perform(get(BASE_URL)
                        .param("status", "OPEN")
                        .param("category", "ENGINEERING"))
                .andExpect(status().isOk())
                .andReturn();

        List<VacancyResponse> actual = parsePagedContent(objectMapper, result, VacancyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(expectedResponse));
    }

    @Test
    @DisplayName("PATCH /api/v1/vacancies/{id}/status - should update status and return 200")
    void givenValidStatusUpdate_updateVacancyStatus_shouldReturn200Ok() throws Exception {
        UpdateVacancyStatusRequest request = validUpdateVacancyStatusRequest();
        VacancyResponse expectedResponse = aVacancyResponse();

        when(vacancyService.updateVacancyStatus(DEFAULT_ID, request)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        VacancyResponse actual = parseResponse(objectMapper, result, VacancyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("DELETE /api/v1/vacancies/{id} - should delete vacancy and return 204")
    void givenExistingId_deleteVacancy_shouldReturn204NoContent() throws Exception {
        doNothing().when(vacancyService).deleteVacancy(DEFAULT_ID);

        mockMvc.perform(delete(BASE_URL + "/" + DEFAULT_ID))
                .andExpect(status().isNoContent());

        verify(vacancyService).deleteVacancy(DEFAULT_ID);
    }

    @Test
    @DisplayName("POST /api/v1/vacancies - duplicate vacancy should return 409 Conflict with ProblemDetail")
    void givenDuplicateVacancy_createVacancy_shouldReturn409ConflictWithProblemDetail() throws Exception {
        CreateVacancyRequest request = validCreateVacancyRequest();
        String message = "Vacancy already exists with title: " + request.title();
        ProblemDetail expected = expectedConflictProblemDetail(message);

        when(vacancyService.createVacancy(any(CreateVacancyRequest.class)))
                .thenThrow(new DuplicateResourceException(message));

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(objectMapper, result);
        assertThat(actual).usingRecursiveComparison().ignoringFields("properties.timestamp").isEqualTo(expected);
    }

    @Test
    @DisplayName("PATCH /api/v1/vacancies/{id}/status - invalid transition should return 422 with ProblemDetail")
    void givenInvalidTransition_updateVacancyStatus_shouldReturn422UnprocessableWithProblemDetail() throws Exception {
        UpdateVacancyStatusRequest request = validUpdateVacancyStatusRequest();
        String message = "Cannot transition from CLOSED to DRAFT";
        ProblemDetail expected = expectedInvalidStateTransitionProblemDetail(message);

        when(vacancyService.updateVacancyStatus(eq(DEFAULT_ID), any(UpdateVacancyStatusRequest.class)))
                .thenThrow(new InvalidStateTransitionException(message));

        MvcResult result = mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        ProblemDetail actual = parseProblemDetail(objectMapper, result);
        assertThat(actual).usingRecursiveComparison().ignoringFields("properties.timestamp").isEqualTo(expected);
    }
}
