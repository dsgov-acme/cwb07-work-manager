package io.nuvalence.workmanager.service.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TransactionPriorityConverterTest {

    private final TransactionPriorityConverter converter = new TransactionPriorityConverter();

    @Test
    void testConvertToDatabaseColumn() {
        TransactionPriority priority = TransactionPriority.MEDIUM;
        Integer result = converter.convertToDatabaseColumn(priority);
        assertEquals(20, result);
    }

    @Test
    void testConvertToDatabaseColumn_NullInput() {
        Integer result = converter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    void testConvertToEntityAttribute() {
        Integer integerPriority = 20;
        TransactionPriority result = converter.convertToEntityAttribute(integerPriority);
        assertEquals(TransactionPriority.MEDIUM, result);
    }

    @Test
    void testConvertToEntityAttribute_NullInput() {
        TransactionPriority result = converter.convertToEntityAttribute(null);
        assertNull(result);
    }
}
