package io.nuvalence.workmanager.service.mapper;

import jakarta.validation.constraints.NotNull;

/**
 * Failure when a referenced schema cannot be retrieved.
 */
public class MissingSchemaException extends Exception {
    private static final long serialVersionUID = 1187973706938428981L;

    public MissingSchemaException(@NotNull final String schema) {
        super(String.format("Schema with name [%s] not found.", schema));
    }
}
