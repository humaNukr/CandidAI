package ua.edu.ukma.candidai.vacancy;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SalaryRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSalaryRange {

    String message() default "Minimum salary cannot be greater than maximum salary";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
