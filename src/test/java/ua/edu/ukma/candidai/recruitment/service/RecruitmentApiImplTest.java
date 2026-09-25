package ua.edu.ukma.candidai.recruitment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.recruitment.ApplicationDetails;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentApiImplTest {

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private RecruitmentApiImpl recruitmentApi;

    @Test
    @DisplayName("getApplication should return ApplicationDetails when application exists")
    void givenExistingApplication_getApplication_shouldReturnDetails() {
        ApplicationResponse response = new ApplicationResponse(
                APPLICATION_ID,
                VACANCY_ID,
                "John Doe",
                "john@example.com",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resume.pdf",
                ApplicationStatus.SCREENING,
                88,
                "Promising candidate",
                NOW,
                NOW
        );

        when(applicationService.getById(APPLICATION_ID)).thenReturn(response);

        ApplicationDetails details = recruitmentApi.getApplication(APPLICATION_ID);

        assertThat(details.id()).isEqualTo(APPLICATION_ID);
        assertThat(details.vacancyId()).isEqualTo(VACANCY_ID);
        assertThat(details.candidateName()).isEqualTo("John Doe");
        assertThat(details.email()).isEqualTo("john@example.com");
        assertThat(details.phone()).isEqualTo("+380501234567");
        assertThat(details.resumeUrl()).isEqualTo("https://storage.candidai.ukma.edu.ua/resume.pdf");
        assertThat(details.status()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(details.matchingScore()).isEqualTo(88);
        assertThat(details.comment()).isEqualTo("Promising candidate");
    }

    @Test
    @DisplayName("getApplication should propagate ResourceNotFoundException when application not found")
    void givenNonExistentApplication_getApplication_shouldThrowException() {
        when(applicationService.getById(APPLICATION_ID))
                .thenThrow(new ResourceNotFoundException("Application not found with id: " + APPLICATION_ID));

        assertThatThrownBy(() -> recruitmentApi.getApplication(APPLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found with id: " + APPLICATION_ID);
    }

    @Test
    @DisplayName("updateStatus with 3 params should delegate with null matching score")
    void givenStatusAndComment_updateStatus_shouldDelegateWithNullScore() {
        recruitmentApi.updateStatus(APPLICATION_ID, ApplicationStatus.INTERVIEW, "Passed screening");

        verify(applicationService).updateStatus(
                APPLICATION_ID,
                new UpdateApplicationStatusRequest(ApplicationStatus.INTERVIEW, "Passed screening", null)
        );
    }

    @Test
    @DisplayName("updateStatus with 4 params should delegate with matching score")
    void givenStatusScoreAndComment_updateStatus_shouldDelegateWithScore() {
        recruitmentApi.updateStatus(APPLICATION_ID, ApplicationStatus.INTERVIEW, 92, "High score");

        verify(applicationService).updateStatus(
                APPLICATION_ID,
                new UpdateApplicationStatusRequest(ApplicationStatus.INTERVIEW, "High score", 92)
        );
    }
}
