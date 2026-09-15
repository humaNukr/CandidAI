package ua.edu.ukma.candidai.vacancy.service;

import org.mapstruct.Mapper;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;

@Mapper(config = GlobalMapperConfig.class)
interface VacancyMapper {

    VacancyResponse toResponse(Vacancy vacancy);
}
