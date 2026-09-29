package ua.edu.ukma.candidai.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.company.CompanyApi;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.repository.CompanyRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyApiImpl implements CompanyApi {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean companyExists(UUID id) {
        return companyRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyResponse> findCompanyById(UUID id) {
        return companyRepository.findById(id).map(companyMapper::toResponse);
    }
}
