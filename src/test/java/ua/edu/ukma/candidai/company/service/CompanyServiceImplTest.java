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

import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.VacancyApi;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.company.CompanyTestResources.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private CommonGenerator commonGenerator;

    @Mock
    private VacancyApi vacancyApi;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Test
    @DisplayName("createCompany should save company and return response when name does not exist")
    void givenValidRequest_createCompany_shouldSaveAndReturnResponse() {
        CreateCompanyRequest request = sampleCreateCompanyRequest();
        Company company = sampleCompany();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(false);
        when(companyMapper.toEntity(request)).thenReturn(company);
        when(commonGenerator.uuid()).thenReturn(DEFAULT_COMPANY_ID);
        when(commonGenerator.now()).thenReturn(DEFAULT_CREATED_AT);
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company, 0)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.createCompany(request);

        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("createCompany should throw DuplicateResourceException when name already exists")
    void givenExistingName_createCompany_shouldThrowDuplicateResourceException() {
        CreateCompanyRequest request = sampleCreateCompanyRequest();

        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(expectedDuplicateCompanyNameMessage(request.name()));
    }

    @Test
    @DisplayName("getCompanyById should return company response when company exists")
    void givenExistingId_getCompanyById_shouldReturnCompanyResponse() {
        Company company = sampleCompany();
        CompanyResponse expectedResponse = sampleCompanyResponse();

        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(vacancyApi.countActiveVacanciesByCompanyId(DEFAULT_COMPANY_ID)).thenReturn(0);
        when(companyMapper.toResponse(company, 0)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.getCompanyById(DEFAULT_COMPANY_ID);

        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("getCompanyById should throw ResourceNotFoundException when company does not exist")
    void givenNonExistentId_getCompanyById_shouldThrowResourceNotFoundException() {
        when(companyRepository.findById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyById(NON_EXISTENT_COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID));
    }

    @Test
    @DisplayName("getCompanies should return matching companies when name keyword is provided")
    void givenNameKeyword_getCompanies_shouldReturnMatchingCompanies() {
        String name = "Tech";
        Company company = sampleCompany();
        CompanySummaryResponse summaryResponse = sampleCompanySummaryResponse();

        when(companyRepository.findByNameContainingIgnoreCase("Tech")).thenReturn(List.of(company));
        when(companyMapper.toSummaryResponse(company)).thenReturn(summaryResponse);

        List<CompanySummaryResponse> response = companyService.getCompanies(name);

        assertThat(response).usingRecursiveComparison().isEqualTo(List.of(summaryResponse));
    }

    @Test
    @DisplayName("getCompanies should return all companies when name is blank")
    void givenBlankName_getCompanies_shouldReturnAllCompanies() {
        Company company = sampleCompany();
        CompanySummaryResponse summaryResponse = sampleCompanySummaryResponse();

        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(companyMapper.toSummaryResponse(company)).thenReturn(summaryResponse);

        List<CompanySummaryResponse> response = companyService.getCompanies("   ");

        assertThat(response).usingRecursiveComparison().isEqualTo(List.of(summaryResponse));
    }

    @Test
    @DisplayName("getCompanies should return all companies when name is null")
    void givenNullName_getCompanies_shouldReturnAllCompanies() {
        Company company = sampleCompany();
        CompanySummaryResponse summaryResponse = sampleCompanySummaryResponse();

        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(companyMapper.toSummaryResponse(company)).thenReturn(summaryResponse);

        List<CompanySummaryResponse> response = companyService.getCompanies(null);

        assertThat(response).usingRecursiveComparison().isEqualTo(List.of(summaryResponse));
    }

    @Test
    @DisplayName("updateCompany should update company and return response when name is changed and unique")
    void givenExistingIdAndDifferentUniqueName_updateCompany_shouldUpdateAndReturnResponse() {
        Company company = sampleCompany();
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();
        CompanyResponse expectedResponse = sampleUpdatedCompanyResponse();

        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(false);
        when(companyRepository.save(company)).thenReturn(company);
        when(vacancyApi.countActiveVacanciesByCompanyId(DEFAULT_COMPANY_ID)).thenReturn(0);
        when(companyMapper.toResponse(company, 0)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.updateCompany(DEFAULT_COMPANY_ID, request);

        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
        verify(companyMapper).updateEntityFromRequest(request, company);
    }

    @Test
    @DisplayName("updateCompany should update company without checking duplicate name when name is unchanged")
    void givenSameName_updateCompany_shouldNotCheckDuplicateAndReturnResponse() {
        Company company = sampleCompany();
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                DEFAULT_NAME,
                UPDATED_DESCRIPTION,
                UPDATED_LOGO_URL,
                UPDATED_CONTACT_EMAIL
        );
        CompanyResponse expectedResponse = sampleUpdatedCompanyResponse();

        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        when(vacancyApi.countActiveVacanciesByCompanyId(DEFAULT_COMPANY_ID)).thenReturn(0);
        when(companyMapper.toResponse(company, 0)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.updateCompany(DEFAULT_COMPANY_ID, request);

        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
        verify(companyMapper).updateEntityFromRequest(request, company);
    }

    @Test
    @DisplayName("updateCompany should throw DuplicateResourceException when new name already exists")
    void givenExistingIdAndDuplicateName_updateCompany_shouldThrowDuplicateResourceException() {
        Company company = sampleCompany();
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();

        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCase(request.name())).thenReturn(true);

        assertThatThrownBy(() -> companyService.updateCompany(DEFAULT_COMPANY_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(expectedDuplicateCompanyNameMessage(request.name()));
    }

    @Test
    @DisplayName("updateCompany should throw ResourceNotFoundException when company does not exist")
    void givenNonExistentId_updateCompany_shouldThrowResourceNotFoundException() {
        UpdateCompanyRequest request = sampleUpdateCompanyRequest();

        when(companyRepository.findById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.updateCompany(NON_EXISTENT_COMPANY_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID));
    }

    @Test
    @DisplayName("deleteCompany should delete company when company exists")
    void givenExistingId_deleteCompany_shouldDeleteCompany() {
        Company company = sampleCompany();

        when(companyRepository.findById(DEFAULT_COMPANY_ID)).thenReturn(Optional.of(company));

        companyService.deleteCompany(DEFAULT_COMPANY_ID);

        verify(companyRepository).delete(company);
    }

    @Test
    @DisplayName("deleteCompany should throw ResourceNotFoundException when company does not exist")
    void givenNonExistentId_deleteCompany_shouldThrowResourceNotFoundException() {
        when(companyRepository.findById(NON_EXISTENT_COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.deleteCompany(NON_EXISTENT_COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedCompanyNotFoundMessage(NON_EXISTENT_COMPANY_ID));
    }
}
