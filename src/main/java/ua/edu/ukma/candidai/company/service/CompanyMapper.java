package ua.edu.ukma.candidai.company.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.model.Company;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompanyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Company toEntity(CreateCompanyRequest request);

    @Mapping(target = "activeVacanciesCount", source = "activeVacanciesCount")
    CompanyResponse toResponse(Company company, int activeVacanciesCount);

    default CompanyResponse toResponse(Company company) {
        return toResponse(company, 0);
    }

    CompanySummaryResponse toSummaryResponse(Company company);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(UpdateCompanyRequest request, @MappingTarget Company company);
}
