package ua.edu.ukma.candidai.recruitment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.recruitment.model.InterviewFeedback;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {

    List<InterviewFeedback> findByApplicationId(UUID applicationId);

    @Query("SELECT f FROM InterviewFeedback f JOIN FETCH f.application WHERE f.id = :id")
    Optional<InterviewFeedback> findByIdWithApplication(@Param("id") UUID id);

    @Query("SELECT f FROM InterviewFeedback f JOIN FETCH f.application WHERE f.application.id = :applicationId")
    List<InterviewFeedback> findAllByApplicationIdWithApplication(@Param("applicationId") UUID applicationId);
}
