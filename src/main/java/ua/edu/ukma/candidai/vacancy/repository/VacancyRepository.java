package ua.edu.ukma.candidai.vacancy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.util.Optional;
import java.util.UUID;

public interface VacancyRepository {

    Vacancy save(Vacancy vacancy);

    Optional<Vacancy> findById(UUID id);

    Page<Vacancy> findAll(VacancyStatus status, JobCategory category, Pageable pageable);

    boolean existsActiveByAuthorIdAndTitle(UUID authorId, String title);
}
