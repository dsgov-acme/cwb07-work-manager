package io.nuvalence.workmanager.service.domain.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProfileTypeConverterTest {

    private final ProfileTypeConverter converter = new ProfileTypeConverter();

    @Test
    void testConvertToDatabaseColumn() {
        ProfileType profileType = ProfileType.EMPLOYER;
        String result = converter.convertToDatabaseColumn(profileType);
        assertEquals("EMPLOYER", result);
    }

    @Test
    void testConvertToDatabaseColumn_NullInput() {
        String result = converter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    void testConvertToEntityAttribute() {
        String profileTypeString = "EMPLOYER";
        ProfileType result = converter.convertToEntityAttribute(profileTypeString);
        assertEquals(ProfileType.EMPLOYER, result);
    }

    @Test
    void testConvertToEntityAttribute_NullInput() {
        ProfileType result = converter.convertToEntityAttribute(null);
        assertNull(result);
    }
}
