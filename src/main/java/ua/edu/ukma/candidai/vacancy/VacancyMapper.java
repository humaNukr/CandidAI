package ua.edu.ukma.candidai.vacancy;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;

@Mapper(config = GlobalMapperConfig.class)
interface VacancyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vacancy toEntity(CreateVacancyRequest request);

    VacancyResponse toResponse(Vacancy vacancy);
}
