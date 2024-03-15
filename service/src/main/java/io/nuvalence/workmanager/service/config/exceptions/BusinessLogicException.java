package io.nuvalence.workmanager.service.config.exceptions;

/**
 * Custom exception for business logic issues.
 */
public class BusinessLogicException extends RuntimeException {
    private static final long serialVersionUID = 6730229792768796235L;

    public BusinessLogicException(String message) {
        super(message);
    }
}
