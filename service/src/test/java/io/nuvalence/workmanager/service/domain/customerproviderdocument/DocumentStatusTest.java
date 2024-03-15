package io.nuvalence.workmanager.service.domain.customerproviderdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.DocumentStatus;
import org.junit.jupiter.api.Test;

class DocumentStatusTest {

    @Test
    void testFromText_WhenValidTextProvided() {
        String text = "Accepted";

        DocumentStatus status = DocumentStatus.fromText(text);

        assertNotNull(status);
        assertEquals(DocumentStatus.ACCEPTED, status);
    }

    @Test
    void testFromText_WhenInvalidTextProvided() {
        String text = "Invalid-status";

        DocumentStatus status = DocumentStatus.fromText(text);

        assertNotNull(status);
        assertEquals(DocumentStatus.PENDING, status);
    }
}
