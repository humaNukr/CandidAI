package ua.edu.ukma.candidai.vacancy.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ua.edu.ukma.candidai.company.model.Company;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Skill;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

@DataJpaTest
class VacancyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VacancyRepository vacancyRepository;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO users (id, full_name, email, role, created_at) VALUES (?, ?, ?, ?, ?)")
                .setParameter(1, DEFAULT_AUTHOR_ID)
                .setParameter(2, "Test Author")
                .setParameter(3, "author@test.com")
                .setParameter(4, "RECRUITER")
                .setParameter(5, DEFAULT_NOW)
                .executeUpdate();
    }

    @Test
    @DisplayName("findWithSkillsById - should fetch vacancy with skills using EntityGraph")
    void givenPersistedVacancyWithSkills_findWithSkillsById_shouldFetchSkillsEagerly() {
        Company company = persistCompany();
        Skill skill = aSkillJava();
        entityManager.persist(skill);
        Vacancy vacancy = aVacancy();
        vacancy.setCompanyId(company.getId());
        vacancy.setSkills(List.of(skill));
        entityManager.persistAndFlush(vacancy);
        entityManager.clear();

        Optional<Vacancy> actual = vacancyRepository.findWithSkillsById(vacancy.getId());

        assertThat(actual).isPresent();
        assertThat(actual.get().getSkills())
                .usingRecursiveComparison()
                .isEqualTo(List.of(skill));
    }

    @Test
    @DisplayName("findAllWithSkillsBy - should fetch all vacancies with skills using EntityGraph")
    void givenPersistedVacanciesWithSkills_findAllWithSkillsBy_shouldFetchSkillsEagerly() {
        Company company = persistCompany();
        Skill skill = aSkillJava();
        entityManager.persist(skill);
        Vacancy vacancy = aVacancy();
        vacancy.setCompanyId(company.getId());
        vacancy.setSkills(List.of(skill));
        entityManager.persistAndFlush(vacancy);
        entityManager.clear();

        List<Vacancy> actual = vacancyRepository.findAllWithSkillsBy();

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst().getSkills())
                .usingRecursiveComparison()
                .isEqualTo(List.of(skill));
    }

    @Test
    @DisplayName("findByStatus - should return vacancies matching status")
    void givenVacancies_findByStatus_shouldReturnMatchingStatusOnly() {
        Company company = persistCompany();
        Vacancy vacancy = aVacancy();
        vacancy.setCompanyId(company.getId());
        entityManager.persistAndFlush(vacancy);
        entityManager.clear();

        List<Vacancy> actual = vacancyRepository.findByStatus(VacancyStatus.OPEN);

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst().getId()).isEqualTo(vacancy.getId());
    }

    @Test
    @DisplayName("findByCompanyId - should return vacancies matching companyId")
    void givenVacancies_findByCompanyId_shouldReturnMatchingCompanyVacancies() {
        Company company = persistCompany();
        Vacancy vacancy = aVacancy();
        vacancy.setCompanyId(company.getId());
        entityManager.persistAndFlush(vacancy);
        entityManager.clear();

        List<Vacancy> actual = vacancyRepository.findByCompanyId(company.getId());

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst().getId()).isEqualTo(vacancy.getId());
    }

    @Test
    @DisplayName("findByStatusAndCompanyId - should return matching vacancies")
    void givenVacancies_findByStatusAndCompanyId_shouldReturnMatching() {
        Company company = persistCompany();
        Vacancy vacancy = aVacancy();
        vacancy.setCompanyId(company.getId());
        entityManager.persistAndFlush(vacancy);
        entityManager.clear();

        List<Vacancy> actual = vacancyRepository.findByStatusAndCompanyId(VacancyStatus.OPEN, company.getId());

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst().getId()).isEqualTo(vacancy.getId());
    }

    @Test
    @DisplayName("existsByAuthorIdAndTitleIgnoreCaseAndStatusAndDeletedFalse - should detect existing active vacancy")
    void givenActiveVacancy_existsActive_shouldReturnTrue() {
        Company company = persistCompany();
        Vacancy vacancy = aVacancy();
        vacancy.setCompanyId(company.getId());
        entityManager.persistAndFlush(vacancy);

        boolean exists = vacancyRepository.existsByAuthorIdAndTitleIgnoreCaseAndStatusAndDeletedFalse(
                vacancy.getAuthorId(),
                "senior java engineer",
                VacancyStatus.OPEN
        );

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsActiveByAuthorIdAndTitle - should return true for active vacancy")
    void givenActiveVacancy_existsActiveByAuthorIdAndTitle_shouldReturnTrue() {
        Company company = persistCompany();
        Vacancy vacancy = aVacancy();
        vacancy.setCompanyId(company.getId());
        entityManager.persistAndFlush(vacancy);

        boolean exists = vacancyRepository.existsActiveByAuthorIdAndTitle(
                vacancy.getAuthorId(),
                "senior java engineer"
        );

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("findAllActive - should filter and paginate active vacancies with skills")
    void givenFilteredQuery_findAllActive_shouldReturnPagedResults() {
        Company company = persistCompany();
        Vacancy v1 = aVacancy();
        v1.setCompanyId(company.getId());
        entityManager.persistAndFlush(v1);

        Page<Vacancy> page = vacancyRepository.findAllActive(
                VacancyStatus.OPEN,
                JobCategory.ENGINEERING,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(v1.getId());
    }

    @Test
    @DisplayName("findAll - should delegate to findAllActive and return paged results")
    void givenFilteredQuery_findAll_shouldDelegateToFindAllActive() {
        Company company = persistCompany();
        Vacancy v1 = aVacancy();
        v1.setCompanyId(company.getId());
        entityManager.persistAndFlush(v1);

        Page<Vacancy> page = vacancyRepository.findAll(
                VacancyStatus.OPEN,
                JobCategory.ENGINEERING,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(v1.getId());
    }

    private Company persistCompany() {
        Company company = Company.builder()
                .id(UUID.randomUUID())
                .name("Company " + UUID.randomUUID())
                .createdAt(DEFAULT_NOW)
                .build();
        return entityManager.persistAndFlush(company);
    }
}
