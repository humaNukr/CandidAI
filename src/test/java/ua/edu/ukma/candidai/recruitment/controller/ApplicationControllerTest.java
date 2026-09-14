package ua.edu.ukma.candidai.recruitment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.recruitment.dto.model.InterviewDecision;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.BASE_URL;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.DEFAULT_ID;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.JSON_PARSING_ERROR_JSON;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.JSON_WITH_UNKNOWN_PROPERTY;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.NON_EXISTENT_ID;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.VALIDATION_ERROR_JSON;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.anApplicationResponse;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.notFoundProblemDetailJson;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.validSubmitFeedbackRequest;
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.validUpdateStatusRequest;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationController applicationController;

    @BeforeEach
    void setUp() {
        applicationController.saveApplication(anApplicationResponse());
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status - should update status and return 200 Ok")
    void givenValidStatusUpdate_updateApplicationStatus_shouldReturn200Ok() throws Exception {
        UpdateApplicationStatusRequest request = validUpdateStatusRequest();

        mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$.status").value("INTERVIEW"))
                .andExpect(jsonPath("$.comment").value("Candidate passed screening successfully"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status - should return 400 ProblemDetail when status is null")
    void givenNullStatus_updateApplicationStatus_shouldReturn400BadRequest() throws Exception {
        UpdateApplicationStatusRequest invalidRequest = new UpdateApplicationStatusRequest(null, "comment");

        mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status - should return 404 ProblemDetail when application not found")
    void givenNonExistentId_updateApplicationStatus_shouldReturn404NotFound() throws Exception {
        UpdateApplicationStatusRequest request = validUpdateStatusRequest();

        mockMvc.perform(patch(BASE_URL + "/" + NON_EXISTENT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status - should return 400 when unknown property is provided")
    void givenUnknownProperty_updateApplicationStatus_shouldReturn400BadRequest() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_WITH_UNKNOWN_PROPERTY))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(JSON_PARSING_ERROR_JSON));
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should submit feedback and return 201 Created")
    void givenValidRequest_submitFeedback_shouldReturn201Created() throws Exception {
        SubmitInterviewFeedbackRequest request = validSubmitFeedbackRequest();

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")))
                .andExpect(jsonPath("$.applicationId").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$.interviewerName").value("Alex Techlead"))
                .andExpect(jsonPath("$.technicalScore").value(4))
                .andExpect(jsonPath("$.notes").value("Strong knowledge of Java and Spring Boot architecture"))
                .andExpect(jsonPath("$.decision").value("HIRE"));
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should return 400 when technical score is below 1")
    void givenTechnicalScoreBelowMin_submitFeedback_shouldReturn400BadRequest() throws Exception {
        SubmitInterviewFeedbackRequest invalidRequest = new SubmitInterviewFeedbackRequest(
                "Alex Techlead",
                0,
                "Valid notes",
                InterviewDecision.HIRE
        );

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.technicalScore").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should return 400 when technical score is above 5")
    void givenTechnicalScoreAboveMax_submitFeedback_shouldReturn400BadRequest() throws Exception {
        SubmitInterviewFeedbackRequest invalidRequest = new SubmitInterviewFeedbackRequest(
                "Alex Techlead",
                6,
                "Valid notes",
                InterviewDecision.HIRE
        );

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.technicalScore").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should return 400 when notes are blank")
    void givenBlankNotes_submitFeedback_shouldReturn400BadRequest() throws Exception {
        SubmitInterviewFeedbackRequest invalidRequest = new SubmitInterviewFeedbackRequest(
                "Alex Techlead",
                4,
                "   ",
                InterviewDecision.HIRE
        );

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.notes").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should return 400 when interviewer name is blank")
    void givenBlankInterviewerName_submitFeedback_shouldReturn400BadRequest() throws Exception {
        SubmitInterviewFeedbackRequest invalidRequest = new SubmitInterviewFeedbackRequest(
                "",
                4,
                "Valid notes",
                InterviewDecision.HIRE
        );

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.interviewerName").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should return 400 when decision is null")
    void givenNullDecision_submitFeedback_shouldReturn400BadRequest() throws Exception {
        SubmitInterviewFeedbackRequest invalidRequest = new SubmitInterviewFeedbackRequest(
                "Alex Techlead",
                4,
                "Valid notes",
                null
        );

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.decision").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should return 404 when application not found")
    void givenNonExistentId_submitFeedback_shouldReturn404NotFound() throws Exception {
        SubmitInterviewFeedbackRequest request = validSubmitFeedbackRequest();

        mockMvc.perform(post(BASE_URL + "/" + NON_EXISTENT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - should return 400 when unknown property is provided")
    void givenUnknownProperty_submitFeedback_shouldReturn400BadRequest() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_WITH_UNKNOWN_PROPERTY))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(JSON_PARSING_ERROR_JSON));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id}/feedbacks - should return 200 Ok with feedbacks list")
    void givenExistingId_getFeedbacks_shouldReturn200OkWithFeedbacksList() throws Exception {
        SubmitInterviewFeedbackRequest request = validSubmitFeedbackRequest();

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get(BASE_URL + "/" + DEFAULT_ID + "/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$[0].interviewerName").value("Alex Techlead"))
                .andExpect(jsonPath("$[0].technicalScore").value(4));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id}/feedbacks - should return 404 ProblemDetail when application not found")
    void givenNonExistentId_getFeedbacks_shouldReturn404NotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_ID + "/feedbacks"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));
    }
}
