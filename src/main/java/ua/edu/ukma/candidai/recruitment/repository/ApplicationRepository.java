package ua.edu.ukma.candidai.recruitment.repository;

import ua.edu.ukma.candidai.recruitment.model.Application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository {

    Application save(Application application);

    Optional<Application> findById(UUID id);

    boolean existsByVacancyIdAndEmail(UUID vacancyId, String email);

    List<Application> findByVacancyId(UUID vacancyId);
}
