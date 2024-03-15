package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Record Definition repository.
 */
public interface RecordDefinitionRepository
        extends JpaRepository<RecordDefinition, UUID>, JpaSpecificationExecutor<RecordDefinition> {

    Optional<RecordDefinition> findByKey(String key);
}
