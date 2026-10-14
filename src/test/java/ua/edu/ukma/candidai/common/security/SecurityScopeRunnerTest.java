package ua.edu.ukma.candidai.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.security.SecurityTestResources.*;

class SecurityScopeRunnerTest {

    @Test
    @DisplayName("callWithContext should execute task with bound SecurityContext in scope")
    void givenSecurityContext_callWithContext_shouldExecuteTaskWithScopedContext() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_EMAIL, null));

        String principal = SecurityScopeRunner.callWithContext(context, () -> {
            SecurityContext scoped = SecurityScopeRunner.SECURITY_CONTEXT.get();
            return scoped.getAuthentication().getName();
        });

        assertThat(principal).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("runWithContext should execute runnable with bound SecurityContext in scope")
    void givenSecurityContext_runWithContext_shouldExecuteRunnableWithScopedContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_EMAIL, null));

        final String[] result = new String[1];
        SecurityScopeRunner.runWithContext(context, () -> {
            result[0] = SecurityScopeRunner.SECURITY_CONTEXT.get().getAuthentication().getName();
        });

        assertThat(result[0]).isEqualTo(TEST_EMAIL);
    }
}
