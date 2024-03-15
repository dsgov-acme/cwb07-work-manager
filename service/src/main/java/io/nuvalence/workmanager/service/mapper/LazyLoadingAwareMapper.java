package io.nuvalence.workmanager.service.mapper;

import org.hibernate.Hibernate;
import org.mapstruct.Condition;

import java.util.Collection;

/**
 * Decorator interface that adds a mapstruct condition to avoid attmepting to map unloaded hibernate collections.
 */
public interface LazyLoadingAwareMapper {

    /**
     * Returns false (causing the mapping to skip the collection) if the collection is an uninitalized lazy loaded
     * hibernate collection.
     *
     * @param sourceCollection collection to inspect
     * @return false if the collection is an uninitalized lazy loaded hibernate collection
     */
    @Condition
    default boolean isNotLazyLoaded(Collection<?> sourceCollection) {
        // Case: Source field in domain object is lazy: Skip mapping
        // Continue Mapping
        return Hibernate.isInitialized(sourceCollection);

        // Skip mapping
    }
}
