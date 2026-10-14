package ua.edu.ukma.candidai.common.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.common.security.SecurityTestResources.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtValidatorService jwtValidatorService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Test
    @DisplayName("doFilterInternal should continue chain when Authorization header is missing")
    void givenNoAuthHeader_doFilterInternal_shouldProceedFilterChain() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtValidatorService);
    }

    @Test
    @DisplayName("doFilterInternal should authenticate user when valid Bearer token is provided")
    void givenValidBearerToken_doFilterInternal_shouldSetAuthentication() throws Exception {
        String token = "valid-token";
        UserDetails userDetails = aTestUserDetails();

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtValidatorService.extractUsername(token)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtValidatorService.isTokenValid(token, userDetails)).thenReturn(true);

        try {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(TEST_EMAIL);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("doFilterInternal should invoke entrypoint and clear context when token is invalid")
    void givenInvalidToken_doFilterInternal_shouldInvokeEntryPoint() throws Exception {
        String token = "invalid-token";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtValidatorService.extractUsername(token)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(authenticationEntryPoint).commence(eq(request), eq(response), any());
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
