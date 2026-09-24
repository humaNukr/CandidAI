package ua.edu.ukma.candidai.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound should return 404 ProblemDetail with correct fields")
    void givenResourceNotFoundException_handleNotFound_shouldReturn404ProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Vacancy not found with id 123");

        ProblemDetail problem = handler.handleNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).isEqualTo("Vacancy not found with id 123");
        assertThat(problem.getType()).isEqualTo(URI.create("https://candidai.ukma.edu.ua/errors/not-found"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleDuplicate should return 409 Conflict ProblemDetail with correct fields")
    void givenDuplicateResourceException_handleDuplicate_shouldReturn409ProblemDetail() {
        DuplicateResourceException ex = new DuplicateResourceException("Candidate already applied");

        ProblemDetail problem = handler.handleDuplicate(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Conflict");
        assertThat(problem.getDetail()).isEqualTo("Candidate already applied");
        assertThat(problem.getType()).isEqualTo(URI.create("https://candidai.ukma.edu.ua/errors/conflict"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleInvalidTransition should return 422 ProblemDetail with correct fields")
    void givenInvalidStateTransitionException_handleInvalidTransition_shouldReturn422ProblemDetail() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException("Cannot transition to OFFER");

        ProblemDetail problem = handler.handleInvalidTransition(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid State Transition");
        assertThat(problem.getDetail()).isEqualTo("Cannot transition to OFFER");
        assertThat(problem.getType()).isEqualTo(
                URI.create("https://candidai.ukma.edu.ua/errors/invalid-state-transition")
        );
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleUnreadableMessage should return 400 ProblemDetail with correct fields")
    void givenHttpMessageNotReadableException_handleUnreadableMessage_shouldReturn400ProblemDetail() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage(new byte[0])
        );

        ProblemDetail problem = handler.handleUnreadableMessage(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("JSON Parsing Error");
        assertThat(problem.getDetail()).isEqualTo("Malformed request body or unknown properties");
        assertThat(problem.getType()).isEqualTo(URI.create("https://candidai.ukma.edu.ua/errors/bad-request"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleValidation should return 400 ProblemDetail with field error map")
    void givenMethodArgumentNotValidException_handleValidation_shouldReturn400WithErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Error");
        assertThat(problem.getDetail()).isEqualTo("Input validation failed");
        assertThat(problem.getType()).isEqualTo(URI.create("https://candidai.ukma.edu.ua/errors/validation"));
        assertThat(problem.getProperties()).containsKey("errors");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("email", "must not be blank");
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String param) {
    }
}
