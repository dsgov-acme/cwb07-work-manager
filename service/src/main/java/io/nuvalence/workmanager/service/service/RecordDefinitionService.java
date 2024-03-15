package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.models.RecordDefinitionFilters;
import io.nuvalence.workmanager.service.repository.RecordDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

/**
 * Service to manage record definitions.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RecordDefinitionService {

    private final RecordDefinitionRepository repository;
    private final SchemaService schemaService;

    /**
     * Fetches a record definition from the database by id (primary key).
     *
     * @param id record definition id to fetch
     * @return record definition object
     */
    public Optional<RecordDefinition> getRecordDefinitionById(UUID id) {

        return repository.findById(id);
    }

    /**
     * Gets a record definition from the database by key.
     *
     * @param key record definition key
     * @return record definition
     */
    public Optional<RecordDefinition> getRecordDefinitionByKey(String key) {

        return repository.findByKey(key);
    }

    /**
     * Returns a paged list of record definitions matching the provided filters.
     *
     * @param filter filters
     * @return paged list of record definitions matching the filters
     */
    public Page<RecordDefinition> getRecordDefinitionsByFilters(RecordDefinitionFilters filter) {

        return repository.findAll(
                filter.getRecordDefinitionSpecification(), filter.getPageRequest());
    }

    /**
     * Saves a record definition, performing needed validations.
     *
     * @param recordDefinition record definition to save
     * @return saved version of record definition
     */
    public RecordDefinition saveRecordDefinition(RecordDefinition recordDefinition) {

        if (schemaService.getSchemaByKey(recordDefinition.getSchemaKey()).isEmpty()) {
            throw new ProvidedDataException("Schema key not found");
        }

        return repository.save(recordDefinition);
    }
}
