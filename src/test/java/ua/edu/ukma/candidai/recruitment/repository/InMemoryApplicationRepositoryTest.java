package ua.edu.ukma.candidai.recruitment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryApplicationRepositoryTest {

    private static final UUID VACANCY_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID VACANCY_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID APPLICATION_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID APPLICATION_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");

    private InMemoryApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryApplicationRepository();
    }

    @Test
    @DisplayName("save and findById - should save application and find it by id")
    void shouldSaveAndFindById() {
        ApplicationResponse application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "test@example.com");

        ApplicationResponse saved = repository.save(application);

        assertThat(saved).isEqualTo(application);
        Optional<ApplicationResponse> found = repository.findById(APPLICATION_ID_1);
        assertThat(found).isPresent().contains(application);
    }

    @Test
    @DisplayName("findById - should return empty when application does not exist")
    void shouldReturnEmptyWhenNotFound() {
        Optional<ApplicationResponse> found = repository.findById(APPLICATION_ID_2);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return true when vacancy and email match")
    void shouldReturnTrueWhenVacancyIdAndEmailExist() {
        ApplicationResponse application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "candidate@example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_1, "candidate@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return true for case-insensitive email match")
    void shouldReturnTrueForCaseInsensitiveEmail() {
        ApplicationResponse application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "Candidate@Example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_1, "candidate@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return false when email does not match")
    void shouldReturnFalseWhenEmailDifferent() {
        ApplicationResponse application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "candidate@example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_1, "other@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return false when vacancy does not match")
    void shouldReturnFalseWhenVacancyIdDifferent() {
        ApplicationResponse application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "candidate@example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_2, "candidate@example.com");

        assertThat(exists).isFalse();
    }

    private ApplicationResponse createApplication(UUID id, UUID vacancyId, String email) {
        return new ApplicationResponse(
                id,
                vacancyId,
                "Candidate Name",
                email,
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/candidate.pdf",
                ApplicationStatus.APPLIED,
                null,
                NOW,
                NOW
        );
    }
}
