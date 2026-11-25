package ua.edu.ukma.candidai.interview.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByApplicationId(UUID applicationId);

    List<Interview> findByInterviewerId(UUID interviewerId);

    List<Interview> findByStatus(InterviewStatus status);

    List<Interview> findByScheduledAtBetween(Instant from, Instant to);

    @Query(
            "SELECT i FROM Interview i WHERE i.status IN :statuses "
                    + "AND i.scheduledAt >= :start AND i.scheduledAt <= :end "
                    + "AND i.reminderSentAt IS NULL"
    )
    List<Interview> findUpcomingPendingReminders(
            @Param("statuses") Collection<InterviewStatus> statuses,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    boolean existsByApplicationIdAndStatus(UUID applicationId, InterviewStatus status);
}
