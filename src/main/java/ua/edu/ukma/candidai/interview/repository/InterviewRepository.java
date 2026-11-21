package ua.edu.ukma.candidai.interview.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByApplicationId(UUID applicationId);

    List<Interview> findByInterviewerId(UUID interviewerId);

    List<Interview> findByStatus(InterviewStatus status);

    List<Interview> findByScheduledAtBetween(Instant from, Instant to);

    @org.springframework.data.jpa.repository.Query(
            "SELECT i FROM Interview i WHERE i.status IN :statuses "
                    + "AND i.scheduledAt >= :start AND i.scheduledAt <= :end "
                    + "AND i.reminderSentAt IS NULL"
    )
    List<Interview> findUpcomingPendingReminders(
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<InterviewStatus> statuses,
            @org.springframework.data.repository.query.Param("start") Instant start,
            @org.springframework.data.repository.query.Param("end") Instant end
    );

    boolean existsByApplicationIdAndStatus(UUID applicationId, InterviewStatus status);
}
