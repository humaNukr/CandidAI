package ua.edu.ukma.candidai.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class WebExceptionAdviceTest {

    private final WebExceptionAdvice advice = new WebExceptionAdvice();

    @Test
    @DisplayName("handleNotFound - should populate model and return error/404 view")
    void handleNotFound_shouldReturn404View() {
        Model model = new ConcurrentModel();
        ResourceNotFoundException ex = new ResourceNotFoundException("Vacancy not found");

        String view = advice.handleNotFound(ex, model);

        assertThat(view).isEqualTo("error/404");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Vacancy not found");
    }
}
