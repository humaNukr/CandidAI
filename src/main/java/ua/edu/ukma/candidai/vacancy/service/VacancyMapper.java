package ua.edu.ukma.candidai.vacancy.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.Skill;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
interface VacancyMapper {

    @Mapping(target = "requiredSkills", source = "skills")
    @Mapping(target = "preferredSkills", expression = "java(java.util.List.of())")
    VacancyResponse toResponse(Vacancy vacancy);

    default List<String> mapSkillsToStrings(List<Skill> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream()
                .map(Skill::getName)
                .toList();
    }
}
