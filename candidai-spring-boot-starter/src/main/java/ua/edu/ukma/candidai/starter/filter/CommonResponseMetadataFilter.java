package ua.edu.ukma.candidai.starter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.edu.ukma.candidai.starter.properties.CandidAiStarterProperties;

import java.io.IOException;

public class CommonResponseMetadataFilter extends OncePerRequestFilter {

    public static final String HEADER_APPLICATION_NAME = "X-Application-Name";
    public static final String HEADER_ENVIRONMENT = "X-Environment";
    public static final String HEADER_RESPONSE_TIME = "X-Response-Time-Millis";

    private final CandidAiStarterProperties properties;

    public CommonResponseMetadataFilter(CandidAiStarterProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        response.setHeader(HEADER_APPLICATION_NAME, properties.response().serviceName());

        if (properties.response().includeEnvironment()) {
            response.setHeader(HEADER_ENVIRONMENT, properties.environment());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (properties.response().includeExecutionTime()) {
                long duration = System.currentTimeMillis() - startTime;
                response.setHeader(HEADER_RESPONSE_TIME, String.valueOf(duration));
            }
        }
    }
}
