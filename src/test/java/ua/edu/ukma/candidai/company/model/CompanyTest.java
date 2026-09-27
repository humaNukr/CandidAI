package ua.edu.ukma.candidai.company.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.company.CompanyTestResources.*;

class CompanyTest {

    @Test
    @DisplayName("addVacancy - should maintain relationship with vacancy")
    void givenCompanyAndVacancy_addVacancy_shouldMaintainRelation() {
        // Setup / Fixtures
        Company company = sampleCompany();
        Vacancy vacancy = sampleVacancy();

        // Stubbing / Mocking

        // Execution / Action
        company.addVacancy(vacancy);

        // Assertions / Verification
        assertThat(company.getVacancies()).contains(vacancy);
        assertThat(vacancy.getCompanyId()).isEqualTo(DEFAULT_COMPANY_ID);
    }

    @Test
    @DisplayName("addVacancy - should handle null vacancy gracefully")
    void givenNullVacancy_addVacancy_shouldNotFailAndKeepVacanciesUnchanged() {
        // Setup / Fixtures
        Company company = sampleCompany();

        // Stubbing / Mocking

        // Execution / Action
        company.addVacancy(null);

        // Assertions / Verification
        assertThat(company.getVacancies()).isEmpty();
    }

    @Test
    @DisplayName("removeVacancy - should maintain relationship when removing vacancy")
    void givenCompanyAndVacancy_removeVacancy_shouldMaintainRelation() {
        // Setup / Fixtures
        Company company = sampleCompany();
        Vacancy vacancy = sampleVacancy();
        company.addVacancy(vacancy);

        // Stubbing / Mocking

        // Execution / Action
        company.removeVacancy(vacancy);

        // Assertions / Verification
        assertThat(company.getVacancies()).doesNotContain(vacancy);
        assertThat(vacancy.getCompanyId()).isNull();
    }

    @Test
    @DisplayName("removeVacancy - should handle null vacancy gracefully")
    void givenNullVacancy_removeVacancy_shouldNotFail() {
        // Setup / Fixtures
        Company company = sampleCompany();

        // Stubbing / Mocking

        // Execution / Action
        company.removeVacancy(null);

        // Assertions / Verification
        assertThat(company.getVacancies()).isEmpty();
    }

    @Test
    @DisplayName("builder - should initialize vacancies with empty list by default")
    void givenNoExplicitVacancies_build_shouldInitializeEmptyVacanciesList() {
        // Setup / Fixtures
        UUID companyId = UUID.randomUUID();

        // Stubbing / Mocking

        // Execution / Action
        Company company = Company.builder()
                .id(companyId)
                .name("DefaultCo")
                .build();

        // Assertions / Verification
        assertThat(company.getVacancies()).isNotNull().isEmpty();
    }
}
