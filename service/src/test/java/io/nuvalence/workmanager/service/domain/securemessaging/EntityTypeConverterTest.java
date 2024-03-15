package io.nuvalence.workmanager.service.domain.securemessaging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;

class EntityTypeConverterTest {

    private final AttributeConverter<EntityType, String> converter = new EntityTypeConverter();

    @Test
    void testConvertToDatabaseColumn() {
        assertEquals("TRANSACTION", converter.convertToDatabaseColumn(EntityType.TRANSACTION));
        assertEquals("EMPLOYER", converter.convertToDatabaseColumn(EntityType.EMPLOYER));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToEntityAttribute() {
        assertEquals(EntityType.TRANSACTION, converter.convertToEntityAttribute("TRANSACTION"));
        assertEquals(EntityType.EMPLOYER, converter.convertToEntityAttribute("EMPLOYER"));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void testConvertToEntityAttribute_InvalidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("INVALID_VALUE"));
    }
}
