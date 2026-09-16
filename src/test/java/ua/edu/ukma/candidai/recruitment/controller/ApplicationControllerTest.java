package ua.edu.ukma.candidai.recruitment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
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
import static ua.edu.ukma.candidai.recruitment.controller.TestResources.*;

@WebMvcTest(ApplicationController.class)
@Import(CommonGenerator.class)
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
        UpdateApplicationStatusRequest invalidRequest = aUpdateStatusRequest().status(null).build();

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
        SubmitInterviewFeedbackRequest invalidRequest = aSubmitFeedbackRequest().technicalScore(0).build();

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
        SubmitInterviewFeedbackRequest invalidRequest = aSubmitFeedbackRequest().technicalScore(6).build();

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
        SubmitInterviewFeedbackRequest invalidRequest = aSubmitFeedbackRequest().notes("   ").build();

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
        SubmitInterviewFeedbackRequest invalidRequest = aSubmitFeedbackRequest().interviewerName("").build();

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
        SubmitInterviewFeedbackRequest invalidRequest = aSubmitFeedbackRequest().decision(null).build();

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

    @Test
    @DisplayName("POST /api/v1/applications - should create application and return 201 Created")
    void givenValidRequest_applyForVacancy_shouldReturn201Created() throws Exception {
        ApplyForVacancyRequest request = validApplyRequest();
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL)))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.vacancyId").value(DEFAULT_VACANCY_ID.toString()))
                .andExpect(jsonPath("$.candidateName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("+380501234567"))
                .andExpect(jsonPath("$.resumeUrl").value("https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/applications - should return 400 ProblemDetail when email is invalid")
    void givenInvalidEmail_applyForVacancy_shouldReturn400BadRequest() throws Exception {
        ApplyForVacancyRequest invalidRequest = anApplyRequest().email("invalid-email-format").build();
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications - should return 400 ProblemDetail when candidate name is blank")
    void givenBlankCandidateName_applyForVacancy_shouldReturn400BadRequest() throws Exception {
        ApplyForVacancyRequest invalidRequest = anApplyRequest().candidateName("   ").build();
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.candidateName").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications - should return 400 ProblemDetail when phone is invalid")
    void givenInvalidPhone_applyForVacancy_shouldReturn400BadRequest() throws Exception {
        ApplyForVacancyRequest invalidRequest = anApplyRequest().phone("abc12345").build();
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications - should return 400 ProblemDetail when vacancyId is null")
    void givenNullVacancyId_applyForVacancy_shouldReturn400BadRequest() throws Exception {
        ApplyForVacancyRequest invalidRequest = anApplyRequest().vacancyId(null).build();
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.vacancyId").exists());
    }

    @Test
    @DisplayName("POST /api/v1/applications - should return 400 when unknown property is provided")
    void givenUnknownProperty_applyForVacancy_shouldReturn400BadRequest() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_WITH_UNKNOWN_PROPERTY))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(JSON_PARSING_ERROR_JSON));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} - should return 200 Ok with application details")
    void givenExistingId_getApplicationById_shouldReturn200Ok() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + DEFAULT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$.candidateName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} - should return 404 ProblemDetail when application not found")
    void givenNonExistentId_getApplicationById_shouldReturn404NotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));
    }
}
