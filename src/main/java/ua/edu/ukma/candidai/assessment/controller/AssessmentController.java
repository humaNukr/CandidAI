package ua.edu.ukma.candidai.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;

import java.util.UUID;

@Tag(name = "Assessments", description = "AI Candidate resume screening and assessment operations")
@Slf4j
@RestController
@RequestMapping("/api/v1/applications/{id}/screening")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @Operation(summary = "Get screening result", description = "Retrieves AI evaluation report for candidate")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Screening report retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Screening result or application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public AiScreeningResult getScreeningResult(@PathVariable UUID id) {
        log.info("Received request to fetch screening report for application: {}", id);
        return assessmentService.getScreeningResult(id);
    }

    @Operation(summary = "Trigger AI screening", description = "Executes automated AI screening of candidate resume")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Screening completed successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public AiScreeningResult triggerScreening(@PathVariable UUID id) {
        log.info("Received request to manually trigger screening for application: {}", id);
        return assessmentService.executeScreening(id);
    }
}
