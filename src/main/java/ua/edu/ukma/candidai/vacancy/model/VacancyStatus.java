package ua.edu.ukma.candidai.vacancy.model;

public enum VacancyStatus {
    DRAFT,
    OPEN,
    PAUSED,
    CLOSED,
    ARCHIVED;

    public boolean canTransitionTo(VacancyStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case DRAFT -> target == OPEN || target == ARCHIVED;
            case OPEN -> target == PAUSED || target == CLOSED || target == ARCHIVED;
            case PAUSED -> target == OPEN || target == CLOSED || target == ARCHIVED;
            case CLOSED -> target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
