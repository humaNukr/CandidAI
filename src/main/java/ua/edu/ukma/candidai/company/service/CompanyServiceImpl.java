package ua.edu.ukma.candidai.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.model.Company;
import ua.edu.ukma.candidai.company.repository.CompanyRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        if (companyRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Company with name '" + request.name() + "' already exists");
        }

        Company company = companyMapper.toEntity(request);
        company.setId(UUID.randomUUID());
        company.setCreatedAt(Instant.now());

        Company saved = companyRepository.save(company);
        return companyMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findWithVacanciesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanySummaryResponse> getAllCompanies(String search) {
        List<Company> companies;
        if (search != null && !search.isBlank()) {
            companies = companyRepository.findByNameContainingIgnoreCase(search.trim());
        } else {
            companies = companyRepository.findAll();
        }

        return companies.stream()
                .map(companyMapper::toSummaryResponse)
                .toList();
    }

    @Override
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));

        if (!company.getName().equalsIgnoreCase(request.name())
                && companyRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Company with name '" + request.name() + "' already exists");
        }

        companyMapper.updateEntityFromRequest(request, company);
        Company saved = companyRepository.save(company);
        return companyMapper.toResponse(saved);
    }

    @Override
    public void deleteCompany(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));

        companyRepository.delete(company);
    }
}
