package io.nuvalence.workmanager.service.domain.customerproviderdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonConverter;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import org.junit.jupiter.api.Test;

class RejectionReasonConverterTest {

    private final RejectionReasonConverter converter = new RejectionReasonConverter();

    @Test
    void testConvertToDatabaseColumn() {
        RejectionReasonType rejectionReason = RejectionReasonType.DOES_NOT_SATISFY_REQUIREMENTS;
        String result = converter.convertToDatabaseColumn(rejectionReason);
        assertEquals("DOES_NOT_SATISFY_REQUIREMENTS", result);
    }

    @Test
    void testConvertToDatabaseColumn_NullInput() {
        String result = converter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    void testConvertToEntityAttribute() {
        String rejectionReasonString = "DOES_NOT_SATISFY_REQUIREMENTS";
        RejectionReasonType result = converter.convertToEntityAttribute(rejectionReasonString);
        assertEquals(RejectionReasonType.DOES_NOT_SATISFY_REQUIREMENTS, result);
    }

    @Test
    void testConvertToEntityAttribute_NullInput() {
        RejectionReasonType result = converter.convertToEntityAttribute(null);
        assertNull(result);
    }
}
