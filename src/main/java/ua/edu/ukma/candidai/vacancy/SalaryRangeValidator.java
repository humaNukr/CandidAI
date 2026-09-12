package ua.edu.ukma.candidai.vacancy;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ua.edu.ukma.candidai.common.util.ValidationUtils;

import java.math.BigDecimal;

public class SalaryRangeValidator implements ConstraintValidator<ValidSalaryRange, CreateVacancyRequest> {

    @Override
    public boolean isValid(CreateVacancyRequest request, ConstraintValidatorContext context) {
        BigDecimal salaryMin = request.salaryMin();
        BigDecimal salaryMax = request.salaryMax();
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
