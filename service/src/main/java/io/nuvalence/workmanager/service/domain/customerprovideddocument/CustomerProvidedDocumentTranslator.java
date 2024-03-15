package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import io.nuvalence.auth.access.cerbos.AccessResourceTranslator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translator of Customer Provided Documents into Map.
 */
@Component
public class CustomerProvidedDocumentTranslator implements AccessResourceTranslator {

    @Override
    public Object translate(Object resource) {
        if (resource instanceof CustomerProvidedDocument customerProvidedDocument) {
            return convertToMap(customerProvidedDocument);
        }
        return resource;
    }

    private static Map<String, Object> convertToMap(CustomerProvidedDocument document) {
        Map<String, Object> map = new HashMap<>();

        // Convert each field of the CustomerProvidedDocument instance to an appropriate
        // representation and add it to the map.
        map.put("id", document.getId());
        map.put("reviewStatus", document.getReviewStatus());
        List<String> rejectionReasonsAsStrings =
                document.getRejectionReasons().stream()
                        .map(
                                rejectionReason ->
                                        rejectionReason.getRejectionReasonValue().getValue())
                        .toList();
        map.put("rejectionReasons", rejectionReasonsAsStrings);
        map.put("transactionId", document.getTransactionId());
        map.put("dataPath", document.getDataPath());
        map.put("active", document.getActive());
        map.put("classifier", document.getClassifier());

        return map;
    }
}
