package io.nuvalence.workmanager.service.domain.customerproviderdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatusConverter;
import org.junit.jupiter.api.Test;

class ReviewStatusConverterTest {

    private final ReviewStatusConverter converter = new ReviewStatusConverter();

    @Test
    void testConvertToDatabaseColumn() {
        ReviewStatus status = ReviewStatus.NEW;
        String result = converter.convertToDatabaseColumn(status);
        assertEquals("NEW", result);
    }

    @Test
    void testConvertToDatabaseColumn_NullInput() {
        String result = converter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    void testConvertToEntityAttribute() {
        String statusString = "NEW";
        ReviewStatus result = converter.convertToEntityAttribute(statusString);
        assertEquals(ReviewStatus.NEW, result);
    }

    @Test
    void testConvertToEntityAttribute_NullInput() {
        ReviewStatus result = converter.convertToEntityAttribute(null);
        assertNull(result);
    }
}
