package ua.edu.ukma.candidai.company.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.model.Company;
import ua.edu.ukma.candidai.company.repository.CompanyRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_COMPANY_ID;
import static ua.edu.ukma.candidai.company.CompanyTestResources.DEFAULT_NAME;
import static ua.edu.ukma.candidai.company.CompanyTestResources.NON_EXISTENT_COMPANY_ID;
import static ua.edu.ukma.candidai.company.CompanyTestResources.UPDATED_CONTACT_EMAIL;
import static ua.edu.ukma.candidai.company.CompanyTestResources.UPDATED_DESCRIPTION;
import static ua.edu.ukma.candidai.company.CompanyTestResources.UPDATED_LOGO_URL;
import static ua.edu.ukma.candidai.company.CompanyTestResources.expectedCompanyNotFoundMessage;
import static ua.edu.ukma.candidai.company.CompanyTestResources.expectedDuplicateCompanyNameMessage;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompany;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompanyResponse;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompanySummaryResponse;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCompanyWithVacancies;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleCreateCompanyRequest;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleUpdateCompanyRequest;
import static ua.edu.ukma.candidai.company.CompanyTestResources.sampleUpdatedCompanyResponse;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Test
    @DisplayName("createCompany should save company and return response when name does not exist")
    void givenValidRequest_createCompany_shouldSaveAndReturnResponse() {
        // Setup / Fixtures
        CreateCompanyRequest request = sampleCreateCompanyRequest();
        Company company = sampleCompany();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        // Stubbing / Mocking
        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(false);
        when(companyMapper.toEntity(request)).thenReturn(company);
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        // Execution / Action
        CompanyResponse response = companyService.createCompany(request);

        // Assertions / Verification
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("createCompany should throw DuplicateResourceException when name already exists")
    void givenExistingName_createCompany_shouldThrowDuplicateResourceException() {
        // Setup / Fixtures
        CreateCompanyRequest request = sampleCreateCompanyRequest();

        // Stubbing / Mocking
        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(true);

        // Execution / Action & Assertions / Verification
        assertThatThrownBy(() -> companyService.createCompany(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(expectedDuplicateCompanyNameMessage(request.name()));
    }

    @Test
    @DisplayName("getCompanyById should return company response when company exists")
    void givenExistingId_getCompanyById_shouldReturnCompanyResponse() {
        // Setup / Fixtures
        Company company = sampleCompanyWithVacancies();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        // Stubbing / Mocking
        when(companyRepository.findWithVacanciesById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        // Execution / Action
        CompanyResponse response = companyService.getCompanyById(DEFAULT_COMPANY_ID);

        // Assertions / Verification
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("getCompanyById should throw ResourceNotFoundException when company does not exist")
    void givenNonExistentId_getCompanyById_shouldThrowResourceNotFoundException() {
        // Setup / Fixtures

        // Stubbing / Mocking
        when(companyRepository.findWithVacanciesById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        // Execution / Action & Assertions / Verification
        assertThatThrownBy(() -> companyService.getCompanyById(NON_EXISTENT_COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID));
    }

    @Test
    @DisplayName("getAllCompanies should return matching companies when search keyword is provided")
    void givenSearchKeyword_getAllCompanies_shouldReturnMatchingCompanies() {
        // Setup / Fixtures
        String search = "Tech";
        Company company = sampleCompany();
        CompanySummaryResponse summaryResponse = sampleCompanySummaryResponse();

        // Stubbing / Mocking
        when(companyRepository.findByNameContainingIgnoreCase("Tech")).thenReturn(List.of(company));
        when(companyMapper.toSummaryResponse(company)).thenReturn(summaryResponse);

        // Execution / Action
        List<CompanySummaryResponse> response = companyService.getAllCompanies(search);

        // Assertions / Verification
        assertThat(response).containsExactly(summaryResponse);
    }

    @Test
    @DisplayName("getAllCompanies should return all companies when search is blank")
    void givenBlankSearch_getAllCompanies_shouldReturnAllCompanies() {
        // Setup / Fixtures
        Company company = sampleCompany();
        CompanySummaryResponse summaryResponse = sampleCompanySummaryResponse();

        // Stubbing / Mocking
        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(companyMapper.toSummaryResponse(company)).thenReturn(summaryResponse);

        // Execution / Action
        List<CompanySummaryResponse> response = companyService.getAllCompanies("   ");

        // Assertions / Verification
        assertThat(response).containsExactly(summaryResponse);
    }

    @Test
    @DisplayName("getAllCompanies should return all companies when search is null")
    void givenNullSearch_getAllCompanies_shouldReturnAllCompanies() {
        // Setup / Fixtures
        Company company = sampleCompany();
        CompanySummaryResponse summaryResponse = sampleCompanySummaryResponse();

        // Stubbing / Mocking
        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(companyMapper.toSummaryResponse(company)).thenReturn(summaryResponse);

        // Execution / Action
        List<CompanySummaryResponse> response = companyService.getAllCompanies(null);

        // Assertions / Verification
        assertThat(response).containsExactly(summaryResponse);
    }

    @Test
    @DisplayName("updateCompany should update company and return response when name is changed and unique")
    void givenExistingIdAndDifferentUniqueName_updateCompany_shouldUpdateAndReturnResponse() {
        // Setup / Fixtures
        Company company = sampleCompany();
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();
        CompanyResponse expectedResponse = sampleUpdatedCompanyResponse();

        // Stubbing / Mocking
        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(false);
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        // Execution / Action
        CompanyResponse response = companyService.updateCompany(DEFAULT_COMPANY_ID, request);

        // Assertions / Verification
        assertThat(response).isEqualTo(expectedResponse);
        verify(companyMapper).updateEntityFromRequest(request, company);
    }

    @Test
    @DisplayName("updateCompany should update company without checking duplicate name when name is unchanged")
    void givenSameName_updateCompany_shouldNotCheckDuplicateAndReturnResponse() {
        // Setup / Fixtures
        Company company = sampleCompany();
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                DEFAULT_NAME,
                UPDATED_DESCRIPTION,
                UPDATED_LOGO_URL,
                UPDATED_CONTACT_EMAIL
        );
        CompanyResponse expectedResponse = sampleUpdatedCompanyResponse();

        // Stubbing / Mocking
        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        // Execution / Action
        CompanyResponse response = companyService.updateCompany(DEFAULT_COMPANY_ID, request);

        // Assertions / Verification
        assertThat(response).isEqualTo(expectedResponse);
        verify(companyMapper).updateEntityFromRequest(request, company);
    }

    @Test
    @DisplayName("updateCompany should throw DuplicateResourceException when new name already exists")
    void givenExistingIdAndDuplicateName_updateCompany_shouldThrowDuplicateResourceException() {
        // Setup / Fixtures
        Company company = sampleCompany();
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();

        // Stubbing / Mocking
        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(true);

        // Execution / Action & Assertions / Verification
        assertThatThrownBy(() -> companyService.updateCompany(DEFAULT_COMPANY_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(expectedDuplicateCompanyNameMessage(request.name()));
    }

    @Test
    @DisplayName("updateCompany should throw ResourceNotFoundException when company does not exist")
    void givenNonExistentId_updateCompany_shouldThrowResourceNotFoundException() {
        // Setup / Fixtures
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();

        // Stubbing / Mocking
        when(companyRepository.findById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        // Execution / Action & Assertions / Verification
        assertThatThrownBy(() -> companyService.updateCompany(NON_EXISTENT_COMPANY_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID));
    }

    @Test
    @DisplayName("deleteCompany should delete company when company exists")
    void givenExistingId_deleteCompany_shouldDeleteCompany() {
        // Setup / Fixtures
        Company company = sampleCompany();

        // Stubbing / Mocking
        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));

        // Execution / Action
        companyService.deleteCompany(DEFAULT_COMPANY_ID);

        // Assertions / Verification
        verify(companyRepository).delete(company);
    }

    @Test
    @DisplayName("deleteCompany should throw ResourceNotFoundException when company does not exist")
    void givenNonExistentId_deleteCompany_shouldThrowResourceNotFoundException() {
        // Setup / Fixtures

        // Stubbing / Mocking
        when(companyRepository.findById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        // Execution / Action & Assertions / Verification
        assertThatThrownBy(() -> companyService.deleteCompany(NON_EXISTENT_COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID));
    }
}
