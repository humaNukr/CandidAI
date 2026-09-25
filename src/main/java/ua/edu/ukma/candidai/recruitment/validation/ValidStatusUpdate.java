package ua.edu.ukma.candidai.recruitment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = StatusUpdateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStatusUpdate {

    String message() default "Comment is required when status is REJECTED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
