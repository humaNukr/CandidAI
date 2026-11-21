package ua.edu.ukma.candidai.interview.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.interview.event.InterviewReminderEvent;
import ua.edu.ukma.candidai.interview.model.Interview;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.repository.InterviewRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewReminderScheduler {

    private static final List<InterviewStatus> ACTIVE_INTERVIEW_STATUSES = List.of(
            InterviewStatus.SCHEDULED,
            InterviewStatus.RESCHEDULED
    );

    private final InterviewRepository interviewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CommonGenerator commonGenerator;

    @Value("${candidai.scheduler.interview-reminder.window-hours:24}")
    private int windowHours;

    @Scheduled(
            fixedDelayString = "${candidai.scheduler.interview-reminder.fixed-delay-minutes:30}",
            timeUnit = TimeUnit.MINUTES
    )
    @SchedulerLock(name = "InterviewReminderJob", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    @Transactional
    public void sendUpcomingInterviewReminders() {
        Instant now = commonGenerator.now();
        Instant windowEnd = now.plus(Duration.ofHours(windowHours));

        List<Interview> upcomingInterviews = interviewRepository
                .findUpcomingPendingReminders(
                        ACTIVE_INTERVIEW_STATUSES,
                        now,
                        windowEnd
                );

        for (Interview interview : upcomingInterviews) {
            interview.markReminderSent(now);
            eventPublisher.publishEvent(new InterviewReminderEvent(
                    interview.getId(),
                    interview.getApplicationId(),
                    interview.getInterviewerId(),
                    interview.getInterviewerName(),
                    interview.getType(),
                    interview.getScheduledAt(),
                    interview.getDurationMinutes(),
                    interview.getMeetingLink()
            ));
        }

        if (!upcomingInterviews.isEmpty()) {
            interviewRepository.saveAll(upcomingInterviews);
        }

        log.info("[SCHEDULER] InterviewReminderJob: sent reminders for {} upcoming interview(s) between {} and {}",
                upcomingInterviews.size(), now, windowEnd);
    }
}
