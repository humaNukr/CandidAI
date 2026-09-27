package ua.edu.ukma.candidai.company.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.model.Company;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_COMPANY_ID;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_CONTACT_EMAIL;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_CREATED_AT;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_DESCRIPTION;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_LOGO_URL;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_NAME;
import static ua.edu.ukma.candidai.company.CompanyTestResources.UPDATED_CONTACT_EMAIL;
import static ua.edu.ukma.candidai.company.CompanyTestResources.UPDATED_DESCRIPTION;
import static ua.edu.ukma.candidai.company.CompanyTestResources.UPDATED_LOGO_URL;
import static ua.edu.ukma.candidai.company.CompanyTestResources.UPDATED_NAME;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompany;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompanyWithVacancies;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCreateCompanyRequest;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleUpdateCompanyRequest;

class CompanyMapperTest {

    private final CompanyMapper mapper = Mappers.getMapper(CompanyMapper.class);

    @Test
    @DisplayName("toEntity - should map CreateCompanyRequest to Company")
    void givenCreateRequest_toEntity_shouldMapAllFields() {
        CreateCompanyRequest request = sampleCreateCompanyRequest();

        Company entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(entity.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(entity.getLogoUrl()).isEqualTo(DEFAULT_LOGO_URL);
        assertThat(entity.getContactEmail()).isEqualTo(DEFAULT_CONTACT_EMAIL);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("toResponse - should map Company to CompanyResponse and count only OPEN vacancies")
    void givenCompanyWithVacancies_toResponse_shouldCalculateActiveVacanciesCountCorrectly() {
        Company company = sampleCompanyWithVacancies();

        CompanyResponse response = mapper.toResponse(company);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(DEFAULT_COMPANY_ID);
        assertThat(response.name()).isEqualTo(DEFAULT_NAME);
        assertThat(response.description()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(response.logoUrl()).isEqualTo(DEFAULT_LOGO_URL);
        assertThat(response.contactEmail()).isEqualTo(DEFAULT_CONTACT_EMAIL);
        assertThat(response.createdAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(response.activeVacanciesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("toResponse - should return 0 active vacancies when company has no vacancies")
    void givenCompanyWithoutVacancies_toResponse_shouldReturnZeroActiveVacancies() {
        Company company = sampleCompany();
        company.setVacancies(null);

        CompanyResponse response = mapper.toResponse(company);

        assertThat(response).isNotNull();
        assertThat(response.activeVacanciesCount()).isZero();
    }

    @Test
    @DisplayName("toSummaryResponse - should map Company to CompanySummaryResponse")
    void givenCompany_toSummaryResponse_shouldMapAllSummaryFields() {
        Company company = sampleCompany();

        CompanySummaryResponse response = mapper.toSummaryResponse(company);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(DEFAULT_COMPANY_ID);
        assertThat(response.name()).isEqualTo(DEFAULT_NAME);
        assertThat(response.description()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(response.logoUrl()).isEqualTo(DEFAULT_LOGO_URL);
        assertThat(response.contactEmail()).isEqualTo(DEFAULT_CONTACT_EMAIL);
        assertThat(response.createdAt()).isEqualTo(DEFAULT_CREATED_AT);
    }

    @Test
    @DisplayName("updateEntityFromRequest - should update existing Company in-place")
    void givenUpdateRequest_updateEntityFromRequest_shouldUpdateEntityInPlace() {
        Company company = sampleCompany();
        UpdateCompanyRequest updateRequest = sampleUpdateCompanyRequest();

        mapper.updateEntityFromRequest(updateRequest, company);

        assertThat(company.getName()).isEqualTo(UPDATED_NAME);
        assertThat(company.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(company.getLogoUrl()).isEqualTo(UPDATED_LOGO_URL);
        assertThat(company.getContactEmail()).isEqualTo(UPDATED_CONTACT_EMAIL);
        assertThat(company.getId()).isEqualTo(DEFAULT_COMPANY_ID);
        assertThat(company.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
    }
}
