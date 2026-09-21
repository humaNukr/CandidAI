package ua.edu.ukma.candidai.recruitment.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ua.edu.ukma.candidai.common.util.ValidationUtils;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;

public class StatusUpdateValidator implements ConstraintValidator<ValidStatusUpdate, UpdateApplicationStatusRequest> {

    @Override
    public boolean isValid(UpdateApplicationStatusRequest request, ConstraintValidatorContext context) {
        if (request == null || request.status() == null) {
            return true;
        }

        if (request.status() == ApplicationStatus.REJECTED
                && (request.comment() == null || request.comment().trim().isEmpty())) {
            ValidationUtils.addViolation(
                    "Comment is required when status is REJECTED",
                    "comment",
                    context
            );
            return false;
        }

        return true;
    }
}
