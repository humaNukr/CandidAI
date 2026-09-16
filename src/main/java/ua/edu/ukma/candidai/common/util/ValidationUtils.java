package ua.edu.ukma.candidai.common.util;

import jakarta.validation.ConstraintValidatorContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationUtils {

    public static void addViolation(String message, String property, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
