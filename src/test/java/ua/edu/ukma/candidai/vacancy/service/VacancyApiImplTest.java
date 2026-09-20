package ua.edu.ukma.candidai.vacancy.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.vacancy.TestResources.DEFAULT_ID;
import static ua.edu.ukma.candidai.vacancy.TestResources.NON_EXISTENT_ID;
import static ua.edu.ukma.candidai.vacancy.TestResources.aVacancyResponse;

@ExtendWith(MockitoExtension.class)
class VacancyApiImplTest {

    @Mock
    private VacancyService vacancyService;

    @InjectMocks
    private VacancyApiImpl vacancyApi;

    @Test
    @DisplayName("isVacancyOpen should return true when vacancy exists and status is OPEN")
    void givenOpenVacancy_isVacancyOpen_shouldReturnTrue() {
        VacancyResponse openVacancy = aVacancyResponse(VacancyStatus.OPEN);
        when(vacancyService.getVacancyById(DEFAULT_ID)).thenReturn(openVacancy);

        boolean actual = vacancyApi.isVacancyOpen(DEFAULT_ID);

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("isVacancyOpen should return false when vacancy exists and status is not OPEN")
    void givenClosedVacancy_isVacancyOpen_shouldReturnFalse() {
        VacancyResponse closedVacancy = aVacancyResponse(VacancyStatus.CLOSED);
        when(vacancyService.getVacancyById(DEFAULT_ID)).thenReturn(closedVacancy);

        boolean actual = vacancyApi.isVacancyOpen(DEFAULT_ID);

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("isVacancyOpen should return false when vacancy is not found")
    void givenNonExistentVacancy_isVacancyOpen_shouldReturnFalse() {
        when(vacancyService.getVacancyById(NON_EXISTENT_ID))
                .thenThrow(new ResourceNotFoundException("Vacancy not found with id: " + NON_EXISTENT_ID));

        boolean actual = vacancyApi.isVacancyOpen(NON_EXISTENT_ID);

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("getVacancyCategory should return vacancy category when vacancy exists")
    void givenExistingVacancy_getVacancyCategory_shouldReturnCategory() {
        VacancyResponse vacancy = aVacancyResponse(JobCategory.DATA_AI);
        when(vacancyService.getVacancyById(DEFAULT_ID)).thenReturn(vacancy);

        JobCategory actual = vacancyApi.getVacancyCategory(DEFAULT_ID);

        assertThat(actual).isEqualTo(JobCategory.DATA_AI);
    }

    @Test
    @DisplayName("getVacancyCategory should propagate ResourceNotFoundException when vacancy not found")
    void givenNonExistentVacancy_getVacancyCategory_shouldThrowException() {
        when(vacancyService.getVacancyById(NON_EXISTENT_ID))
                .thenThrow(new ResourceNotFoundException("Vacancy not found with id: " + NON_EXISTENT_ID));

        assertThatThrownBy(() -> vacancyApi.getVacancyCategory(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vacancy not found with id: " + NON_EXISTENT_ID);
    }
}
