package io.nuvalence.workmanager.service.utils.testutils;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelRequest;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelResponse;
import org.apache.commons.beanutils.DynaProperty;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Utility transaction methods for testing.
 */
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "java:S2187"})
public class TransactionUtilsForTest {

    /**
     * Creates a mock CustomerProvidedDocument for testing purposes.
     *
     * @param transaction the transaction to which the document is part of.
     * @return a customer provided document mock
     */
    public static CustomerProvidedDocument createCustomerProvidedDocument(Transaction transaction) {
        return CustomerProvidedDocument.builder()
                .id(UUID.randomUUID())
                .reviewStatus(ReviewStatus.NEW)
                .transactionId(transaction.getId())
                .dataPath("schema_key_test")
                .active(true)
                .build();
    }

    /**
     * Creates a transaction builder for testing purposes.
     *
     * @return a transaction builder
     */
    public static Transaction.TransactionBuilder getCommonTransactionBuilder() {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .transactionDefinitionId(UUID.randomUUID())
                .transactionDefinitionKey("dummy")
                .processInstanceId("Dummy user test")
                .externalId("test")
                .status("low")
                .createdBy(UUID.randomUUID().toString())
                .createdTimestamp(OffsetDateTime.now())
                .lastUpdatedTimestamp(OffsetDateTime.now())
                .subjectProfileType(ProfileType.INDIVIDUAL)
                .additionalParties(Collections.emptyList())
                .data(new DynamicEntity(Schema.builder().build()))
                .subjectProfileId(UUID.randomUUID());
    }

    /**
     * Creates a mock CustomerProvidedDocumentModelResponse for testing purposes.
     *
     * @param transaction the transaction to which the document is part of.
     * @return a customer provided document model response mock
     */
    public static CustomerProvidedDocumentModelResponse createCustomerProvidedDocumentModel(
            Transaction transaction) {
        CustomerProvidedDocumentModelResponse customerProvidedDocumentModel =
                new CustomerProvidedDocumentModelResponse();

        customerProvidedDocumentModel.setId(UUID.randomUUID());
        customerProvidedDocumentModel.setActive(true);
        customerProvidedDocumentModel.setReviewStatus(ReviewStatus.NEW.toString());
        customerProvidedDocumentModel.setTransaction(transaction.getId());
        customerProvidedDocumentModel.dataPath("schema_key");

        return customerProvidedDocumentModel;
    }

    /**
     * Creates a mock CustomerProvidedDocumentModelRequest for testing purposes.
     *
     * @return a customer provided document model request mock
     */
    public static CustomerProvidedDocumentModelRequest
            createCustomerProvidedDocumentModelRequest() {
        CustomerProvidedDocumentModelRequest customerProvidedDocumentModel =
                new CustomerProvidedDocumentModelRequest();

        customerProvidedDocumentModel.setReviewStatus(ReviewStatus.NEW.getValue());

        return customerProvidedDocumentModel;
    }

    /**
     * Creates a mock CustomerProvidedDocument for testing purposes.
     *
     * @param documentId optional document ID to be assigned to the new CustomerProvidedDocument
     * @return a customer provided document model request mock
     */
    public static CustomerProvidedDocument createRequestCustomerProvidedDocument(UUID documentId) {
        CustomerProvidedDocument customerProvidedDocument =
                CustomerProvidedDocument.builder()
                        .id(documentId != null ? documentId : UUID.randomUUID())
                        .reviewStatus(ReviewStatus.NEW)
                        .dataPath("schema_key.property")
                        .build();

        return customerProvidedDocument;
    }

    /**
     * Creates a mock Schema for testing purposes.
     *
     * @return a schema mock
     */
    public static Schema createMockSchema() {
        List<DynaProperty> dynaProperties = List.of(new DynaProperty("property"));

        return Schema.builder().properties(dynaProperties).build();
    }
}
