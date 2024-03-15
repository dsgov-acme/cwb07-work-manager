package io.nuvalence.workmanager.service.mapper;

import jakarta.validation.constraints.NotNull;

/**
 * Failure when an input string does not match an expected regex pattern.
 */
public class InvalidRegexPatternException extends Exception {
    private static final long serialVersionUID = -6530047628360824627L;

    /**
     * Initializes a new InvalidRegexPatternException with inputString, pattern and description.
     *
     * @param inputString input string
     * @param pattern regex pattern
     * @param description description
     */
    public InvalidRegexPatternException(
            @NotNull final String inputString,
            @NotNull final String pattern,
            @NotNull final String description) {
        super(
                String.format(
                        "Input string of [%s] for the [%s] does not match the pattern: [%s]",
                        inputString, description, pattern));
    }

    /**
     * Initializes a new InvalidRegexPatternException with inputString and pattern.
     *
     * @param inputString input string
     * @param pattern regex pattern
     */
    public InvalidRegexPatternException(
            @NotNull final String inputString, @NotNull final String pattern) {
        super(
                String.format(
                        "Input string of [%s] does not match the pattern: [%s]",
                        inputString, pattern));
    }
}
