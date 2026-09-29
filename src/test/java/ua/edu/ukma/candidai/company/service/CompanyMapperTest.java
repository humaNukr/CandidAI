package ua.edu.ukma.candidai.company.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.model.Company;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.company.CompanyTestResources.*;

class CompanyMapperTest {

    private final CompanyMapper mapper = Mappers.getMapper(CompanyMapper.class);

    @Test
    @DisplayName("toEntity - should map CreateCompanyRequest to Company")
    void givenCreateRequest_toEntity_shouldMapAllFields() {
        CreateCompanyRequest request = sampleCreateCompanyRequest();

        Company entity = mapper.toEntity(request);

        assertThat(entity).usingRecursiveComparison().isEqualTo(expectedCreatedCompany());
    }

    @Test
    @DisplayName("toResponse - should map Company and activeVacanciesCount to CompanyResponse")
    void givenCompanyAndCount_toResponse_shouldMapAllFieldsAndSetCount() {
        Company company = sampleCompany();
        int count = 3;

        CompanyResponse response = mapper.toResponse(company, count);

        assertThat(response).usingRecursiveComparison().isEqualTo(sampleCompanyResponseWithActiveVacancies(count));
    }

    @Test
    @DisplayName("toResponse - should default activeVacanciesCount to 0 when count not provided")
    void givenCompany_toResponse_shouldDefaultActiveVacanciesCountToZero() {
        Company company = sampleCompany();

        CompanyResponse response = mapper.toResponse(company);

        assertThat(response).usingRecursiveComparison().isEqualTo(sampleCompanyResponse());
    }

    @Test
    @DisplayName("toSummaryResponse - should map Company to CompanySummaryResponse")
    void givenCompany_toSummaryResponse_shouldMapAllSummaryFields() {
        Company company = sampleCompany();

        CompanySummaryResponse response = mapper.toSummaryResponse(company);

        assertThat(response).usingRecursiveComparison().isEqualTo(sampleCompanySummaryResponse());
    }

    @Test
    @DisplayName("updateEntityFromRequest - should update existing Company in-place")
    void givenUpdateRequest_updateEntityFromRequest_shouldUpdateEntityInPlace() {
        Company company = sampleCompany();
        UpdateCompanyRequest updateRequest = sampleUpdateCompanyRequest();

        mapper.updateEntityFromRequest(updateRequest, company);

        assertThat(company).usingRecursiveComparison().isEqualTo(sampleUpdatedCompany());
    }
}
