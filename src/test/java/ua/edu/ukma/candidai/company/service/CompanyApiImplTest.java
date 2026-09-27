package ua.edu.ukma.candidai.company.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.model.Company;
import ua.edu.ukma.candidai.company.repository.CompanyRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_COMPANY_ID;
import static ua.edu.ukma.candidai.company.CompanyTestResources.NON_EXISTENT_COMPANY_ID;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompany;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompanyResponse;

@ExtendWith(MockitoExtension.class)
class CompanyApiImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private CompanyApiImpl companyApi;

    @Test
    @DisplayName("companyExists should return true when company exists")
    void givenExistingId_companyExists_shouldReturnTrue() {
        // Setup / Fixtures

        // Stubbing / Mocking
        when(companyRepository.existsById(DEFAULT_COMPANY_ID)).thenReturn(true);

        // Execution / Action
        boolean exists = companyApi.companyExists(DEFAULT_COMPANY_ID);

        // Assertions / Verification
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("companyExists should return false when company does not exist")
    void givenNonExistentId_companyExists_shouldReturnFalse() {
        // Setup / Fixtures

        // Stubbing / Mocking
        when(companyRepository.existsById(NON_EXISTENT_COMPANY_ID)).thenReturn(false);

        // Execution / Action
        boolean exists = companyApi.companyExists(NON_EXISTENT_COMPANY_ID);

        // Assertions / Verification
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findCompanyById should return company response optional when company exists")
    void givenExistingId_findCompanyById_shouldReturnCompanyResponseOptional() {
        // Setup / Fixtures
        Company company = sampleCompany();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        // Stubbing / Mocking
        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        // Execution / Action
        Optional<CompanyResponse> response = companyApi.findCompanyById(DEFAULT_COMPANY_ID);

        // Assertions / Verification
        assertThat(response).contains(expectedResponse);
    }

    @Test
    @DisplayName("findCompanyById should return empty optional when company does not exist")
    void givenNonExistentId_findCompanyById_shouldReturnEmptyOptional() {
        // Setup / Fixtures

        // Stubbing / Mocking
        when(companyRepository.findById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        // Execution / Action
        Optional<CompanyResponse> response = companyApi.findCompanyById(NON_EXISTENT_COMPANY_ID);

        // Assertions / Verification
        assertThat(response).isEmpty();
    }
}
