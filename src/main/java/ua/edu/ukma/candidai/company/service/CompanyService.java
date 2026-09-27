package ua.edu.ukma.candidai.company.service;

import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CompanyService {

    CompanyResponse createCompany(CreateCompanyRequest request);

    CompanyResponse getCompanyById(UUID id);

    List<CompanySummaryResponse> getCompanies(String name);

    CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request);

    void deleteCompany(UUID id);
}
