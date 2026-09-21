package ua.edu.ukma.candidai.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.repository.ScreeningResultRepository;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications/{id}/screening")
@RequiredArgsConstructor
public class AssessmentController {

    private final ScreeningResultRepository screeningResultRepository;
    private final AssessmentService assessmentService;

    @GetMapping
    public AiScreeningResult getScreeningResult(@PathVariable UUID id) {
        return screeningResultRepository.findByApplicationId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Screening result not found for application: " + id
                ));
    }

    @PostMapping
    public AiScreeningResult triggerScreening(@PathVariable UUID id) {
        return assessmentService.executeScreening(id);
    }
}
