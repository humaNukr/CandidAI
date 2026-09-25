package ua.edu.ukma.candidai.recruitment.dto.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ApplicationStatus {
    APPLIED,
    SCREENING,
    INTERVIEW,
    OFFER,
    HIRED,
    REJECTED;

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> VALID_TRANSITIONS
            = new EnumMap<>(ApplicationStatus.class);

    static {
        VALID_TRANSITIONS.put(APPLIED, EnumSet.of(SCREENING, REJECTED));
        VALID_TRANSITIONS.put(SCREENING, EnumSet.of(INTERVIEW, REJECTED));
        VALID_TRANSITIONS.put(INTERVIEW, EnumSet.of(OFFER, REJECTED));
        VALID_TRANSITIONS.put(OFFER, EnumSet.of(HIRED, REJECTED));
        VALID_TRANSITIONS.put(HIRED, Collections.emptySet());
        VALID_TRANSITIONS.put(REJECTED, Collections.emptySet());
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        if (target == null) {
            return false;
        }
        return VALID_TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }
}
