package io.nuvalence.workmanager.service.domain;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines an entity that is versioned.
 */
public class VersionedEntity {

    /**
     * Whether the provided name is valid.
     *
     * @param name The name.
     * @return Whether the provided name is valid.
     */
    public static boolean isValidName(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }

        Pattern pattern = Pattern.compile(Constants.VALID_FILE_NAME_REGEX_PATTERN);
        Matcher matcher = pattern.matcher(name);
        return matcher.find();
    }

    /**
     * With a provided name, makes the name valid for export.
     *
     * @param name The name.
     * @return A cleaned-up name.
     */
    public static String makeNameValid(String name) {
        if (isValidName(name)) {
            return name;
        }

        if (StringUtils.isBlank(name)) {
            return "";
        }

        return name.replaceAll(Constants.INVALID_FILE_NAME_REGEX_PATTERN, "");
    }

    /**
     * Versioned entity constants.
     */
    public static class Constants {
        public static final DateTimeFormatter DATE_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        public static final OffsetDateTime END_OF_TIME =
                OffsetDateTime.of(
                        LocalDateTime.parse("9999-12-31T23:59:59", DATE_FORMATTER), ZoneOffset.UTC);
        public static final String VALID_FILE_NAME_REGEX_PATTERN = "^[a-zA-Z0-9-_]*$";
        public static final String INVALID_FILE_NAME_REGEX_PATTERN = "[^a-zA-Z0-9-_]+";

        private Constants() {
            throw new AssertionError(
                    "Utility class should not be instantiated, use the static methods.");
        }
    }
}
