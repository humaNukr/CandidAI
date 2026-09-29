package ua.edu.ukma.candidai.company.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.company.CompanyTestResources.*;

class CompanyTest {

    @Test
    @DisplayName("builder - should construct Company with provided fields")
    void givenFields_builder_shouldConstructCompany() {
        Company company = Company.builder()
                .id(DEFAULT_COMPANY_ID)
                .name(DEFAULT_NAME)
                .description(DEFAULT_DESCRIPTION)
                .logoUrl(DEFAULT_LOGO_URL)
                .contactEmail(DEFAULT_CONTACT_EMAIL)
                .createdAt(DEFAULT_CREATED_AT)
                .build();

        assertThat(company).usingRecursiveComparison().isEqualTo(sampleCompany());
    }

    @Test
    @DisplayName("setters - should update mutable fields")
    void givenCompany_setters_shouldUpdateFields() {
        Company company = sampleCompany();

        company.setName(UPDATED_NAME);
        company.setDescription(UPDATED_DESCRIPTION);
        company.setLogoUrl(UPDATED_LOGO_URL);
        company.setContactEmail(UPDATED_CONTACT_EMAIL);

        assertThat(company).usingRecursiveComparison().isEqualTo(sampleUpdatedCompany());
    }
}
