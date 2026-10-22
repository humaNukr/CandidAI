package ua.edu.ukma.candidai.recruitment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.candidate.CandidateApplicationLookup;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateApplicationLookupImpl implements CandidateApplicationLookup {

    private final ApplicationRepository applicationRepository;

    @Override
    public Map<UUID, String> findAppliedVacancyStatusesByCandidateId(UUID candidateId) {
        if (candidateId == null) {
            return Map.of();
        }

        return applicationRepository.findByCandidateId(candidateId).stream()
                .collect(Collectors.toMap(
                        Application::getVacancyId,
                        app -> app.getStatus().name(),
                        (existing, replacement) -> existing
                ));
    }
}
