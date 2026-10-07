package ua.edu.ukma.candidai.starter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ua.edu.ukma.candidai.starter.properties.CandidAiStarterProperties;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.starter.filter.CommonResponseMetadataFilterTestResources.*;

class CommonResponseMetadataFilterTest {

    private final FilterChain filterChain = mock(FilterChain.class);

    @Test
    @DisplayName("doFilterInternal - should attach all metadata headers when enabled")
    void givenEnabledProperties_doFilterInternal_shouldAttachHeaders() throws ServletException, IOException {
        CandidAiStarterProperties properties = createEnabledProperties();
        CommonResponseMetadataFilter filter = new CommonResponseMetadataFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(HEADER_APPLICATION_NAME)).isEqualTo(TEST_SERVICE_NAME);
        assertThat(response.getHeader(HEADER_ENVIRONMENT)).isEqualTo(TEST_ENVIRONMENT);
        assertThat(response.getHeader(HEADER_RESPONSE_TIME)).isNotBlank();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - should not attach response time header when execution time disabled")
    void givenExecutionTimeDisabled_doFilterInternal_shouldNotAttachTimingHeader() throws ServletException, IOException {
        CandidAiStarterProperties properties = createExecutionTimeDisabledProperties();
        CommonResponseMetadataFilter filter = new CommonResponseMetadataFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(HEADER_APPLICATION_NAME)).isEqualTo(TEST_SERVICE_NAME);
        assertThat(response.getHeader(HEADER_ENVIRONMENT)).isEqualTo(TEST_ENVIRONMENT);
        assertThat(response.getHeader(HEADER_RESPONSE_TIME)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - should not attach environment header when environment disabled")
    void givenEnvironmentDisabled_doFilterInternal_shouldNotAttachEnvironmentHeader() throws ServletException, IOException {
        CandidAiStarterProperties properties = createEnvironmentDisabledProperties();
        CommonResponseMetadataFilter filter = new CommonResponseMetadataFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(HEADER_APPLICATION_NAME)).isEqualTo(TEST_SERVICE_NAME);
        assertThat(response.getHeader(HEADER_ENVIRONMENT)).isNull();
        assertThat(response.getHeader(HEADER_RESPONSE_TIME)).isNotBlank();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - should attach timing header in finally block when filterChain throws")
    void givenFilterChainThrowsException_doFilterInternal_shouldAttachTimingHeaderInFinally() throws ServletException, IOException {
        CandidAiStarterProperties properties = createEnabledProperties();
        CommonResponseMetadataFilter filter = new CommonResponseMetadataFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new ServletException("Chain execution failed")).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("Chain execution failed");

        assertThat(response.getHeader(HEADER_RESPONSE_TIME)).isNotBlank();
    }
}
