package ua.edu.ukma.candidai.common.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks business methods for automated AOP auditing.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {

    /**
     * Unique action identifier (e.g. "CREATE_VACANCY", "APPLY_VACANCY").
     */
    String action();

    /**
     * Whether method parameters should be serialized into the audit log.
     */
    boolean logParameters() default true;

    /**
     * Human-readable description of the operation.
     */
    String description() default "";
}
