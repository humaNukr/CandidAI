package ua.edu.ukma.candidai.recruitment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;


import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final VacancyApi vacancyApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CommonGenerator commonGenerator;

    @Override
    public ApplicationResponse apply(ApplyForVacancyRequest request) {
        if (!vacancyApi.isVacancyOpen(request.vacancyId())) {
            throw new ResourceNotFoundException("Vacancy not found or is closed with id: " + request.vacancyId());
        }

        if (applicationRepository.existsByVacancyIdAndEmail(request.vacancyId(), request.email())) {
            throw new DuplicateResourceException(
                    "Candidate with email " + request.email() + " has already applied to vacancy " + request.vacancyId()
            );
        }
        UUID applicationId = commonGenerator.uuid();
        Instant now = commonGenerator.now();

        ApplicationResponse application = new ApplicationResponse(
                applicationId,
                request.vacancyId(),
                request.candidateName(),
                request.email(),
                request.phone(),
                request.resumeUrl(),
                ApplicationStatus.APPLIED,
                null,
                now,
                now
        );

        ApplicationResponse saved = applicationRepository.save(application);

        eventPublisher.publishEvent(new ApplicationSubmittedEvent(
                applicationId,
                request.vacancyId(),
                request.email(),
                now
        ));

        return saved;
    }

    @Override
    public ApplicationResponse getById(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }
}
