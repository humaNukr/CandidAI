package ua.edu.ukma.candidai.assessment.repository;

import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryScreeningResultRepository implements ScreeningResultRepository {

    private final Map<UUID, AiScreeningResult> storage = new ConcurrentHashMap<>();

    @Override
    public AiScreeningResult save(AiScreeningResult result) {
        storage.put(result.applicationId(), result);
        return result;
    }

    @Override
    public Optional<AiScreeningResult> findByApplicationId(UUID applicationId) {
        return Optional.ofNullable(storage.get(applicationId));
    }
}
