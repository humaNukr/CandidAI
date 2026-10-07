package ua.edu.ukma.candidai.recruitment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Applications", description = "Candidate application submission and lifecycle management")
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "Update application status", description = "Transitions application to another status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Invalid state transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/status")
    public ApplicationResponse updateApplicationStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateApplicationStatusRequest request
    ) {
        return applicationService.updateStatus(id, request);
    }

    @Operation(summary = "Submit interview feedback", description = "Submits feedback for candidate in interview")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Application not in interview stage",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/feedbacks")
    public ResponseEntity<InterviewFeedbackResponse> submitFeedback(
            @PathVariable UUID id,
            @RequestBody @Valid SubmitInterviewFeedbackRequest request
    ) {
        InterviewFeedbackResponse feedback = applicationService.submitFeedback(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{feedbackId}")
                .buildAndExpand(feedback.id())
                .toUri();

        return ResponseEntity.created(location).body(feedback);
    }

    @Operation(summary = "Apply for a vacancy", description = "Submits a candidate application for an open vacancy")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Vacancy not found or closed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Candidate already applied to this vacancy",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ApplicationResponse> applyForVacancy(
            @RequestBody @Valid ApplyForVacancyRequest request
    ) {
        ApplicationResponse application = applicationService.apply(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(application.id())
                .toUri();
        return ResponseEntity.created(location).body(application);
    }

    @Operation(summary = "Get applications", description = "Retrieves applications for a vacancy")
    @ApiResponse(responseCode = "200", description = "List of applications retrieved successfully")
    @GetMapping
    public List<ApplicationResponse> getApplications(
            @RequestParam(required = false) UUID vacancyId,
            @RequestParam(required = false, defaultValue = "false") boolean sortByScore
    ) {
        if (vacancyId != null) {
            return applicationService.getApplicationsByVacancy(vacancyId, sortByScore);
        }
        return List.of();
    }

    @Operation(summary = "Get application by ID", description = "Retrieves detailed information about an application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found"),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ApplicationResponse getApplicationById(@PathVariable UUID id) {
        return applicationService.getById(id);
    }

    @Operation(summary = "Get application feedbacks", description = "Retrieves interview feedbacks for application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedbacks retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}/feedbacks")
    public List<InterviewFeedbackResponse> getFeedbacks(@PathVariable UUID id) {
        return applicationService.getFeedbacks(id);
    }

    @Operation(summary = "Evaluate candidate", description = "Calculates aggregated candidate score from feedbacks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluation calculated successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}/evaluation")
    public EvaluationResult getEvaluation(@PathVariable UUID id) {
        return applicationService.evaluateCandidate(id);
    }
}
