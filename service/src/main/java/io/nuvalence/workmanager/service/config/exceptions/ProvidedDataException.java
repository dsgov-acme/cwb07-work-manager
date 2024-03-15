package io.nuvalence.workmanager.service.config.exceptions;

/**
 * Exception thrown when there is an error in the provided data with which some logic is being executed.
 */
public class ProvidedDataException extends RuntimeException {
    private static final long serialVersionUID = 4517826618555350016L;

    public ProvidedDataException(String message) {
        super(message);
    }
}
