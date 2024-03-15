package io.nuvalence.workmanager.service.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

class VersionedEntityTest {
    @Test
    void testIsValidName_ValidName_ReturnsTrue() {
        String validName = "example_file_123";
        boolean isValid = VersionedEntity.isValidName(validName);
        Assertions.assertTrue(isValid);
    }

    @Test
    void testIsValidName_InvalidName_ReturnsFalse() {
        String invalidName = "invalid@file";
        boolean isValid = VersionedEntity.isValidName(invalidName);
        Assertions.assertFalse(isValid);
    }

    @Test
    void testIsValidName_BlankName_ReturnsFalse() {
        String blankName = "";
        boolean isValid = VersionedEntity.isValidName(blankName);
        Assertions.assertFalse(isValid);
    }

    @Test
    void testMakeNameValid_InvalidName_ReturnsCleanedName() {
        String invalidName = "inva#lid@file";
        String cleanedName = VersionedEntity.makeNameValid(invalidName);
        Assertions.assertEquals("invalidfile", cleanedName);
    }

    @Test
    void testMakeNameValid_ValidName_ReturnsSameName() {
        String validName = "example_file_123";
        String cleanedName = VersionedEntity.makeNameValid(validName);
        Assertions.assertEquals(validName, cleanedName);
    }

    @Test
    void testMakeNameValid_BlankName_ReturnsEmptyString() {
        String blankName = "";
        String cleanedName = VersionedEntity.makeNameValid(blankName);
        Assertions.assertEquals("", cleanedName);
    }

    @Test
    void testDateFormatPattern_ValidPattern() {
        String expectedPattern = "yyyy-MM-dd'T'HH:mm:ss";
        DateTimeFormatter expectedFormatter =
                new DateTimeFormatterBuilder().appendPattern(expectedPattern).toFormatter();

        DateTimeFormatter actualFormatter = VersionedEntity.Constants.DATE_FORMATTER;

        Assertions.assertEquals(expectedFormatter.toString(), actualFormatter.toString());
    }

    @Test
    void testEndOfTime_ValidDateTime() {
        String expectedDateTime = "9999-12-31T23:59:59Z";
        String actualDateTime =
                VersionedEntity.Constants.END_OF_TIME.format(
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Assertions.assertEquals(expectedDateTime, actualDateTime);
    }

    @Test
    void testValidFileNameRegexPattern_ValidPattern() {
        String validPattern = "^[a-zA-Z0-9-_]*$";
        String actualPattern = VersionedEntity.Constants.VALID_FILE_NAME_REGEX_PATTERN;
        Assertions.assertEquals(validPattern, actualPattern);
    }

    @Test
    void testInvalidFileNameRegexPattern_ValidPattern() {
        String invalidPattern = "[^a-zA-Z0-9-_]+";
        String actualPattern = VersionedEntity.Constants.INVALID_FILE_NAME_REGEX_PATTERN;
        Assertions.assertEquals(invalidPattern, actualPattern);
    }
}
