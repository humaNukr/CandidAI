package ua.edu.ukma.candidai.recruitment.scheduler;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationStatusCount;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationFunnelRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for CandidateFunnelMetricsScheduler")
class CandidateFunnelMetricsSchedulerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-24T12:00:00Z");

    @Mock
    private ApplicationFunnelRepository funnelRepository;

    @Mock
    private CommonGenerator commonGenerator;

    @InjectMocks
    private CandidateFunnelMetricsScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(commonGenerator.now()).thenReturn(FIXED_NOW);
    }

    @Test
    @DisplayName("calculateFunnelMetrics - should aggregate counts, hire rate, and average score")
    void calculateFunnelMetrics_WhenApplicationsExist_ShouldComputeFunnelSnapshot() {
        when(funnelRepository.countApplicationsByStatus()).thenReturn(List.of(
                new ApplicationStatusCount(ApplicationStatus.APPLIED, 4L),
                new ApplicationStatusCount(ApplicationStatus.SCREENING, 2L),
                new ApplicationStatusCount(ApplicationStatus.INTERVIEW, 2L),
                new ApplicationStatusCount(ApplicationStatus.HIRED, 1L),
                new ApplicationStatusCount(ApplicationStatus.REJECTED, 1L)
        ));
        when(funnelRepository.findAverageMatchingScore()).thenReturn(78.666);

        scheduler.calculateFunnelMetrics();

        CandidateFunnelMetricsSnapshot snapshot = scheduler.getLatestSnapshot().orElseThrow();
        assertThat(snapshot.calculatedAt()).isEqualTo(FIXED_NOW);
        assertThat(snapshot.totalApplications()).isEqualTo(10L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.APPLIED)).isEqualTo(4L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.SCREENING)).isEqualTo(2L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.INTERVIEW)).isEqualTo(2L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.OFFER)).isZero();
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.HIRED)).isEqualTo(1L);
        assertThat(snapshot.countsByStatus().get(ApplicationStatus.REJECTED)).isEqualTo(1L);
        assertThat(snapshot.hireRatePercent()).isEqualTo(10.0);
        assertThat(snapshot.averageMatchingScore()).isEqualTo(78.67);
    }

    @Test
    @DisplayName("calculateFunnelMetrics - should handle empty repository safely without division by zero")
    void calculateFunnelMetrics_WhenNoApplications_ShouldReturnZeroedSnapshot() {
        when(funnelRepository.countApplicationsByStatus()).thenReturn(List.of());
        when(funnelRepository.findAverageMatchingScore()).thenReturn(null);

        scheduler.calculateFunnelMetrics();

        CandidateFunnelMetricsSnapshot snapshot = scheduler.getLatestSnapshot().orElseThrow();
        assertThat(snapshot.calculatedAt()).isEqualTo(FIXED_NOW);
        assertThat(snapshot.totalApplications()).isZero();
        assertThat(snapshot.hireRatePercent()).isZero();
        assertThat(snapshot.averageMatchingScore()).isNull();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertThat(snapshot.countsByStatus().get(status)).isZero();
        }
    }
}
