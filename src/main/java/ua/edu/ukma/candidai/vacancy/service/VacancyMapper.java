package ua.edu.ukma.candidai.vacancy.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;

@Mapper(config = GlobalMapperConfig.class)
interface VacancyMapper {

    @Mapping(target = "companyId", source = "companyId")
    VacancyResponse toResponse(Vacancy vacancy);
}
