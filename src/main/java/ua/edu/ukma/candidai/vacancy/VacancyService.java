package ua.edu.ukma.candidai.vacancy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
class VacancyService {

    private final CommonGenerator generator;
    private final VacancyMapper vacancyMapper;
    private final Map<UUID, Vacancy> storage = new ConcurrentHashMap<>();

    public VacancyResponse createVacancy(CreateVacancyRequest request) {
        Vacancy vacancy = Vacancy.create(request, generator.uuid(), generator.now());
        storage.put(vacancy.getId(), vacancy);
        return vacancyMapper.toResponse(vacancy);
    }

    public Page<VacancyResponse> getAllVacancies(VacancyStatus status, JobCategory category, Pageable pageable) {
        List<Vacancy> filtered = storage.values().stream()
                .filter(v -> !v.isDeleted())
                .filter(v -> status == null || status.equals(v.getStatus()))
                .filter(v -> category == null || category.equals(v.getCategory()))
                .toList();

        Comparator<Vacancy> comparator = Comparator.comparing(
                Vacancy::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            if (order.isDescending()) {
                comparator = comparator.reversed();
            }
        } else {
            comparator = comparator.reversed();
        }

        List<Vacancy> sorted = filtered.stream()
                .sorted(comparator)
                .toList();

        long total = sorted.size();
        List<VacancyResponse> content;
        if (pageable.isPaged()) {
            content = sorted.stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .map(vacancyMapper::toResponse)
                    .toList();
        } else {
            content = sorted.stream()
                    .map(vacancyMapper::toResponse)
                    .toList();
        }

        return new PageImpl<>(content, pageable, total);
    }

    public VacancyResponse getVacancyById(UUID id) {
        Vacancy vacancy = storage.get(id);
        if (vacancy == null || vacancy.isDeleted()) {
            throw new ResourceNotFoundException("Vacancy not found with id: " + id);
        }
        return vacancyMapper.toResponse(vacancy);
    }

    public VacancyResponse updateVacancyStatus(UUID id, UpdateVacancyStatusRequest request) {
        Vacancy vacancy = storage.get(id);
        if (vacancy == null || vacancy.isDeleted()) {
            throw new ResourceNotFoundException("Vacancy not found with id: " + id);
        }
        vacancy.updateStatus(request.status(), generator.now());
        return vacancyMapper.toResponse(vacancy);
    }

    public void deleteVacancy(UUID id) {
        Vacancy vacancy = storage.get(id);
        if (vacancy == null || vacancy.isDeleted()) {
            throw new ResourceNotFoundException("Vacancy not found with id: " + id);
        }
        vacancy.softDelete(generator.now());
    }
}
