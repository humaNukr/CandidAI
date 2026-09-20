package ua.edu.ukma.candidai.recruitment.service;

import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;

import java.util.UUID;

public interface ApplicationService {
    ApplicationResponse apply(ApplyForVacancyRequest request);

    ApplicationResponse getById(UUID id);
}
