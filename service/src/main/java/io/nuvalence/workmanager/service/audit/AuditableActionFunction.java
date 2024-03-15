package io.nuvalence.workmanager.service.audit;

/**
 * Function interface for transformative actions on a subject.
 *
 * @param <T> Type of subject
 */
@FunctionalInterface
public interface AuditableActionFunction<T> {
    T execute(T input) throws Exception;
}
