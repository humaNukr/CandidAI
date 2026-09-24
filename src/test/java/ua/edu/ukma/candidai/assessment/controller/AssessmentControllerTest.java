package ua.edu.ukma.candidai.assessment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssessmentController.class)
class AssessmentControllerTest {

    private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String BASE_URL = "/api/v1/applications/" + APP_ID + "/screening";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @Test
    @DisplayName("GET /api/v1/applications/{id}/screening - should return 200 Ok when screening result exists")
    void givenExistingScreening_getScreeningResult_shouldReturn200Ok() throws Exception {
        AiScreeningResult result = AiScreeningResult.completed(
                APP_ID, VACANCY_ID, 88, true, "Strong fit",
                List.of("Java"), List.of(), List.of("Q1"), Instant.parse("2026-09-21T10:00:00Z")
        );

        when(assessmentService.getScreeningResult(APP_ID)).thenReturn(result);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(APP_ID.toString()))
                .andExpect(jsonPath("$.matchingScore").value(88))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.summary").value("Strong fit"))
                .andExpect(jsonPath("$.suggestedQuestions[0]").value("Q1"));

        verify(assessmentService).getScreeningResult(APP_ID);
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id}/screening - should return 404 ProblemDetail when result not found")
    void givenNonExistentId_getScreeningResult_shouldReturn404NotFound() throws Exception {
        when(assessmentService.getScreeningResult(APP_ID))
                .thenThrow(new ResourceNotFoundException("Screening result not found for application: " + APP_ID));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(assessmentService).getScreeningResult(APP_ID);
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/screening - should trigger screening and return 200 Ok")
    void givenApplicationId_triggerScreening_shouldReturn200Ok() throws Exception {
        AiScreeningResult result = AiScreeningResult.completed(
                APP_ID, VACANCY_ID, 88, true, "Strong fit",
                List.of("Java"), List.of(), List.of("Q1"), Instant.parse("2026-09-21T10:00:00Z")
        );

        when(assessmentService.executeScreening(APP_ID)).thenReturn(result);

        mockMvc.perform(post(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(APP_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.matchingScore").value(88));

        verify(assessmentService).executeScreening(APP_ID);
    }
}
