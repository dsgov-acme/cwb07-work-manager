package io.nuvalence.workmanager.service.domain.customerproviderdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import org.junit.jupiter.api.Test;

class RejectionReasonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetValue() {
        RejectionReasonType reason = RejectionReasonType.INCORRECT_TYPE;
        String result = reason.getValue();
        assertEquals("INCORRECT_TYPE", result);
    }

    @Test
    void testFromString_ValidValue() {
        String value = "DOES_NOT_SATISFY_REQUIREMENTS";
        RejectionReasonType result = RejectionReasonType.fromValue(value);
        assertEquals(RejectionReasonType.DOES_NOT_SATISFY_REQUIREMENTS, result);
    }

    @Test
    void testFromString_InvalidValue() {
        String value = "INVALID";
        assertThrows(IllegalArgumentException.class, () -> RejectionReasonType.fromValue(value));
    }

    @Test
    void testJsonSerialization() throws Exception {
        RejectionReasonType reason = RejectionReasonType.POOR_QUALITY;
        String json = objectMapper.writeValueAsString(reason);
        assertEquals("\"POOR_QUALITY\"", json);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "\"SUSPECTED_FRAUD\"";
        RejectionReasonType result = objectMapper.readValue(json, RejectionReasonType.class);
        assertEquals(RejectionReasonType.SUSPECTED_FRAUD, result);
    }

    @Test
    void testGetLabel() {
        RejectionReasonType rejectionReasonType = RejectionReasonType.POOR_QUALITY;
        String result = rejectionReasonType.getLabel();
        assertEquals("Poor Quality", result);
    }
}
