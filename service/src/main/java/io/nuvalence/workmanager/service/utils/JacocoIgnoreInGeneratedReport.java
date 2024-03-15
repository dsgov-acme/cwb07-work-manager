package io.nuvalence.workmanager.service.utils;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Documents a class, method or constructor to inform jacoco to ignore for purposes of calculating code coverage.
 * <p/>
 * Jacoco will ignore any class, method or constructor annotated with an annotation with 'Generated' in the simple
 * name, thus the strange name here.
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, CONSTRUCTOR})
public @interface JacocoIgnoreInGeneratedReport {
    /**
     * Documented reason justifying exclusion.
     *
     * @return reason as String
     */
    String reason();
}
