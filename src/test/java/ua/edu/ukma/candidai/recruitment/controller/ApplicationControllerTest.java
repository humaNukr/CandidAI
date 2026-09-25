package ua.edu.ukma.candidai.recruitment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.*;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationService applicationService;

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status - should update status and return 200 Ok")
    void givenValidStatusUpdate_updateApplicationStatus_shouldReturn200Ok() throws Exception {
        UpdateApplicationStatusRequest request = validUpdateStatusRequest();
        ApplicationResponse updatedResponse = updatedApplicationResponse();

        when(applicationService.updateStatus(eq(DEFAULT_ID), any(UpdateApplicationStatusRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$.status").value("INTERVIEW"))
                .andExpect(jsonPath("$.comment").value("Candidate passed screening successfully"));

        verify(applicationService).updateStatus(eq(DEFAULT_ID), any(UpdateApplicationStatusRequest.class));
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
    @DisplayName("PATCH /api/v1/applications/{id}/status - should return 400 when REJECTED without comment")
    void givenRejectedStatusWithoutComment_updateApplicationStatus_shouldReturn400BadRequest() throws Exception {
        UpdateApplicationStatusRequest invalidRequest = aUpdateStatusRequest()
                .status(ApplicationStatus.REJECTED)
                .comment(null)
                .build();

        mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.comment").value("Comment is required when status is REJECTED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status - should return 404 ProblemDetail when application not found")
    void givenNonExistentId_updateApplicationStatus_shouldReturn404NotFound() throws Exception {
        UpdateApplicationStatusRequest request = validUpdateStatusRequest();
        when(applicationService.updateStatus(eq(NON_EXISTENT_ID), any(UpdateApplicationStatusRequest.class)))
                .thenThrow(new ResourceNotFoundException("Application not found with id: " + NON_EXISTENT_ID));

        mockMvc.perform(patch(BASE_URL + "/" + NON_EXISTENT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));

        verify(applicationService).updateStatus(eq(NON_EXISTENT_ID), any(UpdateApplicationStatusRequest.class));
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
        InterviewFeedbackResponse feedback = anInterviewFeedbackResponse();

        when(applicationService.submitFeedback(eq(DEFAULT_ID), any(SubmitInterviewFeedbackRequest.class)))
                .thenReturn(feedback);

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL + "/" + DEFAULT_ID)))
                .andExpect(jsonPath("$.applicationId").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$.interviewerName").value("Alex Techlead"))
                .andExpect(jsonPath("$.technicalScore").value(4))
                .andExpect(jsonPath("$.notes").value("Strong knowledge of Java and Spring Boot architecture"))
                .andExpect(jsonPath("$.decision").value("HIRE"));

        verify(applicationService).submitFeedback(eq(DEFAULT_ID), any(SubmitInterviewFeedbackRequest.class));
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
        when(applicationService.submitFeedback(eq(NON_EXISTENT_ID), any(SubmitInterviewFeedbackRequest.class)))
                .thenThrow(new ResourceNotFoundException("Application not found with id: " + NON_EXISTENT_ID));

        mockMvc.perform(post(BASE_URL + "/" + NON_EXISTENT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));

        verify(applicationService).submitFeedback(eq(NON_EXISTENT_ID), any(SubmitInterviewFeedbackRequest.class));
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
        InterviewFeedbackResponse fb = anInterviewFeedbackResponse("Good");
        when(applicationService.getFeedbacks(DEFAULT_ID)).thenReturn(List.of(fb));

        mockMvc.perform(get(BASE_URL + "/" + DEFAULT_ID + "/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$[0].interviewerName").value("Alex Techlead"))
                .andExpect(jsonPath("$[0].technicalScore").value(4));

        verify(applicationService).getFeedbacks(DEFAULT_ID);
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id}/feedbacks - should return 404 ProblemDetail when application not found")
    void givenNonExistentId_getFeedbacks_shouldReturn404NotFound() throws Exception {
        when(applicationService.getFeedbacks(NON_EXISTENT_ID))
                .thenThrow(new ResourceNotFoundException("Application not found with id: " + NON_EXISTENT_ID));

        mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_ID + "/feedbacks"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));

        verify(applicationService).getFeedbacks(NON_EXISTENT_ID);
    }

    @Test
    @DisplayName("POST /api/v1/applications - should create application and return 201 Created")
    void givenValidRequest_applyForVacancy_shouldReturn201Created() throws Exception {
        ApplyForVacancyRequest request = validApplyRequest();
        when(applicationService.apply(any(ApplyForVacancyRequest.class))).thenReturn(anApplicationResponse());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL)))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.vacancyId").value(DEFAULT_VACANCY_ID.toString()))
                .andExpect(jsonPath("$.candidateId").value(DEFAULT_CANDIDATE_ID.toString()))
                .andExpect(jsonPath("$.candidateName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("+380501234567"))
                .andExpect(jsonPath("$.resumeUrl").value("https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").isNotEmpty());

        verify(applicationService).apply(any(ApplyForVacancyRequest.class));
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
    @DisplayName("POST /api/v1/applications - should return 400 ProblemDetail when candidateId is null")
    void givenNullCandidateId_applyForVacancy_shouldReturn400BadRequest() throws Exception {
        ApplyForVacancyRequest invalidRequest = anApplyRequest().candidateId(null).build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(VALIDATION_ERROR_JSON))
                .andExpect(jsonPath("$.errors.candidateId").exists());
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
        when(applicationService.getById(DEFAULT_ID)).thenReturn(anApplicationResponse());

        mockMvc.perform(get(BASE_URL + "/" + DEFAULT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$.candidateName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.status").value("APPLIED"));

        verify(applicationService).getById(DEFAULT_ID);
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} - should return 404 ProblemDetail when application not found")
    void givenNonExistentId_getApplicationById_shouldReturn404NotFound() throws Exception {
        when(applicationService.getById(NON_EXISTENT_ID))
                .thenThrow(new ResourceNotFoundException("Application not found with id: " + NON_EXISTENT_ID));

        mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().json(notFoundProblemDetailJson(NON_EXISTENT_ID)));

        verify(applicationService).getById(NON_EXISTENT_ID);
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id}/evaluation - should return 200 Ok with evaluation result")
    void givenExistingId_getEvaluation_shouldReturn200Ok() throws Exception {
        EvaluationResult evaluation = anEvaluationResult();
        when(applicationService.evaluateCandidate(DEFAULT_ID)).thenReturn(evaluation);

        mockMvc.perform(get(BASE_URL + "/" + DEFAULT_ID + "/evaluation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(4.5))
                .andExpect(jsonPath("$.recommendedDecision").value("HIRE"))
                .andExpect(jsonPath("$.summaryReason").value("Engineering evaluation completed"));

        verify(applicationService).evaluateCandidate(DEFAULT_ID);
    }

    @Test
    @DisplayName("GET /api/v1/applications?vacancyId={id} - should return 200 Ok with list of applications")
    void givenVacancyId_getApplications_shouldReturn200OkWithApplications() throws Exception {
        when(applicationService.getApplicationsByVacancy(DEFAULT_VACANCY_ID, false))
                .thenReturn(List.of(anApplicationResponse()));

        mockMvc.perform(get(BASE_URL).param("vacancyId", DEFAULT_VACANCY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DEFAULT_ID.toString()))
                .andExpect(jsonPath("$[0].vacancyId").value(DEFAULT_VACANCY_ID.toString()))
                .andExpect(jsonPath("$[0].candidateName").value("John Doe"));

        verify(applicationService).getApplicationsByVacancy(DEFAULT_VACANCY_ID, false);
    }

    @Test
    @DisplayName("GET /api/v1/applications?vacancyId={id}&sortByScore=true - should pass sortByScore to service")
    void givenSortByScore_getApplications_shouldCallServiceWithSortFlag() throws Exception {
        when(applicationService.getApplicationsByVacancy(DEFAULT_VACANCY_ID, true))
                .thenReturn(List.of(anApplicationResponse()));

        mockMvc.perform(get(BASE_URL)
                        .param("vacancyId", DEFAULT_VACANCY_ID.toString())
                        .param("sortByScore", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DEFAULT_ID.toString()));

        verify(applicationService).getApplicationsByVacancy(DEFAULT_VACANCY_ID, true);
    }

    @Test
    @DisplayName("GET /api/v1/applications - should return 200 Ok with empty list when no vacancyId provided")
    void givenNoVacancyId_getApplications_shouldReturn200OkWithEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("POST /api/v1/applications - duplicate candidate should return 409 Conflict with ProblemDetail")
    void givenDuplicateCandidate_applyForVacancy_shouldReturn409ConflictWithProblemDetail() throws Exception {
        ApplyForVacancyRequest request = validApplyRequest();
        when(applicationService.apply(any(ApplyForVacancyRequest.class)))
                .thenThrow(new DuplicateResourceException("Candidate already applied with email " + request.email()));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("Candidate already applied with email " + request.email()))
                .andExpect(jsonPath("$.type").value("https://candidai.ukma.edu.ua/errors/conflict"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status - invalid transition should return 422 with ProblemDetail")
    void givenInvalidTransition_updateStatus_shouldReturn422UnprocessableWithProblemDetail() throws Exception {
        UpdateApplicationStatusRequest request = validUpdateStatusRequest();
        when(applicationService.updateStatus(eq(DEFAULT_ID), any()))
                .thenThrow(new InvalidStateTransitionException("Cannot transition from APPLIED to OFFER"));

        mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Invalid State Transition"))
                .andExpect(jsonPath("$.detail").value("Cannot transition from APPLIED to OFFER"))
                .andExpect(jsonPath("$.type").value("https://candidai.ukma.edu.ua/errors/invalid-state-transition"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/feedbacks - non-interview status should return 422 with ProblemDetail")
    void givenNonInterviewStatus_submitFeedback_shouldReturn422UnprocessableWithProblemDetail() throws Exception {
        SubmitInterviewFeedbackRequest request = validSubmitFeedbackRequest();
        when(applicationService.submitFeedback(eq(DEFAULT_ID), any()))
                .thenThrow(new InvalidStateTransitionException(
                        "Cannot submit feedback for application in status: APPLIED. Expected: INTERVIEW"
                ));

        mockMvc.perform(post(BASE_URL + "/" + DEFAULT_ID + "/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Invalid State Transition"))
                .andExpect(jsonPath("$.type").value("https://candidai.ukma.edu.ua/errors/invalid-state-transition"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
