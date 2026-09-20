package ua.edu.ukma.candidai.vacancy;

import ua.edu.ukma.candidai.vacancy.model.JobCategory;

import java.util.UUID;

public interface VacancyApi {

    boolean isVacancyOpen(UUID vacancyId);

    JobCategory getVacancyCategory(UUID vacancyId);
}
