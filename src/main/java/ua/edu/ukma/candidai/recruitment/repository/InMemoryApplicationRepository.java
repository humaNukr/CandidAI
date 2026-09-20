package ua.edu.ukma.candidai.recruitment.repository;

import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryApplicationRepository implements ApplicationRepository {

    private final Map<UUID, ApplicationResponse> storage = new ConcurrentHashMap<>();

    @Override
    public ApplicationResponse save(ApplicationResponse application) {
        storage.put(application.id(), application);
        return application;
    }

    @Override
    public Optional<ApplicationResponse> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean existsByVacancyIdAndEmail(UUID vacancyId, String email) {
        return storage.values().stream()
                .anyMatch(app -> app.vacancyId().equals(vacancyId)
                        && app.email().equalsIgnoreCase(email));
    }
}
