package ua.edu.ukma.candidai.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    @DisplayName("shouldNotFilterAsyncDispatch - should return false to support async request dispatching")
    void givenTraceIdFilter_shouldNotFilterAsyncDispatch_shouldReturnFalse() {
        assertThat(filter.shouldNotFilterAsyncDispatch()).isFalse();
    }

    @Test
    @DisplayName("doFilter - should generate new UUID when header is missing and clear MDC in finally")
    void givenNoTraceIdHeader_doFilter_shouldGenerateUuidAndSetHeaderAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotNull().isNotBlank();
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter - should preserve traceId when X-Trace-Id header is provided")
    void givenExistingTraceIdHeader_doFilter_shouldPreserveTraceIdAndSetHeaderAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String customTraceId = "candidai-custom-trace-uuid-101";
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, customTraceId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(customTraceId);
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter - should generate new UUID when header is blank")
    void givenBlankTraceIdHeader_doFilter_shouldGenerateNewUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotNull().isNotBlank();
        assertThat(traceId.trim()).isNotEmpty();
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter - should sanitize CRLF and control characters to prevent log and header injection")
    void givenCrlfInTraceIdHeader_doFilter_shouldSanitizeTraceIdAndPreventInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String dangerousHeader = "trace-123\r\nInjected-Header: evil\nLog-Injection";
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, dangerousHeader);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotNull().doesNotContain("\r", "\n", " ", ":");
        assertThat(traceId).isEqualTo("trace-123Injected-HeaderevilLog-Injection");
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter - should fallback to generated UUID if header contains only invalid characters")
    void givenOnlyInvalidCharactersInHeader_doFilter_shouldFallbackToGeneratedUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "\r\n\t :;!?@#");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotNull().isNotBlank();
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter - should use X-Request-ID header when X-Trace-Id is not present")
    void givenAltTraceIdHeader_doFilter_shouldUseAltHeaderAndSetHeaderAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String altTraceId = "alt-request-id-777";
        request.addHeader(TraceIdFilter.ALT_TRACE_ID_HEADER, altTraceId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(altTraceId);
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter - should ensure MDC is cleared even when filter chain throws an exception")
    void givenFilterChainThrowsException_doFilter_shouldCleanUpMdcInFinally() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNotNull();
            throw new IllegalStateException("Simulated processing error");
        })).isInstanceOf(IllegalStateException.class).hasMessage("Simulated processing error");

        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }
}
