package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for transaction definition set order.
 */
public interface TransactionDefinitionSetOrderRepository
        extends JpaRepository<TransactionDefinitionSetOrder, Integer> {
    Optional<TransactionDefinitionSetOrder> findByTransactionDefinitionSetKey(String key);
}
