package ua.edu.ukma.candidai.vacancy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryVacancyRepository implements VacancyRepository {

    private final Map<UUID, Vacancy> storage = new ConcurrentHashMap<>();

    @Override
    public Vacancy save(Vacancy vacancy) {
        storage.put(vacancy.getId(), vacancy);
        return vacancy;
    }

    @Override
    public Optional<Vacancy> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Page<Vacancy> findAll(VacancyStatus status, JobCategory category, Pageable pageable) {
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
        List<Vacancy> content;
        if (pageable.isPaged()) {
            content = sorted.stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .toList();
        } else {
            content = sorted;
        }

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public boolean existsActiveByAuthorIdAndTitle(UUID authorId, String title) {
        String normalizedTitle = title.trim();
        return storage.values().stream()
                .anyMatch(v -> !v.isDeleted()
                        && v.getStatus() == VacancyStatus.OPEN
                        && authorId.equals(v.getAuthorId())
                        && v.getTitle().trim().equalsIgnoreCase(normalizedTitle));
    }
}
