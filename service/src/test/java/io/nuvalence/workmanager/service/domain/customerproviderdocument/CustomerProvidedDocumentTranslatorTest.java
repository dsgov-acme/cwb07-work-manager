package io.nuvalence.workmanager.service.domain.customerproviderdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocumentTranslator;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReason;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class CustomerProvidedDocumentTranslatorTest {

    @Test
    void testTranslate_CustomerProvidedDocument() {
        UUID id = UUID.randomUUID();
        CustomerProvidedDocument document = new CustomerProvidedDocument();
        document.setReviewStatus(ReviewStatus.NEW);
        document.setTransactionId(id);
        document.setDataPath("/documents");
        document.setActive(true);
        document.setClassifier("Sensitive");
        document.setRejectionReasons(
                List.of(
                        RejectionReason.builder()
                                .rejectionReasonValue(
                                        RejectionReasonType.DOES_NOT_SATISFY_REQUIREMENTS)
                                .customerProvidedDocument(document)
                                .build()));

        CustomerProvidedDocumentTranslator translator = new CustomerProvidedDocumentTranslator();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("id", null);
        expectedResult.put("reviewStatus", "NEW");
        expectedResult.put("rejectionReasons", "[DOES_NOT_SATISFY_REQUIREMENTS]");
        expectedResult.put("transactionId", id.toString());
        expectedResult.put("dataPath", "/documents");
        expectedResult.put("active", true);
        expectedResult.put("classifier", "Sensitive");
        Object result = translator.translate(document);

        assertEquals(expectedResult.toString(), result.toString());
    }

    @Test
    void testTranslate_NonCustomerProvidedDocument() {
        Object resource = new Object();

        CustomerProvidedDocumentTranslator translator = new CustomerProvidedDocumentTranslator();
        Object result = translator.translate(resource);

        assertEquals(resource, result);
    }
}
