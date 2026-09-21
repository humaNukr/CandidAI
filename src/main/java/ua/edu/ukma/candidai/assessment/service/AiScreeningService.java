package ua.edu.ukma.candidai.assessment.service;

import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.util.UUID;

public interface AiScreeningService {

    AiScreeningResult screenCandidate(UUID applicationId, String resumeText, VacancyDetails vacancy);
}
