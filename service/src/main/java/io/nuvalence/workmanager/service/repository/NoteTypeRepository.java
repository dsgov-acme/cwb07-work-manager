package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.NoteType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for note types.
 */
public interface NoteTypeRepository extends CrudRepository<NoteType, UUID> {
    Optional<NoteType> findByName(@Param("name") String name);
}
