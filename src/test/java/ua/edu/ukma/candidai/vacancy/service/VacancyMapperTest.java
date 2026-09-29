package ua.edu.ukma.candidai.vacancy.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

class VacancyMapperTest {

    private final VacancyMapper mapper = Mappers.getMapper(VacancyMapper.class);

    @Test
    @DisplayName("toResponse - should map vacancy with skills to expected response")
    void givenVacancyWithSkills_toResponse_shouldMapAllFieldsRecursively() {
        Vacancy vacancy = aVacancy();
        VacancyResponse expected = aVacancyResponse();

        VacancyResponse actual = mapper.toResponse(vacancy);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
