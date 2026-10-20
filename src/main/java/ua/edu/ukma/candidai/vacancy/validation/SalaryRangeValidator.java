package ua.edu.ukma.candidai.vacancy.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ua.edu.ukma.candidai.common.util.ValidationUtils;

import java.math.BigDecimal;

public class SalaryRangeValidator implements ConstraintValidator<ValidSalaryRange, HasSalaryRange> {

    @Override
    public boolean isValid(HasSalaryRange value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BigDecimal salaryMin = value.salaryMin();
        BigDecimal salaryMax = value.salaryMax();
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            ValidationUtils.addViolation(
                    "Minimum salary cannot be greater than maximum salary",
                    "salaryMin",
                    context
            );
            return false;
        }
        return true;
    }
}
