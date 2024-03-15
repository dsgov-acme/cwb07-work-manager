package io.nuvalence.workmanager.service.config.exceptions;

import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import lombok.Getter;

/**
 * Exception thrown when there is Nuvalence formIO validation error.
 */
public class NuvalenceFormioValidationException extends RuntimeException {
    private static final long serialVersionUID = 4517826618555350016L;

    @Getter private final NuvalenceFormioValidationExMessage formioValidationErrors;

    public NuvalenceFormioValidationException(
            NuvalenceFormioValidationExMessage formioValidationErrors) {
        this.formioValidationErrors = formioValidationErrors;
    }

    public NuvalenceFormioValidationExMessage getErrorMessages() {
        return formioValidationErrors;
    }
}
