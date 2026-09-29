package ua.edu.ukma.candidai.interview.model;

public enum InterviewStatus {
    SCHEDULED,
    RESCHEDULED,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
