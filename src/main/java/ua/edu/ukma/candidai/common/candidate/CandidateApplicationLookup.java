package ua.edu.ukma.candidai.common.candidate;

import java.util.Map;
import java.util.UUID;

public interface CandidateApplicationLookup {

    Map<UUID, String> findAppliedVacancyStatusesByCandidateId(UUID candidateId);
}
