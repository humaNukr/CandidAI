package ua.edu.ukma.candidai.vacancy.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.validation.HasSalaryRange;
import ua.edu.ukma.candidai.vacancy.validation.ValidSalaryRange;
import ua.edu.ukma.candidai.vacancy.validation.ValidSkillsList;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@ValidSalaryRange
public record CreateVacancyForm(
        @NotBlank(message = "{vacancies.form.title.required}")
        @Size(min = 3, max = 150, message = "{vacancies.form.title.size}")
        String title,

        @NotNull(message = "{vacancies.form.category.required}")
        JobCategory category,

        @NotBlank(message = "{vacancies.form.specialization.required}")
        @Size(min = 2, max = 100, message = "{vacancies.form.specialization.size}")
        String specialization,

        @NotBlank(message = "{vacancies.form.seniorityLevel.required}")
        @Size(min = 2, max = 50, message = "{vacancies.form.seniorityLevel.size}")
        String seniorityLevel,

        @NotNull(message = "{vacancies.form.minYearsOfExperience.required}")
        @Min(value = 0, message = "{vacancies.form.minYearsOfExperience.min}")
        @Max(value = 50, message = "{vacancies.form.minYearsOfExperience.max}")
        Integer minYearsOfExperience,

        @NotBlank(message = "{vacancies.form.description.required}")
        @Size(min = 10, max = 2000, message = "{vacancies.form.description.size}")
        String description,

        @NotBlank(message = "{vacancies.form.requiredSkills.required}")
        @ValidSkillsList(message = "{vacancies.form.requiredSkills.required}")
        String requiredSkills,

        String preferredSkills,

        @NotNull(message = "{vacancies.form.minEnglishLevel.required}")
        EnglishLevel minEnglishLevel,

        @Positive(message = "{vacancies.form.salaryMin.positive}")
        BigDecimal salaryMin,

        @Positive(message = "{vacancies.form.salaryMax.positive}")
        BigDecimal salaryMax,

        @NotBlank(message = "{vacancies.form.currency.required}")
        @Size(min = 3, max = 3, message = "{vacancies.form.currency.size}")
        String currency,

        @NotNull(message = "{vacancies.form.employmentType.required}")
        EmploymentType employmentType,

        @NotNull(message = "{vacancies.form.locationType.required}")
        LocationType locationType,

        @Size(max = 150, message = "{vacancies.form.location.size}")
        String location
) implements HasSalaryRange {

    public static final UUID DEFAULT_COMPANY_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");

    public static CreateVacancyForm empty() {
        return new CreateVacancyForm(
                "",
                JobCategory.ENGINEERING,
                "",
                "",
                1,
                "",
                "",
                "",
                EnglishLevel.B2,
                null,
                null,
                "USD",
                EmploymentType.FULL_TIME,
                LocationType.REMOTE,
                ""
        );
    }

    public CreateVacancyRequest toRequest(UUID authorId) {
        List<String> required = parseSkills(requiredSkills);
        List<String> preferred = parseSkills(preferredSkills);

        return new CreateVacancyRequest(
                authorId,
                authorId,
                DEFAULT_COMPANY_ID,
                VacancyStatus.OPEN,
                title,
                category,
                specialization,
                seniorityLevel,
                minYearsOfExperience,
                description,
                required,
                preferred,
                minEnglishLevel,
                salaryMin,
                salaryMax,
                currency,
                employmentType,
                locationType,
                location,
                null
        );
    }

    private static List<String> parseSkills(String skillsStr) {
        if (skillsStr == null || skillsStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(skillsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
