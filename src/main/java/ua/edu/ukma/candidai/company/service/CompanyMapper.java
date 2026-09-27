package ua.edu.ukma.candidai.company.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.model.Company;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompanyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "vacancies", ignore = true)
    Company toEntity(CreateCompanyRequest request);

    @Mapping(target = "activeVacanciesCount", source = "vacancies", qualifiedByName = "mapActiveVacanciesCount")
    CompanyResponse toResponse(Company company);

    CompanySummaryResponse toSummaryResponse(Company company);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "vacancies", ignore = true)
    void updateEntityFromRequest(UpdateCompanyRequest request, @MappingTarget Company company);

    @Named("mapActiveVacanciesCount")
    default int mapActiveVacanciesCount(List<Vacancy> vacancies) {
        if (vacancies == null) {
            return 0;
        }
        return (int) vacancies.stream()
                .filter(v -> v != null && v.getStatus() == VacancyStatus.OPEN)
                .count();
    }
}
