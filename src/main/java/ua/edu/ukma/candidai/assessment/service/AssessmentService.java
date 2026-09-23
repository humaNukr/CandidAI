package ua.edu.ukma.candidai.assessment.service;

import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;

import java.util.UUID;

public interface AssessmentService {

    AiScreeningResult executeScreening(UUID applicationId);
}
