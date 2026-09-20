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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.SubmitInterviewFeedbackRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CommonGenerator generator;
    private final Map<UUID, ApplicationResponse> applications = new ConcurrentHashMap<>();
    private final Map<UUID, List<InterviewFeedbackResponse>> feedbacks = new ConcurrentHashMap<>();

    @PatchMapping("/{id}/status")
    public ApplicationResponse updateApplicationStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateApplicationStatusRequest request
    ) {
        ApplicationResponse existing = applications.get(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Application not found with id: " + id);
        }

        Instant now = generator.now();
        String comment = request.comment() != null ? request.comment() : existing.comment();

        ApplicationResponse updated = new ApplicationResponse(
                existing.id(),
                existing.vacancyId(),
                existing.candidateName(),
                existing.email(),
                existing.phone(),
                existing.resumeUrl(),
                request.status(),
                comment,
                existing.appliedAt(),
                now
        );

        applications.put(id, updated);
        return updated;
    }

    @PostMapping("/{id}/feedbacks")
    public ResponseEntity<InterviewFeedbackResponse> submitFeedback(
            @PathVariable UUID id,
            @RequestBody @Valid SubmitInterviewFeedbackRequest request
    ) {
        if (!applications.containsKey(id)) {
            throw new ResourceNotFoundException("Application not found with id: " + id);
        }

        UUID feedbackId = generator.uuid();
        InterviewFeedbackResponse feedback = new InterviewFeedbackResponse(
                feedbackId,
                id,
                request.interviewerName(),
                request.technicalScore(),
                request.notes(),
                request.decision(),
                generator.now()
        );

        feedbacks.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(feedback);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{feedbackId}")
                .buildAndExpand(feedbackId)
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

    @GetMapping("/{id}")
    public ApplicationResponse getApplicationById(@PathVariable UUID id) {
        return applicationService.getById(id);
    }

    @GetMapping("/{id}/feedbacks")
    public List<InterviewFeedbackResponse> getFeedbacks(@PathVariable UUID id) {
        if (!applications.containsKey(id)) {
            throw new ResourceNotFoundException("Application not found with id: " + id);
        }

        return feedbacks.getOrDefault(id, List.of());
    }

    public void saveApplication(ApplicationResponse application) {
        applications.put(application.id(), application);
    }
}
