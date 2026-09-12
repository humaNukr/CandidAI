package ua.edu.ukma.candidai.vacancy;

import org.mapstruct.Mapper;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;

@Mapper(config = GlobalMapperConfig.class)
interface VacancyMapper {

    VacancyResponse toResponse(Vacancy vacancy);
}
