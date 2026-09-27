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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.company.CompanyTestResources.*;

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
        when(companyRepository.existsById(DEFAULT_COMPANY_ID)).thenReturn(true);

        boolean exists = companyApi.companyExists(DEFAULT_COMPANY_ID);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("companyExists should return false when company does not exist")
    void givenNonExistentId_companyExists_shouldReturnFalse() {
        when(companyRepository.existsById(NON_EXISTENT_COMPANY_ID)).thenReturn(false);

        boolean exists = companyApi.companyExists(NON_EXISTENT_COMPANY_ID);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findCompanyById should return company response optional when company exists")
    void givenExistingId_findCompanyById_shouldReturnCompanyResponseOptional() {
        Company company = sampleCompany();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        Optional<CompanyResponse> response = companyApi.findCompanyById(DEFAULT_COMPANY_ID);

        assertThat(response).contains(expectedResponse);
    }

    @Test
    @DisplayName("findCompanyById should return empty optional when company does not exist")
    void givenNonExistentId_findCompanyById_shouldReturnEmptyOptional() {
        when(companyRepository.findById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        Optional<CompanyResponse> response = companyApi.findCompanyById(NON_EXISTENT_COMPANY_ID);

        assertThat(response).isEmpty();
    }
}
