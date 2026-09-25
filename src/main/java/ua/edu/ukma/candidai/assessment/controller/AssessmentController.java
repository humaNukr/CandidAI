package ua.edu.ukma.candidai.assessment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/applications/{id}/screening")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    public AiScreeningResult getScreeningResult(@PathVariable UUID id) {
        log.info("Received request to fetch screening report for application: {}", id);
        return assessmentService.getScreeningResult(id);
    }

    @PostMapping
    public AiScreeningResult triggerScreening(@PathVariable UUID id) {
        log.info("Received request to manually trigger screening for application: {}", id);
        return assessmentService.executeScreening(id);
    }
}
