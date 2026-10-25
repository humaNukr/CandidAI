package ua.edu.ukma.candidai.testutils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;

public final class IntegrationTestResources {

    public static final UUID SEED_COMPANY_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");
    public static final UUID SEED_RECRUITER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    public static final UUID SEED_CANDIDATE_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");

    private IntegrationTestResources() {
    }

    public static CreateCompanyRequest aValidCreateCompanyRequest() {
        return new CreateCompanyRequest(
                "Nova Global Tech",
                "Leading high-tech innovation provider",
                "https://candidai.ukma.edu.ua/logos/nova.png",
                "hr@novatech.ua"
        );
    }

    public static CreateCompanyRequest anInvalidCreateCompanyRequest() {
        return new CreateCompanyRequest(
                "",
                "Description",
                "https://candidai.ukma.edu.ua/logos/invalid.png",
                "invalid-email"
        );
    }

    public static CreateVacancyRequest aValidCreateVacancyRequest() {
        return new CreateVacancyRequest(
                SEED_RECRUITER_ID,
                SEED_RECRUITER_ID,
                SEED_COMPANY_ID,
                VacancyStatus.OPEN,
                "Lead Cloud Architect",
                JobCategory.ENGINEERING,
                "Cloud Systems",
                "Lead",
                6,
                "Leading high-load cloud architectures and Kubernetes clusters.",
                List.of("Java 25", "Spring Boot", "PostgreSQL"),
                List.of("Docker", "Kubernetes"),
                EnglishLevel.C1,
                new BigDecimal("5000.00"),
                new BigDecimal("7000.00"),
                "USD",
                EmploymentType.FULL_TIME,
                LocationType.REMOTE,
                "Kyiv, Ukraine",
                Instant.now().plus(Duration.ofDays(30))
        );
    }

    public static CreateVacancyRequest anInvalidCreateVacancyRequest() {
        return new CreateVacancyRequest(
                SEED_RECRUITER_ID,
                null,
                SEED_COMPANY_ID,
                VacancyStatus.OPEN,
                "",
                JobCategory.ENGINEERING,
                "",
                "",
                -1,
                "short",
                List.of(),
                List.of(),
                EnglishLevel.B1,
                new BigDecimal("7000.00"),
                new BigDecimal("5000.00"),
                "US",
                EmploymentType.FULL_TIME,
                LocationType.REMOTE,
                "",
                Instant.now().minus(Duration.ofDays(1))
        );
    }

    public static ApplyForVacancyRequest aValidApplyRequest(UUID vacancyId) {
        return new ApplyForVacancyRequest(
                vacancyId,
                SEED_CANDIDATE_ID,
                "Taras Shevchenko",
                "candidate@candidai.ua",
                "+380501234567",
                "https://storage.candidai.ukma.edu.ua/resumes/taras_cv.pdf"
        );
    }

    public static ApplyForVacancyRequest anInvalidApplyRequest(UUID vacancyId) {
        return new ApplyForVacancyRequest(
                vacancyId,
                SEED_CANDIDATE_ID,
                "",
                "not-an-email",
                "invalid-phone",
                "not-a-url"
        );
    }
}
