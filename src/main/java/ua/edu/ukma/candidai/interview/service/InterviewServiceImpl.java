package ua.edu.ukma.candidai.interview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.interview.dto.CancelInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.InterviewResponse;
import ua.edu.ukma.candidai.interview.dto.RescheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.ScheduleInterviewRequest;
import ua.edu.ukma.candidai.interview.event.InterviewCancelledEvent;
import ua.edu.ukma.candidai.interview.event.InterviewCompletedEvent;
import ua.edu.ukma.candidai.interview.event.InterviewRescheduledEvent;
import ua.edu.ukma.candidai.interview.event.InterviewScheduledEvent;
import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.repository.InterviewRepository;
import ua.edu.ukma.candidai.recruitment.ApplicationDetails;
import ua.edu.ukma.candidai.recruitment.RecruitmentApi;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final InterviewRepository interviewRepository;
    private final RecruitmentApi recruitmentApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CommonGenerator commonGenerator;

    @Override
    @Transactional
    public InterviewResponse scheduleInterview(ScheduleInterviewRequest request) {
        log.info("Scheduling interview for application {} with interviewer {}",
                request.applicationId(), request.interviewerName());

        ApplicationDetails application = recruitmentApi.getApplication(request.applicationId());

        if (application.status() == ApplicationStatus.REJECTED || application.status() == ApplicationStatus.HIRED) {
            throw new InvalidStateTransitionException(
                    "Cannot schedule interview for application in terminal status: " + application.status()
            );
        }
        if (application.status() == ApplicationStatus.APPLIED) {
            throw new InvalidStateTransitionException(
                    "Cannot schedule interview for application in APPLIED status. Application must pass screening first"
            );
        }
        if (application.status() != ApplicationStatus.SCREENING
                && application.status() != ApplicationStatus.INTERVIEW) {
            throw new InvalidStateTransitionException(
                    "Cannot schedule interview for application in status: " + application.status()
            );
        }

        Instant now = commonGenerator.now();
        int duration = (request.durationMinutes() != null && request.durationMinutes() > 0)
                ? request.durationMinutes()
                : DEFAULT_DURATION_MINUTES;

        Interview interview = Interview.create(
                commonGenerator.uuid(),
                request.applicationId(),
                request.interviewerId(),
                request.interviewerName(),
                request.type(),
                request.scheduledAt(),
                duration,
                request.meetingLink(),
                request.notes(),
                now
        );

        Interview saved = interviewRepository.save(interview);

        if (application.status() == ApplicationStatus.SCREENING) {
            recruitmentApi.updateStatus(
                    request.applicationId(),
                    ApplicationStatus.INTERVIEW,
                    "Interview scheduled for " + request.scheduledAt()
            );
        }

        eventPublisher.publishEvent(new InterviewScheduledEvent(
                saved.getId(),
                saved.getApplicationId(),
                saved.getInterviewerId(),
                saved.getInterviewerName(),
                saved.getType(),
                saved.getScheduledAt(),
                saved.getDurationMinutes(),
                saved.getMeetingLink()
        ));

        log.info("Successfully scheduled interview {} for application {}", saved.getId(), saved.getApplicationId());
        return InterviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(UUID id) {
        return interviewRepository.findById(id)
                .map(InterviewResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplicationId(UUID applicationId) {
        return interviewRepository.findByApplicationId(applicationId).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByInterviewerId(UUID interviewerId) {
        return interviewRepository.findByInterviewerId(interviewerId).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByStatus(InterviewStatus status) {
        return interviewRepository.findByStatus(status).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public InterviewResponse rescheduleInterview(UUID id, RescheduleInterviewRequest request) {
        log.info("Rescheduling interview {} to new time {}", id, request.newScheduledAt());

        Interview interview = findInterviewOrThrow(id);
        Instant now = commonGenerator.now();

        interview.reschedule(
                request.newScheduledAt(),
                request.durationMinutes(),
                request.meetingLink(),
                request.reason(),
                now
        );
        Interview updated = interviewRepository.save(interview);

        eventPublisher.publishEvent(new InterviewRescheduledEvent(
                updated.getId(),
                updated.getApplicationId(),
                updated.getScheduledAt(),
                updated.getDurationMinutes(),
                updated.getMeetingLink(),
                request.reason()
        ));

        return InterviewResponse.from(updated);
    }

    @Override
    @Transactional
    public InterviewResponse cancelInterview(UUID id, CancelInterviewRequest request) {
        log.info("Cancelling interview {} with reason: {}", id, request.reason());

        Interview interview = findInterviewOrThrow(id);
        Instant now = commonGenerator.now();

        interview.cancel(request.reason(), now);
        Interview updated = interviewRepository.save(interview);

        eventPublisher.publishEvent(new InterviewCancelledEvent(
                updated.getId(),
                updated.getApplicationId(),
                request.reason()
        ));

        return InterviewResponse.from(updated);
    }

    @Override
    @Transactional
    public InterviewResponse completeInterview(UUID id) {
        log.info("Completing interview {}", id);

        Interview interview = findInterviewOrThrow(id);
        Instant now = commonGenerator.now();

        interview.complete(now);
        Interview updated = interviewRepository.save(interview);

        eventPublisher.publishEvent(new InterviewCompletedEvent(
                updated.getId(),
                updated.getApplicationId()
        ));

        return InterviewResponse.from(updated);
    }

    private Interview findInterviewOrThrow(UUID id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
    }
}
