package ua.edu.ukma.candidai.vacancy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.VacancyStatusChangedEvent;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Skill;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.repository.SkillRepository;
import ua.edu.ukma.candidai.vacancy.repository.VacancyRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class VacancyServiceImpl implements VacancyService {

    private final VacancyRepository vacancyRepository;
    private final SkillRepository skillRepository;
    private final VacancyMapper vacancyMapper;
    private final CommonGenerator generator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public VacancyResponse createVacancy(CreateVacancyRequest request) {
        if (vacancyRepository.existsByAuthorIdAndTitleIgnoreCaseAndStatusAndDeletedFalse(
                request.authorId(), request.title(), VacancyStatus.OPEN)) {
            log.warn("Duplicate active vacancy creation rejected for author {} with title '{}'",
                    request.authorId(), request.title());
            throw new DuplicateResourceException(
                    "Active vacancy with title '" + request.title() + "' already exists for author "
                            + request.authorId()
            );
        }
        List<Skill> resolvedSkills = resolveOrCreateSkills(request.requiredSkills(), request.preferredSkills());
        Instant now = generator.now();
        Vacancy vacancy = Vacancy.create(request, generator.uuid(), now, resolvedSkills);
        vacancyRepository.save(vacancy);
        log.info("Created vacancy {} with title '{}' for author {}",
                vacancy.getId(), vacancy.getTitle(), vacancy.getAuthorId());
        return vacancyMapper.toResponse(vacancy);
    }

    @Override
    public Page<VacancyResponse> getAllVacancies(VacancyStatus status, JobCategory category, Pageable pageable) {
        return vacancyRepository.findAllActive(status, category, pageable).map(vacancyMapper::toResponse);
    }

    @Override
    public VacancyResponse getVacancyById(UUID id) {
        Vacancy vacancy = findActiveVacancyOrThrow(id);
        return vacancyMapper.toResponse(vacancy);
    }

    @Override
    @Transactional
    public VacancyResponse updateVacancyStatus(UUID id, UpdateVacancyStatusRequest request) {
        Vacancy vacancy = findActiveVacancyOrThrow(id);
        VacancyStatus oldStatus = vacancy.getStatus();
        Instant now = generator.now();
        vacancy.updateStatus(request.status(), now);
        vacancyRepository.save(vacancy);
        log.info("Updated vacancy {} status from {} to {}", id, oldStatus, request.status());

        eventPublisher.publishEvent(new VacancyStatusChangedEvent(
                vacancy.getId(),
                vacancy.getTitle(),
                vacancy.getAuthorId(),
                oldStatus,
                vacancy.getStatus(),
                now
        ));

        return vacancyMapper.toResponse(vacancy);
    }

    @Override
    @Transactional
    public void deleteVacancy(UUID id) {
        Vacancy vacancy = findActiveVacancyOrThrow(id);
        vacancy.softDelete(generator.now());
        vacancyRepository.save(vacancy);
        log.info("Soft-deleted vacancy with id: {}", id);
    }

    private List<Skill> resolveOrCreateSkills(List<String> required, List<String> preferred) {
        List<String> allSkillNames = new ArrayList<>();
        if (required != null) {
            required.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .forEach(name -> {
                        if (allSkillNames.stream().noneMatch(n -> n.equalsIgnoreCase(name))) {
                            allSkillNames.add(name);
                        }
                    });
        }
        if (preferred != null) {
            preferred.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .forEach(name -> {
                        if (allSkillNames.stream().noneMatch(n -> n.equalsIgnoreCase(name))) {
                            allSkillNames.add(name);
                        }
                    });
        }

        List<Skill> result = new ArrayList<>();
        for (String skillName : allSkillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                    .orElseGet(() -> skillRepository.save(Skill.builder()
                            .id(generator.uuid())
                            .name(skillName)
                            .build()));
            result.add(skill);
        }
        return result;
    }

    private Vacancy findActiveVacancyOrThrow(UUID id) {
        Vacancy vacancy = vacancyRepository.findWithSkillsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + id));
        if (vacancy.isDeleted()) {
            throw new ResourceNotFoundException("Vacancy not found with id: " + id);
        }
        return vacancy;
    }
}
