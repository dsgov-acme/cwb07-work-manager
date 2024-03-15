package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Transaction Definition Sets.
 */
public interface TransactionDefinitionSetRepository
        extends JpaRepository<TransactionDefinitionSet, UUID> {

    List<TransactionDefinitionSet> findByKey(String key);
}
