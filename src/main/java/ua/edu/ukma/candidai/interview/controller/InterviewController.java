package ua.edu.ukma.candidai.interview.controller;

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
import ua.edu.ukma.candidai.interview.dto.CancelInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.InterviewResponse;
import ua.edu.ukma.candidai.interview.dto.RescheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.ScheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.service.InterviewService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

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

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterviewById(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getInterviewById(id));
    }

    @GetMapping
    public ResponseEntity<List<InterviewResponse>> getInterviews(
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) UUID interviewerId,
            @RequestParam(required = false) InterviewStatus status
    ) {
        if (applicationId != null) {
            return ResponseEntity.ok(interviewService.getInterviewsByApplicationId(applicationId));
        }
        if (interviewerId != null) {
            return ResponseEntity.ok(interviewService.getInterviewsByInterviewerId(interviewerId));
        }
        if (status != null) {
            return ResponseEntity.ok(interviewService.getInterviewsByStatus(status));
        }
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<InterviewResponse> rescheduleInterview(
            @PathVariable UUID id,
            @RequestBody @Valid RescheduleInterviewRequest request
    ) {
        return ResponseEntity.ok(interviewService.rescheduleInterview(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InterviewResponse> cancelInterview(
            @PathVariable UUID id,
            @RequestBody @Valid CancelInterviewRequest request
    ) {
        return ResponseEntity.ok(interviewService.cancelInterview(id, request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<InterviewResponse> completeInterview(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.completeInterview(id));
    }
}
