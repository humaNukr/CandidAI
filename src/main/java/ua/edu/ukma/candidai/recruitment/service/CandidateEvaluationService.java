package ua.edu.ukma.candidai.recruitment.service;

import ua.edu.ukma.candidai.recruitment.service.strategy.EvaluationResult;

import java.util.UUID;

public interface CandidateEvaluationService {

    EvaluationResult evaluateCandidate(UUID id);
}
