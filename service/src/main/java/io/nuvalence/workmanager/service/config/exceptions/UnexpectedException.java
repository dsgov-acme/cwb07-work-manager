package io.nuvalence.workmanager.service.config.exceptions;

/**
 * Custom exception for business logic issues.
 */
public class UnexpectedException extends RuntimeException {
    private static final long serialVersionUID = 8726229712858796681L;

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedException(Throwable cause) {
        super(cause);
    }
}
