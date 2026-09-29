package ua.edu.ukma.candidai.vacancy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancyRepository extends JpaRepository<Vacancy, UUID> {

    @EntityGraph(attributePaths = "skills")
    Optional<Vacancy> findWithSkillsById(UUID id);

    @EntityGraph(attributePaths = "skills")
    List<Vacancy> findAllWithSkillsBy();

    List<Vacancy> findByStatus(VacancyStatus status);

    List<Vacancy> findByCompanyId(UUID companyId);

    List<Vacancy> findByStatusAndCompanyId(VacancyStatus status, UUID companyId);

    boolean existsByAuthorIdAndTitleIgnoreCaseAndStatusAndDeletedFalse(
            UUID authorId,
            String title,
            VacancyStatus status
    );

    default boolean existsActiveByAuthorIdAndTitle(UUID authorId, String title) {
        return existsByAuthorIdAndTitleIgnoreCaseAndStatusAndDeletedFalse(authorId, title, VacancyStatus.OPEN);
    }

    @Query(value = """
        SELECT DISTINCT v FROM Vacancy v
        LEFT JOIN FETCH v.skills s
        WHERE (:status IS NULL OR v.status = :status)
          AND (:category IS NULL OR v.category = :category)
          AND v.deleted = false
    """,
    countQuery = """
        SELECT count(DISTINCT v) FROM Vacancy v
        WHERE (:status IS NULL OR v.status = :status)
          AND (:category IS NULL OR v.category = :category)
          AND v.deleted = false
    """)
    Page<Vacancy> findAllActive(
            @Param("status") VacancyStatus status,
            @Param("category") JobCategory category,
            Pageable pageable
    );

    default Page<Vacancy> findAll(VacancyStatus status, JobCategory category, Pageable pageable) {
        return findAllActive(status, category, pageable);
    }
}
