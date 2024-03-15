package io.nuvalence.workmanager.service.config.exceptions;

/**
 * Exception thrown when an error or unexpected behavior occurs calling an external API.
 */
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 7584162493113091609L;

    public ApiException(String message) {
        super(message);
    }
}
