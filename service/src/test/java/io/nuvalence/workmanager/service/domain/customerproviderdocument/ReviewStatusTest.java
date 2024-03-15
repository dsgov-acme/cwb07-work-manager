package io.nuvalence.workmanager.service.domain.customerproviderdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import org.junit.jupiter.api.Test;

class ReviewStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetValue() {
        ReviewStatus status = ReviewStatus.ACCEPTED;
        String result = status.getValue();
        assertEquals("ACCEPTED", result);
    }

    @Test
    void testFromString_ValidValue() {
        String value = "REJECTED";
        ReviewStatus result = ReviewStatus.fromValue(value);
        assertEquals(ReviewStatus.REJECTED, result);
    }

    @Test
    void testFromString_InvalidValue() {
        String value = "INVALID";
        assertThrows(IllegalArgumentException.class, () -> ReviewStatus.fromValue(value));
    }

    @Test
    void testJsonSerialization() throws Exception {
        ReviewStatus status = ReviewStatus.NEW;
        String json = objectMapper.writeValueAsString(status);
        assertEquals("\"NEW\"", json);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "\"ACCEPTED\"";
        ReviewStatus result = objectMapper.readValue(json, ReviewStatus.class);
        assertEquals(ReviewStatus.ACCEPTED, result);
    }

    @Test
    void testGetLabel() {
        ReviewStatus status = ReviewStatus.ACCEPTED;
        String result = status.getLabel();
        assertEquals("Accepted", result);
    }

    @Test
    void testIsHiddenFromApi() {
        ReviewStatus status = ReviewStatus.ACCEPTED;
        boolean result = status.isHiddenFromApi();
        assertEquals(false, result);

        status = ReviewStatus.PENDING;
        result = status.isHiddenFromApi();
        assertEquals(true, result);
    }
}
