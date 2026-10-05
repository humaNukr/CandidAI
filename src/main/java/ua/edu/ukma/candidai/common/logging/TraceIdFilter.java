package ua.edu.ukma.candidai.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String ALT_TRACE_ID_HEADER = "X-Request-ID";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    private static final int MAX_TRACE_ID_LENGTH = 64;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            log.debug("Processing HTTP request [{} {}] with traceId={}",
                    request.getMethod(), request.getRequestURI(), traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = sanitizeTraceId(request.getHeader(TRACE_ID_HEADER));
        if (traceId != null) {
            return traceId;
        }

        String altTraceId = sanitizeTraceId(request.getHeader(ALT_TRACE_ID_HEADER));
        if (altTraceId != null) {
            return altTraceId;
        }

        return UUID.randomUUID().toString();
    }

    private String sanitizeTraceId(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            return null;
        }

        String sanitized = rawHeader.replaceAll("[^a-zA-Z0-9_-]", "").trim();
        if (sanitized.isEmpty()) {
            return null;
        }

        if (sanitized.length() > MAX_TRACE_ID_LENGTH) {
            return sanitized.substring(0, MAX_TRACE_ID_LENGTH);
        }

        return sanitized;
    }
}
