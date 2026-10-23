package ua.edu.ukma.candidai.vacancy.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

/**
 * Validates that a comma-separated string contains at least one non-blank skill item.
 */
public class SkillsListValidator implements ConstraintValidator<ValidSkillsList, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotBlank handle null/blank check
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .anyMatch(skill -> !skill.isBlank());
    }
}
