package ua.edu.ukma.candidai.common.security;

import org.springframework.security.core.context.SecurityContext;

import java.util.concurrent.Callable;

public final class SecurityScopeRunner {

    public static final ScopedValue<SecurityContext> SECURITY_CONTEXT = ScopedValue.newInstance();

    private SecurityScopeRunner() {
    }

    public static <T> T callWithContext(SecurityContext context, Callable<T> task) throws Exception {
        return ScopedValue.where(SECURITY_CONTEXT, context).call(task::call);
    }

    public static void runWithContext(SecurityContext context, Runnable task) {
        ScopedValue.where(SECURITY_CONTEXT, context).run(task);
    }
}
