package io.nuvalence.workmanager.service.config.exceptions;

/**
 * Custom exception for business logic issues.
 */
public class FileReadException extends RuntimeException {
    private static final long serialVersionUID = 6730229792858796235L;

    public FileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
