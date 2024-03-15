package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.transaction.TransactionLink;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkModel;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkModificationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

/**
 * Maps transaction links.
 *
 * <ul>
 *     <li>API Request ({@link io.nuvalence.workmanager.service.generated.models.TransactionLinkCreationRequest})</li>
 *     <li>API Model ({@link io.nuvalence.workmanager.service.generated.models.TransactionLinkModel})</li>
 *     <li>Logic/Persistence Model
 *     ({@link io.nuvalence.workmanager.service.domain.transaction.TransactionLink})</li>
 * </ul>
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionLinkMapper {
    TransactionLinkMapper INSTANCE = Mappers.getMapper(TransactionLinkMapper.class);

    TransactionLinkModel transactionLinkToTransactionLinkModel(TransactionLink value);

    TransactionLink transactionLinkRequestToTransactionLink(
            TransactionLinkModificationRequest request,
            UUID fromTransactionId,
            UUID toTransactionId);
}
