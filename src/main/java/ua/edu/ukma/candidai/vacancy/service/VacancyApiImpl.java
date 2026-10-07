package ua.edu.ukma.candidai.vacancy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.repository.VacancyRepository;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class VacancyApiImpl implements VacancyApi {

    private final VacancyService vacancyService;
    private final VacancyRepository vacancyRepository;

    @Override
    public boolean isVacancyOpen(UUID vacancyId) {
        try {
            VacancyResponse vacancy = vacancyService.getVacancyById(vacancyId);
            return vacancy.status() == VacancyStatus.OPEN;
        } catch (ResourceNotFoundException e) {
            log.debug("Vacancy {} not found when checking if open: {}", vacancyId, e.getMessage());
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

    @Override
    public int countActiveVacanciesByCompanyId(UUID companyId) {
        if (companyId == null) {
            return 0;
        }
        return (int) vacancyRepository.countByCompanyIdAndStatus(companyId, VacancyStatus.OPEN);
    }
}
