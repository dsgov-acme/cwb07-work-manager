package io.nuvalence.workmanager.service.domain.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProfileAccessLevelConverterTest {

    private final ProfileAccessLevelConverter converter = new ProfileAccessLevelConverter();

    @Test
    void convertToDatabaseColumn() {
        assertEquals("ADMIN", converter.convertToDatabaseColumn(ProfileAccessLevel.ADMIN));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute() {
        assertEquals(ProfileAccessLevel.ADMIN, converter.convertToEntityAttribute("ADMIN"));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
