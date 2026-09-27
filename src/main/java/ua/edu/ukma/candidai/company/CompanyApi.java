package ua.edu.ukma.candidai.company;

import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;

import java.util.Optional;
import java.util.UUID;

public interface CompanyApi {

    boolean companyExists(UUID id);

    Optional<CompanyResponse> findCompanyById(UUID id);
}
