package ua.edu.ukma.candidai.recruitment.repository;

import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.recruitment.model.Application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryApplicationRepository implements ApplicationRepository {

    private final Map<UUID, Application> storage = new ConcurrentHashMap<>();

    @Override
    public Application save(Application application) {
        storage.put(application.getId(), application);
        return application;
    }

    @Override
    public Optional<Application> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean existsByVacancyIdAndEmail(UUID vacancyId, String email) {
        return storage.values().stream()
                .anyMatch(app -> app.getVacancyId().equals(vacancyId)
                        && app.getEmail().equalsIgnoreCase(email));
    }

    @Override
    public List<Application> findByVacancyId(UUID vacancyId) {
        return storage.values().stream()
                .filter(app -> app.getVacancyId().equals(vacancyId))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }

    @Override
    public long count() {
        return storage.size();
    }
}
