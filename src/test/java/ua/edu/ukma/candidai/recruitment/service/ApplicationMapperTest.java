package ua.edu.ukma.candidai.recruitment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.model.Application;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationMapperTest {

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-24T12:00:00Z");

    private final ApplicationMapper mapper = new ApplicationMapper();

    @Test
    @DisplayName("toResponse - should map Application entity to ApplicationResponse DTO")
    void givenApplicationEntity_toResponse_shouldMapAllFields() {
        Application entity = Application.builder()
                .id(APPLICATION_ID)
                .vacancyId(VACANCY_ID)
                .candidateName("Alice Smith")
                .email("alice@example.com")
                .phone("+380509876543")
                .resumeUrl("https://storage.candidai.ukma.edu.ua/resumes/alice.pdf")
                .status(ApplicationStatus.SCREENING)
                .comment("Passed initial screening")
                .appliedAt(NOW)
                .updatedAt(NOW)
                .build();

        ApplicationResponse response = mapper.toResponse(entity);

        assertThat(response.id()).isEqualTo(APPLICATION_ID);
        assertThat(response.vacancyId()).isEqualTo(VACANCY_ID);
        assertThat(response.candidateName()).isEqualTo("Alice Smith");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.phone()).isEqualTo("+380509876543");
        assertThat(response.resumeUrl()).isEqualTo("https://storage.candidai.ukma.edu.ua/resumes/alice.pdf");
        assertThat(response.status()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(response.comment()).isEqualTo("Passed initial screening");
        assertThat(response.appliedAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("toResponse - should return null when application is null")
    void givenNullApplication_toResponse_shouldReturnNull() {
        ApplicationResponse response = mapper.toResponse(null);

        assertThat(response).isNull();
    }
}
