package io.nuvalence.workmanager.service.domain.dynamicschema;

import java.util.Arrays;
import java.util.Objects;

/**
 * Functions for use in computed attribute expressions.
 */
public class ComputedAttributeFunctions {

    private ComputedAttributeFunctions() {
        throw new AssertionError(
                "Utility class should not be instantiated, use the static methods.");
    }

    /**
     * Concatenate strings, ignoring nulls, using a specified delimiter.
     * <p>
     *     Example expression: #concat(", ", "a", null, "b") returns "a, b"
     * </p>
     *
     * @param delimiter delimiter to use between strings
     * @param args strings to concatenate
     * @return concatenated string
     */
    public static String concat(String delimiter, String... args) {
        return String.join(
                delimiter, Arrays.stream(args).filter(Objects::nonNull).toArray(String[]::new));
    }
}
