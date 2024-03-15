package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TransactionNote} entities.
 */
public interface TransactionNoteRepository
        extends JpaRepository<TransactionNote, UUID>, JpaSpecificationExecutor<TransactionNote> {
    List<TransactionNote> findByTransactionId(UUID transactionId);

    Optional<TransactionNote> findByTransactionIdAndId(UUID transactionId, UUID id);
}
