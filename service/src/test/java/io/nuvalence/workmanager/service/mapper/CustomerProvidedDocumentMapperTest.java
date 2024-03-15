package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReason;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

class CustomerProvidedDocumentMapperTest {
    private final CustomerProvidedDocumentMapper documentMapper =
            Mappers.getMapper(CustomerProvidedDocumentMapper.class);

    @Test
    void mapCustomerProvidedDocumentToModelTest() {
        CustomerProvidedDocument customerProvidedDocument =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .reviewStatus(ReviewStatus.NEW)
                        .transactionId(UUID.randomUUID())
                        .dataPath("schema_key_test")
                        .active(true)
                        .build();

        customerProvidedDocument.setRejectionReasons(
                List.of(
                        RejectionReason.builder()
                                .rejectionReasonValue(RejectionReasonType.POOR_QUALITY)
                                .customerProvidedDocument(customerProvidedDocument)
                                .build()));
        CustomerProvidedDocumentModelResponse customerProvidedDocumentModel =
                documentMapper.customerProvidedDocumentToModel(customerProvidedDocument);

        assertEquals(
                customerProvidedDocument.getId().toString(),
                customerProvidedDocumentModel.getId().toString());
        assertEquals(
                customerProvidedDocument.getActive(), customerProvidedDocumentModel.getActive());
        assertEquals(
                customerProvidedDocument.getReviewStatus().toString(),
                customerProvidedDocumentModel.getReviewStatus());
        assertEquals(
                customerProvidedDocument.getDataPath(),
                customerProvidedDocumentModel.getDataPath());
        assertEquals(
                customerProvidedDocument
                        .getRejectionReasons()
                        .get(0)
                        .getRejectionReasonValue()
                        .getValue(),
                customerProvidedDocumentModel.getRejectionReasons().get(0));

        assertEquals(
                customerProvidedDocument.getTransactionId(),
                customerProvidedDocumentModel.getTransaction());
    }

    @Test
    void testMapAndFilterCustomerProvidedDocument() {
        CustomerProvidedDocument customerProvidedDocumentNewState =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .reviewStatus(ReviewStatus.NEW)
                        .transactionId(UUID.randomUUID())
                        .dataPath("schema_key_test")
                        .active(true)
                        .build();

        CustomerProvidedDocument customerProvidedDocumentPendingState =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .reviewStatus(ReviewStatus.PENDING)
                        .transactionId(UUID.randomUUID())
                        .dataPath("schema_key_test")
                        .active(true)
                        .build();

        List<CustomerProvidedDocumentModelResponse> result =
                documentMapper.mapAndFilterCustomerProvidedDocuments(
                        List.of(
                                customerProvidedDocumentNewState,
                                customerProvidedDocumentPendingState));
        assertEquals(1, result.size());
        assertEquals(customerProvidedDocumentNewState.getId(), result.get(0).getId());
    }
}
