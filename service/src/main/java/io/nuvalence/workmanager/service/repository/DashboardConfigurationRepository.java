package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for dashboard configuration.
 */
public interface DashboardConfigurationRepository
        extends CrudRepository<DashboardConfiguration, UUID> {
    Optional<DashboardConfiguration> findByTransactionDefinitionSetKey(
            @Param("transactionSetKey") String transactionSetKey);
}
