package ua.edu.ukma.candidai.company.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.company.model.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByNameIgnoreCase(String name);

    List<Company> findByNameContainingIgnoreCase(String namePattern);

    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "vacancies")
    Optional<Company> findWithVacanciesById(UUID id);

    @EntityGraph(attributePaths = "vacancies")
    List<Company> findAllWithVacanciesBy();

    @Query("""
        SELECT DISTINCT c FROM Company c
        LEFT JOIN FETCH c.vacancies v
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR (v.status = ua.edu.ukma.candidai.vacancy.model.VacancyStatus.OPEN
               AND LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY c.name ASC
    """)
    List<Company> searchByKeywordWithOpenVacancies(@Param("keyword") String keyword);
}
