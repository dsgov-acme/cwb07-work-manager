package io.nuvalence.workmanager.service.domain.dynamicschema;

/**
 * Interface for possible attribute configurations.
 */
public interface AttributeConfiguration {
    boolean canApplyTo(Class<?> type);
}
