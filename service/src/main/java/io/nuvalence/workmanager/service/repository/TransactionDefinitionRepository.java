package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Transaction Definitions.
 */
public interface TransactionDefinitionRepository
        extends JpaRepository<TransactionDefinition, UUID>,
                JpaSpecificationExecutor<TransactionDefinition> {

    @Query("SELECT td FROM TransactionDefinition td WHERE td.name LIKE %:name%")
    List<TransactionDefinition> searchByPartialName(@Param("name") String name);

    @Query("SELECT td FROM TransactionDefinition td WHERE td.category LIKE :category%")
    List<TransactionDefinition> searchByPartialCategory(@Param("category") String category);

    @Query("SELECT td FROM TransactionDefinition td WHERE td.key = :key")
    List<TransactionDefinition> searchByKey(@Param("key") String key);

    @Query("SELECT td FROM TransactionDefinition td WHERE td.key IN (:keys)")
    List<TransactionDefinition> searchByKeys(@Param("keys") List<String> keys);

    List<TransactionDefinition> searchByTransactionDefinitionSetKey(
            String transactionDefinitionSetKey);

    TransactionDefinition findByKeyAndDefaultFormConfigurationKey(
            String key, String defaultFormConfigurationKey);

    void deleteByKey(String key);
}
