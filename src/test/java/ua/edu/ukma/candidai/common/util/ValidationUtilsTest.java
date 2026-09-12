package ua.edu.ukma.candidai.common.util;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationUtilsTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @Test
    @DisplayName("addViolation should configure property node and add constraint violation")
    void shouldAddViolationCorrectly() {
        when(context.buildConstraintViolationWithTemplate("Custom error message")).thenReturn(builder);
        when(builder.addPropertyNode("customProperty")).thenReturn(nodeBuilder);

        ValidationUtils.addViolation("Custom error message", "customProperty", context);

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Custom error message");
        verify(builder).addPropertyNode("customProperty");
        verify(nodeBuilder).addConstraintViolation();
    }
}
