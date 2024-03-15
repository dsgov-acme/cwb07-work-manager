package io.nuvalence.workmanager.service.domain.customerproviderdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.DocumentRejectionReason;
import org.junit.jupiter.api.Test;

class DocumentRejectReasonTest {
    @Test
    void testFromText_WhenValidTextProvided() {
        String text = "Blurry-document";

        DocumentRejectionReason reason = DocumentRejectionReason.fromText(text);

        assertNotNull(reason);
        assertEquals(DocumentRejectionReason.BLURRY_DOCUMENT, reason);
    }

    @Test
    void testFromText_WhenInvalidTextProvided() {
        String text = "Invalid-reason";

        DocumentRejectionReason reason = DocumentRejectionReason.fromText(text);

        assertNull(reason);
    }
}
