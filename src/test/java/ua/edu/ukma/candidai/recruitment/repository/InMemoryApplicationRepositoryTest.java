package ua.edu.ukma.candidai.recruitment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.model.Application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryApplicationRepositoryTest {

    private static final UUID VACANCY_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID VACANCY_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID APPLICATION_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID APPLICATION_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CANDIDATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");

    private InMemoryApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryApplicationRepository();
    }

    @Test
    @DisplayName("save and findById - should save application and find it by id")
    void givenApplication_saveAndFindById_shouldPersistAndRetrieve() {
        Application application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "test@example.com");

        Application saved = repository.save(application);

        assertThat(saved).isEqualTo(application);
        Optional<Application> found = repository.findById(APPLICATION_ID_1);
        assertThat(found).isPresent().contains(application);
    }

    @Test
    @DisplayName("findById - should return empty when application does not exist")
    void givenNonExistentId_findById_shouldReturnEmpty() {
        Optional<Application> found = repository.findById(APPLICATION_ID_2);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return true when vacancy and email match")
    void givenMatchingVacancyAndEmail_existsByVacancyIdAndEmail_shouldReturnTrue() {
        Application application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "candidate@example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_1, "candidate@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return true for case-insensitive email match")
    void givenCaseDifferentEmail_existsByVacancyIdAndEmail_shouldReturnTrue() {
        Application application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "Candidate@Example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_1, "candidate@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return false when email does not match")
    void givenDifferentEmail_existsByVacancyIdAndEmail_shouldReturnFalse() {
        Application application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "candidate@example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_1, "other@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByVacancyIdAndEmail - should return false when vacancy does not match")
    void givenDifferentVacancyId_existsByVacancyIdAndEmail_shouldReturnFalse() {
        Application application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "candidate@example.com");
        repository.save(application);

        boolean exists = repository.existsByVacancyIdAndEmail(VACANCY_ID_2, "candidate@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByVacancyId - should return list of applications matching vacancyId")
    void givenApplicationsForVacancy_findByVacancyId_shouldReturnMatchingList() {
        Application app1 = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "c1@example.com");
        Application app2 = createApplication(APPLICATION_ID_2, VACANCY_ID_1, "c2@example.com");
        repository.save(app1);
        repository.save(app2);

        List<Application> result = repository.findByVacancyId(VACANCY_ID_1);

        assertThat(result).containsExactlyInAnyOrder(app1, app2);
    }

    @Test
    @DisplayName("deleteById - should remove application by id")
    void givenExistingApplication_deleteById_shouldRemoveFromStorage() {
        Application application = createApplication(APPLICATION_ID_1, VACANCY_ID_1, "c1@example.com");
        repository.save(application);

        repository.deleteById(APPLICATION_ID_1);

        assertThat(repository.findById(APPLICATION_ID_1)).isEmpty();
        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("count - should return number of stored applications")
    void givenStoredApplications_count_shouldReturnTotal() {
        assertThat(repository.count()).isEqualTo(0);

        repository.save(createApplication(APPLICATION_ID_1, VACANCY_ID_1, "c1@example.com"));
        repository.save(createApplication(APPLICATION_ID_2, VACANCY_ID_2, "c2@example.com"));

        assertThat(repository.count()).isEqualTo(2);
    }

    private Application createApplication(UUID id, UUID vacancyId, String email) {
        return Application.builder()
                .id(id)
                .vacancyId(vacancyId)
                .candidateId(CANDIDATE_ID)
                .candidateName("Candidate Name")
                .email(email)
                .phone("+380501234567")
                .resumeUrl("https://storage.candidai.ukma.edu.ua/resumes/candidate.pdf")
                .status(ApplicationStatus.APPLIED)
                .comment(null)
                .appliedAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
