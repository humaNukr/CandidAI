package ua.edu.ukma.candidai.vacancy.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a comma-separated skills string contains at least one non-blank skill.
 */
@Documented
@Constraint(validatedBy = SkillsListValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSkillsList {

    String message() default "{vacancies.form.requiredSkills.required}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
