package ua.edu.ukma.candidai.recruitment.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationStatusCount;
import ua.edu.ukma.candidai.recruitment.model.Application;

@Repository
public interface ApplicationFunnelRepository extends JpaRepository<Application, UUID> {

    @Query("""
            SELECT new ua.edu.ukma.candidai.recruitment.dto.response.ApplicationStatusCount(a.status, COUNT(a))
            FROM Application a
            GROUP BY a.status
            """)
    List<ApplicationStatusCount> countApplicationsByStatus();

    @Query("SELECT AVG(a.matchingScore) FROM Application a WHERE a.matchingScore IS NOT NULL")
    Double findAverageMatchingScore();
}
