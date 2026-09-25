package ua.edu.ukma.candidai.recruitment.repository;

import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
class InMemoryInterviewFeedbackRepository implements InterviewFeedbackRepository {

    private final Map<UUID, List<InterviewFeedbackResponse>> feedbacks = new ConcurrentHashMap<>();

    @Override
    public InterviewFeedbackResponse save(InterviewFeedbackResponse feedback) {
        feedbacks.computeIfAbsent(feedback.applicationId(), k -> new CopyOnWriteArrayList<>()).add(feedback);
        return feedback;
    }

    @Override
    public List<InterviewFeedbackResponse> findByApplicationId(UUID applicationId) {
        List<InterviewFeedbackResponse> list = feedbacks.get(applicationId);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }
}
