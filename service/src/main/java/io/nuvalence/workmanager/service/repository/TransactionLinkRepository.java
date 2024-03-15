package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.transaction.TransactionLink;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Transaction Links.
 */
public interface TransactionLinkRepository extends CrudRepository<TransactionLink, UUID> {
    @Query(
            "SELECT tl FROM TransactionLink tl WHERE tl.fromTransactionId = :id or"
                    + " tl.toTransactionId = :id")
    List<TransactionLink> getTransactionLinksById(@Param("id") UUID id);

    @Query(
            "SELECT tl FROM TransactionLink tl WHERE tl.fromTransactionId = :fromTransactionId AND"
                    + "  tl.toTransactionId = :toTransactionId AND tl.transactionLinkType.id ="
                    + " :transactionLinkTypeId")
    List<TransactionLink> getTransactionLinkByFromAndToAndType(
            @Param("fromTransactionId") UUID fromTransactionId,
            @Param("toTransactionId") UUID toTransactionId,
            @Param("transactionLinkTypeId") UUID transactionLinkTypeId);
}
