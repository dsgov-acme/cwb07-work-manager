package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EntityReferenceRepository extends JpaRepository<EntityReference, UUID> {
    List<EntityReference> findByEntityIdAndType(UUID entityId, EntityType type);
}
