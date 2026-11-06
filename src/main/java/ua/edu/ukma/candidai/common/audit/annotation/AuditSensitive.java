package ua.edu.ukma.candidai.common.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks sensitive fields or parameters that must be masked in audit logs.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditSensitive {

    /**
     * Mask value to replace sensitive content with.
     */
    String mask() default "***MASKED***";
}
