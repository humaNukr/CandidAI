package ua.edu.ukma.candidai.interview.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.common.exception.GlobalExceptionHandler;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.interview.dto.CancelInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.InterviewResponse;
import ua.edu.ukma.candidai.interview.dto.RescheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.ScheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.model.InterviewType;
import ua.edu.ukma.candidai.interview.service.InterviewService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterviewController.class)
@Import(GlobalExceptionHandler.class)
class InterviewControllerTest {

    private static final String BASE_URL = "/api/v1/interviews";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewService interviewService;

    @Test
    @DisplayName("POST /api/v1/interviews - should return 201 Created with Location header and response")
    void givenValidRequest_scheduleInterview_shouldReturn201Created() throws Exception {
        UUID interviewId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.DAYS);

        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                applicationId,
                UUID.randomUUID(),
                "Sarah Recruiter",
                InterviewType.HR_SCREENING,
                scheduledAt,
                45,
                "https://meet.google.com/test",
                "Intro interview"
        );

        InterviewResponse response = new InterviewResponse(
                interviewId, applicationId, request.interviewerId(), request.interviewerName(),
                request.type(), InterviewStatus.SCHEDULED, scheduledAt, 45,
                request.meetingLink(), request.notes(), null, Instant.now(), Instant.now()
        );

        when(interviewService.scheduleInterview(any(ScheduleInterviewRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL + "/" + interviewId)))
                .andExpect(jsonPath("$.id").value(interviewId.toString()))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.type").value("HR_SCREENING"));

        verify(interviewService).scheduleInterview(any(ScheduleInterviewRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - should return 400 Bad Request when validation fails")
    void givenInvalidRequest_scheduleInterview_shouldReturn400BadRequest() throws Exception {
        // Missing applicationId and blank interviewerName
        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                null, null, "   ", InterviewType.TECHNICAL,
                Instant.now().plus(1, ChronoUnit.DAYS), 60, null, null
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id} - should return 200 Ok when interview exists")
    void givenExistingId_getInterviewById_shouldReturn200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        InterviewResponse response = createTestResponse(id, InterviewStatus.SCHEDULED);

        when(interviewService.getInterviewById(id)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(interviewService).getInterviewById(id);
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id} - should return 404 ProblemDetail when interview not found")
    void givenMissingId_getInterviewById_shouldReturn404NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(interviewService.getInterviewById(id))
                .thenThrow(new ResourceNotFoundException("Interview not found with id: " + id));

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(interviewService).getInterviewById(id);
    }

    @Test
    @DisplayName("GET /api/v1/interviews - should return 400 Bad Request when no filter provided")
    void givenNoParams_getInterviews_shouldReturn400BadRequest() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "At least one filter parameter must be provided: applicationId, interviewerId, or status"
                ));
    }

    @Test
    @DisplayName("GET /api/v1/interviews - should return 400 Bad Request when multiple filters provided")
    void givenMultipleParams_getInterviews_shouldReturn400BadRequest() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("applicationId", UUID.randomUUID().toString())
                        .param("status", "SCHEDULED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "Only one filter parameter can be specified at a time: applicationId, interviewerId, or status"
                ));
    }

    @Test
    @DisplayName("GET /api/v1/interviews?applicationId=... - should return 200 Ok with list")
    void givenApplicationIdParam_getInterviews_shouldReturnList() throws Exception {
        UUID applicationId = UUID.randomUUID();
        InterviewResponse response = createTestResponse(UUID.randomUUID(), InterviewStatus.SCHEDULED);

        when(interviewService.getInterviewsByApplicationId(applicationId)).thenReturn(List.of(response));

        mockMvc.perform(get(BASE_URL).param("applicationId", applicationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(interviewService).getInterviewsByApplicationId(applicationId);
    }

    @Test
    @DisplayName("GET /api/v1/interviews?interviewerId=... - should return 200 Ok with list")
    void givenInterviewerIdParam_getInterviews_shouldReturnList() throws Exception {
        UUID interviewerId = UUID.randomUUID();
        InterviewResponse response = createTestResponse(UUID.randomUUID(), InterviewStatus.SCHEDULED);

        when(interviewService.getInterviewsByInterviewerId(interviewerId)).thenReturn(List.of(response));

        mockMvc.perform(get(BASE_URL).param("interviewerId", interviewerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(interviewService).getInterviewsByInterviewerId(interviewerId);
    }

    @Test
    @DisplayName("GET /api/v1/interviews?status=... - should return 200 Ok with list")
    void givenStatusParam_getInterviews_shouldReturnList() throws Exception {
        InterviewResponse response = createTestResponse(UUID.randomUUID(), InterviewStatus.SCHEDULED);

        when(interviewService.getInterviewsByStatus(InterviewStatus.SCHEDULED)).thenReturn(List.of(response));

        mockMvc.perform(get(BASE_URL).param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(interviewService).getInterviewsByStatus(InterviewStatus.SCHEDULED);
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/reschedule - should return 200 Ok when valid")
    void givenValidRequest_rescheduleInterview_shouldReturn200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        Instant newTime = Instant.now().plus(4, ChronoUnit.DAYS);
        RescheduleInterviewRequest request = new RescheduleInterviewRequest(
                newTime, 90, "https://meet.google.com/new", "Reschedule reason"
        );
        InterviewResponse response = createTestResponse(id, InterviewStatus.RESCHEDULED);

        when(interviewService.rescheduleInterview(eq(id), any(RescheduleInterviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch(BASE_URL + "/" + id + "/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RESCHEDULED"));

        verify(interviewService).rescheduleInterview(eq(id), any(RescheduleInterviewRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/cancel - should return 200 Ok when valid")
    void givenValidRequest_cancelInterview_shouldReturn200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        CancelInterviewRequest request = new CancelInterviewRequest("Candidate declined");
        InterviewResponse response = createTestResponse(id, InterviewStatus.CANCELLED);

        when(interviewService.cancelInterview(eq(id), any(CancelInterviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch(BASE_URL + "/" + id + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(interviewService).cancelInterview(eq(id), any(CancelInterviewRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/complete - should return 200 Ok")
    void givenValidId_completeInterview_shouldReturn200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        InterviewResponse response = createTestResponse(id, InterviewStatus.COMPLETED);

        when(interviewService.completeInterview(id)).thenReturn(response);

        mockMvc.perform(patch(BASE_URL + "/" + id + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(interviewService).completeInterview(id);
    }

    private InterviewResponse createTestResponse(UUID id, InterviewStatus status) {
        return new InterviewResponse(
                id, UUID.randomUUID(), UUID.randomUUID(), "Test Interviewer",
                InterviewType.TECHNICAL, status, Instant.now().plus(1, ChronoUnit.DAYS),
                60, "https://meet.google.com/test", "Notes", null,
                Instant.now(), Instant.now()
        );
    }
}
