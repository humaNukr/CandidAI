package ua.edu.ukma.candidai.vacancy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.util.UUID;

public interface VacancyService {

    VacancyResponse createVacancy(CreateVacancyRequest request);

    Page<VacancyResponse> getAllVacancies(VacancyStatus status, JobCategory category, Pageable pageable);

    VacancyResponse getVacancyById(UUID id);

    VacancyResponse updateVacancyStatus(UUID id, UpdateVacancyStatusRequest request);

    void deleteVacancy(UUID id);
}
