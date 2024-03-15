package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReason;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps CustomerProvidedDocument objects and their OpenApi generated equivalent.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CustomerProvidedDocumentMapper {
    CustomerProvidedDocumentMapper INSTANCE =
            Mappers.getMapper(CustomerProvidedDocumentMapper.class);

    @Mapping(
            target = "transaction",
            expression = "java(customerProvidedDocument.getTransactionId())")
    @Mapping(
            target = "rejectionReasons",
            expression =
                    "java(mapRejectionReasonsForResponse(customerProvidedDocument.getRejectionReasons()))")
    CustomerProvidedDocumentModelResponse customerProvidedDocumentToModel(
            CustomerProvidedDocument customerProvidedDocument);

    /**
     * Maps all active documents to the model to be shown to user, all inactive documents are excluded.
     *
     * @param customerProvidedDocuments document objects to be filtered and mapped.
     *
     * @return mapped list of customer provided document models.
     */
    default List<CustomerProvidedDocumentModelResponse> mapAndFilterCustomerProvidedDocuments(
            List<CustomerProvidedDocument> customerProvidedDocuments) {
        if (customerProvidedDocuments == null) {
            return new ArrayList<>();
        }
        return customerProvidedDocuments.stream()
                .filter(
                        document ->
                                document.getActive()
                                        && !document.getReviewStatus().equals(ReviewStatus.PENDING))
                .map(this::customerProvidedDocumentToModel)
                .toList();
    }

    /**
     * Maps rejection reason objects to a string list.
     *
     * @param rejectionReasons Reasons to be mapped.
     *
     * @return the resulting string list.
     */
    default List<String> mapRejectionReasonsForResponse(List<RejectionReason> rejectionReasons) {
        if (rejectionReasons == null) {
            return new ArrayList<>();
        }

        return rejectionReasons.stream()
                .map(rejectionReason -> rejectionReason.getRejectionReasonValue().getValue())
                .toList();
    }
}
