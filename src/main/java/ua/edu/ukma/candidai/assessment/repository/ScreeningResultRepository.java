package ua.edu.ukma.candidai.assessment.repository;

import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;

import java.util.Optional;
import java.util.UUID;

public interface ScreeningResultRepository {

    AiScreeningResult save(AiScreeningResult result);

    Optional<AiScreeningResult> findByApplicationId(UUID applicationId);
}
