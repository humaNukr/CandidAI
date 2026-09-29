package ua.edu.ukma.candidai.interview.service;

import ua.edu.ukma.candidai.interview.dto.CancelInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.InterviewResponse;
import ua.edu.ukma.candidai.interview.dto.RescheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.ScheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;

import java.util.List;
import java.util.UUID;

public interface InterviewService {

    InterviewResponse scheduleInterview(ScheduleInterviewRequest request);

    InterviewResponse getInterviewById(UUID id);

    List<InterviewResponse> getInterviewsByApplicationId(UUID applicationId);

    List<InterviewResponse> getInterviewsByInterviewerId(UUID interviewerId);

    List<InterviewResponse> getInterviewsByStatus(InterviewStatus status);

    InterviewResponse rescheduleInterview(UUID id, RescheduleInterviewRequest request);

    InterviewResponse cancelInterview(UUID id, CancelInterviewRequest request);

    InterviewResponse completeInterview(UUID id);
}
