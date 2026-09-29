package ua.edu.ukma.candidai.recruitment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.model.Application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByVacancyIdAndEmail(UUID vacancyId, String email);

    List<Application> findByVacancyId(UUID vacancyId);

    List<Application> findByCandidateId(UUID candidateId);

    List<Application> findByVacancyIdAndStatus(UUID vacancyId, ApplicationStatus status);

    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.feedbacks WHERE a.id = :id")
    Optional<Application> findByIdWithFeedbacks(@Param("id") UUID id);
}
