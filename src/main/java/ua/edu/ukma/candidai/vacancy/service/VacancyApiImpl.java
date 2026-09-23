package ua.edu.ukma.candidai.vacancy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class VacancyApiImpl implements VacancyApi {

    private final VacancyService vacancyService;

    @Override
    public boolean isVacancyOpen(UUID vacancyId) {
        try {
            VacancyResponse vacancy = vacancyService.getVacancyById(vacancyId);
            return vacancy.status() == VacancyStatus.OPEN;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    @Override
    public JobCategory getVacancyCategory(UUID vacancyId) {
        return vacancyService.getVacancyById(vacancyId).category();
    }

    @Override
    public VacancyDetails getVacancyDetails(UUID vacancyId) {
        VacancyResponse vacancy = vacancyService.getVacancyById(vacancyId);
        return new VacancyDetails(
                vacancy.id(),
                vacancy.title(),
                vacancy.description(),
                vacancy.requiredSkills(),
                vacancy.preferredSkills(),
                vacancy.seniorityLevel()
        );
    }
}
