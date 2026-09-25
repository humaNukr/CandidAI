package ua.edu.ukma.candidai.vacancy.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

class InMemoryVacancyRepositoryTest {

    private InMemoryVacancyRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryVacancyRepository();
    }

    @Test
    @DisplayName("save should store and return vacancy")
    void givenVacancy_save_shouldStoreAndReturnVacancy() {
        Vacancy vacancy = aVacancy();

        Vacancy saved = repository.save(vacancy);

        assertThat(saved).isEqualTo(vacancy);
        assertThat(repository.findById(DEFAULT_ID)).contains(vacancy);
    }

    @Test
    @DisplayName("findById with non-existent id should return empty optional")
    void givenNonExistentId_findById_shouldReturnEmpty() {
        Optional<Vacancy> found = repository.findById(NON_EXISTENT_ID);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findById with deleted vacancy should return deleted vacancy")
    void givenDeletedVacancy_findById_shouldReturnVacancy() {
        Vacancy deletedVacancy = aDeletedVacancy();
        repository.save(deletedVacancy);

        Optional<Vacancy> found = repository.findById(DEFAULT_ID);

        assertThat(found).contains(deletedVacancy);
    }

    @Test
    @DisplayName("findAll with no filters should return all non-deleted vacancies sorted by createdAt desc")
    void givenNonDeletedVacancies_findAll_shouldReturnNonDeletedSortedByCreatedAtDesc() {
        Vacancy v1 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000011"))
                .createdAt(Instant.parse("2026-09-01T10:00:00Z"))
                .build();
        Vacancy v2 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000012"))
                .createdAt(Instant.parse("2026-09-03T10:00:00Z"))
                .build();
        Vacancy v3 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000013"))
                .createdAt(Instant.parse("2026-09-02T10:00:00Z"))
                .build();
        Vacancy deleted = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000014"))
                .deleted(true)
                .build();

        repository.save(v1);
        repository.save(v2);
        repository.save(v3);
        repository.save(deleted);

        Page<Vacancy> page = repository.findAll(null, null, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly(v2, v3, v1);
    }

    @Test
    @DisplayName("findAll with status filter should filter by status")
    void givenStatusFilter_findAll_shouldFilterByStatus() {
        Vacancy open = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000021"))
                .status(VacancyStatus.OPEN)
                .build();
        Vacancy closed = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000022"))
                .status(VacancyStatus.CLOSED)
                .build();

        repository.save(open);
        repository.save(closed);

        Page<Vacancy> page = repository.findAll(VacancyStatus.OPEN, null, Pageable.unpaged());

        assertThat(page.getContent()).containsExactly(open);
    }

    @Test
    @DisplayName("findAll with category filter should filter by category")
    void givenCategoryFilter_findAll_shouldFilterByCategory() {
        Vacancy eng = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000031"))
                .category(JobCategory.ENGINEERING)
                .build();
        Vacancy mkt = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000032"))
                .category(JobCategory.MARKETING)
                .build();

        repository.save(eng);
        repository.save(mkt);

        Page<Vacancy> page = repository.findAll(null, JobCategory.MARKETING, Pageable.unpaged());

        assertThat(page.getContent()).containsExactly(mkt);
    }

    @Test
    @DisplayName("findAll with ascending sort should sort by createdAt asc")
    void givenAscendingSort_findAll_shouldSortAscending() {
        Vacancy v1 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000041"))
                .createdAt(Instant.parse("2026-09-01T10:00:00Z"))
                .build();
        Vacancy v2 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000042"))
                .createdAt(Instant.parse("2026-09-02T10:00:00Z"))
                .build();

        repository.save(v1);
        repository.save(v2);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Vacancy> page = repository.findAll(null, null, pageable);

        assertThat(page.getContent()).containsExactly(v1, v2);
    }

    @Test
    @DisplayName("findAll with pagination should paginate correctly")
    void givenPagination_findAll_shouldReturnPagedContent() {
        Vacancy v1 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000051"))
                .createdAt(Instant.parse("2026-09-03T10:00:00Z"))
                .build();
        Vacancy v2 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000052"))
                .createdAt(Instant.parse("2026-09-02T10:00:00Z"))
                .build();
        Vacancy v3 = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000053"))
                .createdAt(Instant.parse("2026-09-01T10:00:00Z"))
                .build();

        repository.save(v1);
        repository.save(v2);
        repository.save(v3);

        Pageable pageable = PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Vacancy> page = repository.findAll(null, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly(v2);
    }

    @Test
    @DisplayName("existsActiveByAuthorIdAndTitle with matching open vacancy should return true")
    void givenMatchingOpenVacancy_existsActiveByAuthorIdAndTitle_shouldReturnTrue() {
        Vacancy vacancy = aVacancyBuilder()
                .authorId(DEFAULT_AUTHOR_ID)
                .title("Software Engineer")
                .status(VacancyStatus.OPEN)
                .build();
        repository.save(vacancy);

        boolean exists = repository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, "Software Engineer");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsActiveByAuthorIdAndTitle with different case and whitespace should return true")
    void givenDifferentCaseAndWhitespace_existsActiveByAuthorIdAndTitle_shouldReturnTrue() {
        Vacancy vacancy = aVacancyBuilder()
                .authorId(DEFAULT_AUTHOR_ID)
                .title("Software Engineer")
                .status(VacancyStatus.OPEN)
                .build();
        repository.save(vacancy);

        boolean exists = repository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, "  software ENGINEER  ");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsActiveByAuthorIdAndTitle with non-open status should return false")
    void givenNonOpenStatus_existsActiveByAuthorIdAndTitle_shouldReturnFalse() {
        Vacancy closed = aVacancyBuilder()
                .authorId(DEFAULT_AUTHOR_ID)
                .title("Software Engineer")
                .status(VacancyStatus.CLOSED)
                .build();
        Vacancy draft = aVacancyBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000062"))
                .authorId(DEFAULT_AUTHOR_ID)
                .title("Data Engineer")
                .status(VacancyStatus.DRAFT)
                .build();

        repository.save(closed);
        repository.save(draft);

        assertThat(repository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, "Software Engineer")).isFalse();
        assertThat(repository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, "Data Engineer")).isFalse();
    }

    @Test
    @DisplayName("existsActiveByAuthorIdAndTitle with deleted vacancy should return false")
    void givenDeletedVacancy_existsActiveByAuthorIdAndTitle_shouldReturnFalse() {
        Vacancy deleted = aVacancyBuilder()
                .authorId(DEFAULT_AUTHOR_ID)
                .title("Software Engineer")
                .status(VacancyStatus.OPEN)
                .deleted(true)
                .build();
        repository.save(deleted);

        assertThat(repository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, "Software Engineer")).isFalse();
    }

    @Test
    @DisplayName("existsActiveByAuthorIdAndTitle with different author should return false")
    void givenDifferentAuthor_existsActiveByAuthorIdAndTitle_shouldReturnFalse() {
        Vacancy vacancy = aVacancyBuilder()
                .authorId(DEFAULT_AUTHOR_ID)
                .title("Software Engineer")
                .status(VacancyStatus.OPEN)
                .build();
        repository.save(vacancy);

        UUID anotherAuthorId = UUID.fromString("00000000-0000-0000-0000-000000000077");
        assertThat(repository.existsActiveByAuthorIdAndTitle(anotherAuthorId, "Software Engineer")).isFalse();
    }

    @Test
    @DisplayName("existsActiveByAuthorIdAndTitle with different title should return false")
    void givenDifferentTitle_existsActiveByAuthorIdAndTitle_shouldReturnFalse() {
        Vacancy vacancy = aVacancyBuilder()
                .authorId(DEFAULT_AUTHOR_ID)
                .title("Software Engineer")
                .status(VacancyStatus.OPEN)
                .build();
        repository.save(vacancy);

        assertThat(repository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, "DevOps Engineer")).isFalse();
    }
}
