package ua.edu.ukma.candidai.recruitment.repository;

import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository {
    ApplicationResponse save(ApplicationResponse application);

    Optional<ApplicationResponse> findById(UUID id);

    boolean existsByVacancyIdAndEmail(UUID vacancyId, String email);
}
