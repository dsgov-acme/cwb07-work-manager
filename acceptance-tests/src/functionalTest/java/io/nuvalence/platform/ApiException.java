package io.nuvalence.platform;

/**
 * Exception thrown when an error occurs while interacting with the API.
 */
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ApiException(String message) {
        super(message);
    }
}
