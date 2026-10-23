package ua.edu.ukma.candidai.recruitment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.*;

@ExtendWith(MockitoExtension.class)
class CandidateApplicationLookupImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private CandidateApplicationLookupImpl candidateApplicationLookup;

    @Test
    @DisplayName("findAppliedVacancyStatusesByCandidateId should return vacancy status map when applications exist")
    void givenCandidateIdWithApplications_findAppliedVacancyStatusesByCandidateId_shouldReturnVacancyStatusMap() {
        UUID vacancyId1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID vacancyId2 = UUID.fromString("00000000-0000-0000-0000-000000000020");
        Application app1 = anApplicationBuilder()
                .vacancyId(vacancyId1)
                .status(ApplicationStatus.APPLIED)
                .build();
        Application app2 = anApplicationBuilder()
                .vacancyId(vacancyId2)
                .status(ApplicationStatus.INTERVIEW)
                .build();
        when(applicationRepository.findByCandidateId(DEFAULT_CANDIDATE_ID)).thenReturn(List.of(app1, app2));

        Map<UUID, String> result = candidateApplicationLookup.findAppliedVacancyStatusesByCandidateId(
                DEFAULT_CANDIDATE_ID);

        Map<UUID, String> expected = Map.of(
                vacancyId1, ApplicationStatus.APPLIED.name(),
                vacancyId2, ApplicationStatus.INTERVIEW.name()
        );
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("findAppliedVacancyStatusesByCandidateId should return empty map when candidate has no applications")
    void givenCandidateIdWithNoApplications_findAppliedVacancyStatusesByCandidateId_shouldReturnEmptyMap() {
        when(applicationRepository.findByCandidateId(DEFAULT_CANDIDATE_ID)).thenReturn(List.of());

        Map<UUID, String> result = candidateApplicationLookup.findAppliedVacancyStatusesByCandidateId(
                DEFAULT_CANDIDATE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAppliedVacancyStatusesByCandidateId should return empty map when candidateId is null")
    void givenNullCandidateId_findAppliedVacancyStatusesByCandidateId_shouldReturnEmptyMap() {
        Map<UUID, String> result = candidateApplicationLookup.findAppliedVacancyStatusesByCandidateId(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAppliedVacancyStatusesByCandidateId should resolve collisions by keeping first status")
    void givenDuplicateApplications_findAppliedVacancyStatusesByCandidateId_shouldKeepFirstStatus() {
        UUID vacancyId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        Application app1 = anApplicationBuilder()
                .vacancyId(vacancyId)
                .status(ApplicationStatus.APPLIED)
                .build();
        Application app2 = anApplicationBuilder()
                .vacancyId(vacancyId)
                .status(ApplicationStatus.REJECTED)
                .build();
        when(applicationRepository.findByCandidateId(DEFAULT_CANDIDATE_ID)).thenReturn(List.of(app1, app2));

        Map<UUID, String> result = candidateApplicationLookup.findAppliedVacancyStatusesByCandidateId(
                DEFAULT_CANDIDATE_ID);

        assertThat(result).containsExactly(Map.entry(vacancyId, ApplicationStatus.APPLIED.name()));
    }
}
