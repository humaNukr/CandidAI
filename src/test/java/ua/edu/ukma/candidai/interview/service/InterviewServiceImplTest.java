package ua.edu.ukma.candidai.interview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
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
import ua.edu.ukma.candidai.interview.model.InterviewType;
import ua.edu.ukma.candidai.interview.repository.InterviewRepository;
import ua.edu.ukma.candidai.recruitment.ApplicationDetails;
import ua.edu.ukma.candidai.recruitment.RecruitmentApi;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private RecruitmentApi recruitmentApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    private UUID applicationId;
    private UUID interviewId;
    private UUID interviewerId;
    private Instant scheduledTime;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        interviewId = UUID.randomUUID();
        interviewerId = UUID.randomUUID();
        scheduledTime = Instant.now().plus(2, ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("scheduleInterview should save interview, update application status and publish event")
    void givenValidRequest_scheduleInterview_shouldSaveAndUpdateStatusAndPublishEvent() {
        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                applicationId,
                interviewerId,
                "John Interviewer",
                InterviewType.TECHNICAL,
                scheduledTime,
                45,
                "https://meet.google.com/xyz",
                "Coding challenge"
        );

        ApplicationDetails appDetails = new ApplicationDetails(
                applicationId, UUID.randomUUID(), "Alice Candidate",
                "alice@example.com", "+380501112233", "resume.pdf",
                ApplicationStatus.APPLIED, "Good match"
        );

        when(recruitmentApi.getApplication(applicationId)).thenReturn(appDetails);
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewResponse response = interviewService.scheduleInterview(request);

        assertThat(response.applicationId()).isEqualTo(applicationId);
        assertThat(response.interviewerName()).isEqualTo("John Interviewer");
        assertThat(response.type()).isEqualTo(InterviewType.TECHNICAL);
        assertThat(response.status()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(response.durationMinutes()).isEqualTo(45);

        verify(recruitmentApi).updateStatus(eq(applicationId), eq(ApplicationStatus.INTERVIEW), any());
        verify(interviewRepository).save(any(Interview.class));

        ArgumentCaptor<InterviewScheduledEvent> eventCaptor = ArgumentCaptor.forClass(InterviewScheduledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().applicationId()).isEqualTo(applicationId);
        assertThat(eventCaptor.getValue().interviewerName()).isEqualTo("John Interviewer");
    }

    @Test
    @DisplayName("scheduleInterview should throw ResourceNotFoundException when application does not exist")
    void givenNonExistentApplication_scheduleInterview_shouldThrowException() {
        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                applicationId, interviewerId, "John", InterviewType.HR_SCREENING,
                scheduledTime, 30, null, null
        );

        when(recruitmentApi.getApplication(applicationId))
                .thenThrow(new ResourceNotFoundException("Application not found"));

        assertThatThrownBy(() -> interviewService.scheduleInterview(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getInterviewById should return response when interview exists")
    void givenExistingId_getInterviewById_shouldReturnResponse() {
        Interview interview = createTestInterview(InterviewStatus.SCHEDULED);
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));

        InterviewResponse response = interviewService.getInterviewById(interviewId);

        assertThat(response.id()).isEqualTo(interviewId);
        assertThat(response.status()).isEqualTo(InterviewStatus.SCHEDULED);
    }

    @Test
    @DisplayName("getInterviewById should throw ResourceNotFoundException when interview missing")
    void givenMissingId_getInterviewById_shouldThrowResourceNotFoundException() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getInterviewById(interviewId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Interview not found with id: " + interviewId);
    }

    @Test
    @DisplayName("getInterviewsByApplicationId should return mapped list")
    void givenApplicationId_getInterviewsByApplicationId_shouldReturnList() {
        Interview interview = createTestInterview(InterviewStatus.SCHEDULED);
        when(interviewRepository.findByApplicationId(applicationId)).thenReturn(List.of(interview));

        List<InterviewResponse> responses = interviewService.getInterviewsByApplicationId(applicationId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).applicationId()).isEqualTo(applicationId);
    }

    @Test
    @DisplayName("rescheduleInterview should update time and publish InterviewRescheduledEvent")
    void givenValidRequest_rescheduleInterview_shouldUpdateAndPublishEvent() {
        Interview interview = createTestInterview(InterviewStatus.SCHEDULED);
        Instant newTime = scheduledTime.plus(3, ChronoUnit.DAYS);
        RescheduleInterviewRequest request = new RescheduleInterviewRequest(
                newTime, 90, "https://zoom.us/new", "Rescheduling reason"
        );

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewResponse response = interviewService.rescheduleInterview(interviewId, request);

        assertThat(response.status()).isEqualTo(InterviewStatus.RESCHEDULED);
        assertThat(response.scheduledAt()).isEqualTo(newTime);
        assertThat(response.durationMinutes()).isEqualTo(90);

        ArgumentCaptor<InterviewRescheduledEvent> eventCaptor
                = ArgumentCaptor.forClass(InterviewRescheduledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().interviewId()).isEqualTo(interviewId);
        assertThat(eventCaptor.getValue().newScheduledAt()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("cancelInterview should set CANCELLED status and publish InterviewCancelledEvent")
    void givenValidRequest_cancelInterview_shouldCancelAndPublishEvent() {
        Interview interview = createTestInterview(InterviewStatus.SCHEDULED);
        CancelInterviewRequest request = new CancelInterviewRequest("Candidate accepted another offer");

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewResponse response = interviewService.cancelInterview(interviewId, request);

        assertThat(response.status()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Candidate accepted another offer");

        ArgumentCaptor<InterviewCancelledEvent> eventCaptor
                = ArgumentCaptor.forClass(InterviewCancelledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reason()).isEqualTo("Candidate accepted another offer");
    }

    @Test
    @DisplayName("completeInterview should set COMPLETED status and publish InterviewCompletedEvent")
    void givenScheduledInterview_completeInterview_shouldCompleteAndPublishEvent() {
        Interview interview = createTestInterview(InterviewStatus.SCHEDULED);

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewResponse response = interviewService.completeInterview(interviewId);

        assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);

        ArgumentCaptor<InterviewCompletedEvent> eventCaptor
                = ArgumentCaptor.forClass(InterviewCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().interviewId()).isEqualTo(interviewId);
    }

    private Interview createTestInterview(InterviewStatus status) {
        return Interview.builder()
                .id(interviewId)
                .applicationId(applicationId)
                .interviewerId(interviewerId)
                .interviewerName("John Interviewer")
                .type(InterviewType.TECHNICAL)
                .status(status)
                .scheduledAt(scheduledTime)
                .durationMinutes(60)
                .meetingLink("https://meet.google.com/test")
                .notes("Notes")
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
    }
}
