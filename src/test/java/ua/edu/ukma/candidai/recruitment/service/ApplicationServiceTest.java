package ua.edu.ukma.candidai.recruitment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.event.ApplicationSubmittedEvent;
import ua.edu.ukma.candidai.recruitment.repository.ApplicationRepository;
import ua.edu.ukma.candidai.vacancy.VacancyApi;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private VacancyApi vacancyApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CommonGenerator commonGenerator;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    @DisplayName("apply - should save application and publish ApplicationSubmittedEvent when valid")
    void givenValidRequest_apply_shouldSaveAndPublishEvent() {
        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"
        );

        when(vacancyApi.isVacancyOpen(VACANCY_ID)).thenReturn(true);
        when(applicationRepository.existsByVacancyIdAndEmail(VACANCY_ID, "john.doe@example.com")).thenReturn(false);
        when(commonGenerator.uuid()).thenReturn(APPLICATION_ID);
        when(commonGenerator.now()).thenReturn(NOW);
        when(applicationRepository.save(any(ApplicationResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse result = applicationService.apply(request);

        assertThat(result.id()).isEqualTo(APPLICATION_ID);
        assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
        verify(applicationRepository).save(any(ApplicationResponse.class));

        ArgumentCaptor<ApplicationSubmittedEvent> captor
                = ArgumentCaptor.forClass(ApplicationSubmittedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ApplicationSubmittedEvent publishedEvent = captor.getValue();
        assertThat(publishedEvent.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(publishedEvent.vacancyId()).isEqualTo(VACANCY_ID);
        assertThat(publishedEvent.email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("apply - should throw DuplicateResourceException when email already applied to vacancy")
    void givenDuplicateEmail_apply_shouldThrowDuplicateResourceException() {
        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"
        );

        when(vacancyApi.isVacancyOpen(VACANCY_ID)).thenReturn(true);
        when(applicationRepository.existsByVacancyIdAndEmail(VACANCY_ID, "john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("apply - should throw ResourceNotFoundException when vacancy is closed or not found")
    void givenClosedVacancy_apply_shouldThrowResourceNotFoundException() {
        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf"
        );

        when(vacancyApi.isVacancyOpen(VACANCY_ID)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.apply(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found or is closed");

        verify(applicationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getById - should return ApplicationResponse when found")
    void givenExistingId_getById_shouldReturnApplication() {
        ApplicationResponse app = new ApplicationResponse(
                APPLICATION_ID,
                VACANCY_ID,
                "John Doe",
                "john.doe@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/john_doe.pdf",
                ApplicationStatus.APPLIED,
                null,
                NOW,
                NOW
        );

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(app));

        ApplicationResponse result = applicationService.getById(APPLICATION_ID);

        assertThat(result).isEqualTo(app);
    }

    @Test
    @DisplayName("getById - should throw ResourceNotFoundException when not found")
    void givenNonExistentId_getById_shouldThrowResourceNotFoundException() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getById(APPLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");
    }
}
