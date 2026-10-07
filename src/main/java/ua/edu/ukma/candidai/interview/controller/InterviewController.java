package ua.edu.ukma.candidai.interview.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
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
import ua.edu.ukma.candidai.interview.dto.CancelInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.InterviewResponse;
import ua.edu.ukma.candidai.interview.dto.RescheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.ScheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.service.InterviewService;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Tag(name = "Interviews", description = "Interview scheduling and candidate feedback coordination")
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "Schedule interview", description = "Schedules a candidate interview session")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Interview scheduled successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @RequestBody @Valid ScheduleInterviewRequest request
    ) {
        InterviewResponse response = interviewService.scheduleInterview(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get interview by ID", description = "Retrieves interview session details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview found"),
            @ApiResponse(responseCode = "404", description = "Interview not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterviewById(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getInterviewById(id));
    }

    @Operation(summary = "Get interviews", description = "Retrieves interviews with filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of interviews retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<InterviewResponse>> getInterviews(
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) UUID interviewerId,
            @RequestParam(required = false) InterviewStatus status
    ) {
        long filterCount = Stream.of(applicationId, interviewerId, status)
                .filter(Objects::nonNull)
                .count();

        if (filterCount == 0) {
            throw new IllegalArgumentException(
                    "At least one filter parameter must be provided: applicationId, interviewerId, or status"
            );
        }
        if (filterCount > 1) {
            throw new IllegalArgumentException(
                    "Only one filter parameter can be specified at a time: applicationId, interviewerId, or status"
            );
        }

        if (applicationId != null) {
            return ResponseEntity.ok(interviewService.getInterviewsByApplicationId(applicationId));
        }
        if (interviewerId != null) {
            return ResponseEntity.ok(interviewService.getInterviewsByInterviewerId(interviewerId));
        }
        return ResponseEntity.ok(interviewService.getInterviewsByStatus(status));
    }

    @Operation(summary = "Reschedule interview", description = "Updates scheduled date/time and link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview rescheduled successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Interview not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Invalid interview state transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<InterviewResponse> rescheduleInterview(
            @PathVariable UUID id,
            @RequestBody @Valid RescheduleInterviewRequest request
    ) {
        return ResponseEntity.ok(interviewService.rescheduleInterview(id, request));
    }

    @Operation(summary = "Cancel interview", description = "Cancels a scheduled interview session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Interview not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Invalid interview state transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InterviewResponse> cancelInterview(
            @PathVariable UUID id,
            @RequestBody @Valid CancelInterviewRequest request
    ) {
        return ResponseEntity.ok(interviewService.cancelInterview(id, request));
    }

    @Operation(summary = "Complete interview", description = "Marks interview session as completed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview marked as completed"),
            @ApiResponse(responseCode = "404", description = "Interview not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Invalid interview state transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/complete")
    public ResponseEntity<InterviewResponse> completeInterview(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.completeInterview(id));
    }
}
