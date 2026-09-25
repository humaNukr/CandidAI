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

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PatchMapping("/{id}/status")
    public ApplicationResponse updateApplicationStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateApplicationStatusRequest request
    ) {
        return applicationService.updateStatus(id, request);
    }

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

    @GetMapping("/{id}")
    public ApplicationResponse getApplicationById(@PathVariable UUID id) {
        return applicationService.getById(id);
    }

    @GetMapping("/{id}/feedbacks")
    public List<InterviewFeedbackResponse> getFeedbacks(@PathVariable UUID id) {
        return applicationService.getFeedbacks(id);
    }

    @GetMapping("/{id}/evaluation")
    public EvaluationResult getEvaluation(@PathVariable UUID id) {
        return applicationService.evaluateCandidate(id);
    }
}
