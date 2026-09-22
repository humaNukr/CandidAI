package ua.edu.ukma.candidai.recruitment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.vacancy.VacancyApi;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final VacancyApi vacancyApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CommonGenerator commonGenerator;

    @Override
    public ApplicationResponse apply(ApplyForVacancyRequest request) {
        log.info("Processing job application for candidate '{}' on vacancy: {}",
                request.candidateName(), request.vacancyId());

        if (!vacancyApi.isVacancyOpen(request.vacancyId())) {
            log.warn("Application rejected: vacancy {} is not open or not found", request.vacancyId());
            throw new ResourceNotFoundException("Vacancy not found or is closed with id: " + request.vacancyId());
        }

        if (applicationRepository.existsByVacancyIdAndEmail(request.vacancyId(), request.email())) {
            log.warn("Application rejected: candidate with email {} already applied to vacancy {}",
                    request.email(), request.vacancyId());
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
                saved.id(),
                saved.vacancyId(),
                saved.candidateName(),
                saved.email(),
                saved.resumeUrl(),
                saved.appliedAt()
        ));
        log.info("Successfully created application {} for vacancy {}", saved.id(), saved.vacancyId());

        return saved;
    }

    @Override
    public ApplicationResponse getById(UUID id) {
        log.debug("Fetching application with id: {}", id);
        return applicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Application not found with id: {}", id);
                    return new ResourceNotFoundException("Application not found with id: " + id);
                });
    }

    @Override
    public ApplicationResponse updateStatus(UUID id, UpdateApplicationStatusRequest request) {
        ApplicationResponse existing = getById(id);

        Instant now = commonGenerator.now();
        String comment = request.comment() != null ? request.comment() : existing.comment();

        ApplicationResponse updated = new ApplicationResponse(
                existing.id(),
                existing.vacancyId(),
                existing.candidateName(),
                existing.email(),
                existing.phone(),
                existing.resumeUrl(),
                request.status(),
                comment,
                existing.appliedAt(),
                now
        );

        ApplicationResponse saved = applicationRepository.save(updated);
        log.info("Updated status for application {} to {}", saved.id(), request.status());

        return saved;
    }
}
