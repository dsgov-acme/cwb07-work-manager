package io.nuvalence.workmanager.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.VersionedEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentProcessingConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.InvalidRegexPatternException;
import io.nuvalence.workmanager.service.models.SchemaFilters;
import io.nuvalence.workmanager.service.repository.SchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaProperty;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Service layer to manage Schemas.
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class SchemaService {
    public static final String UNABLE_TO_PARSE_SCHEMA_JSON_STORED_IN_DATABASE =
            "Unable to parse schema JSON stored in database.";
    private final SchemaRepository schemaRepository;
    private final DynamicSchemaMapper mapper;

    /**
     * Fetches a schema from the database by key.
     *
     * @param key Schema key to fetch
     * @return Schema object
     */
    public Optional<Schema> getSchemaByKey(final String key) {
        return schemaRepository
                .findByKey(key)
                .map(
                        row -> {
                            try {
                                return mapper.schemaRowToSchema(row);
                            } catch (JsonProcessingException e) {
                                throw new UnexpectedException(
                                        UNABLE_TO_PARSE_SCHEMA_JSON_STORED_IN_DATABASE, e);
                            }
                        });
    }

    private Optional<SchemaRow> getSchemaRowByKey(final String key) {
        return schemaRepository.findByKey(key);
    }

    /**
     * Fetches a schema from the database by id (primary key).
     *
     * @param id Schema id to fetch
     * @return Schema object
     */
    public Optional<Schema> getSchemaById(final UUID id) {
        return schemaRepository
                .findById(id)
                .map(
                        row -> {
                            try {
                                return mapper.schemaRowToSchema(row);
                            } catch (JsonProcessingException e) {
                                throw new UnexpectedException(
                                        UNABLE_TO_PARSE_SCHEMA_JSON_STORED_IN_DATABASE, e);
                            }
                        });
    }

    /**
     * Returns a Page of Schemas based on the filters passed in.
     *
     *
     * @param filters Filters to apply to the query
     *
     * @return Page of Schemas
     */
    public Page<Schema> getSchemasByFilters(final SchemaFilters filters) {

        Page<SchemaRow> schemaPage =
                schemaRepository.findAll(
                        filters.getSchemaSpecification(), filters.getPageRequest());

        return schemaPage.map(
                (SchemaRow row) -> {
                    try {
                        return mapper.schemaRowToSchema(row);
                    } catch (JsonProcessingException e) {
                        throw new UnexpectedException(
                                UNABLE_TO_PARSE_SCHEMA_JSON_STORED_IN_DATABASE, e);
                    }
                });
    }

    /**
     * Saves a schema.
     *
     * @param schema Schema to save.
     * @return saved Schema
     *
     * @throws InvalidRegexPatternException if the schema key is invalid.
     * @throws UnexpectedException if the schema cannot be converted to JSON.
     */
    public Schema saveSchema(final Schema schema) {
        try {
            if (!VersionedEntity.isValidName(schema.getKey())) {
                throw new InvalidRegexPatternException(
                        schema.getKey(),
                        VersionedEntity.Constants.VALID_FILE_NAME_REGEX_PATTERN,
                        "schema");
            }

            validateUniqueAttributeNames(schema);

            List<Schema> schemaParents = getSchemaParents(schema.getKey());

            SchemaRow schemaRow = mapper.schemaToSchemaRow(schema);
            Set<SchemaRow> children = new HashSet<>();
            schema.getRelatedSchemas()
                    .forEach(
                            (key, value) ->
                                    validateSchemaChildren(schema, children, schemaParents, value));
            schemaRow.setChildren(children);

            return mapper.schemaRowToSchema(schemaRepository.save(schemaRow));
        } catch (InvalidRegexPatternException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Unable to marshall schema to JSON.", e);
        }
    }

    private void validateUniqueAttributeNames(Schema schema) {

        DynaProperty[] dynaProperties = schema.getDynaProperties();

        if (dynaProperties != null && dynaProperties.length > 0) {

            HashSet<String> attributeNames = new HashSet<>();
            // tree set to provide a sorted response
            TreeSet<String> repeatedAttributes = new TreeSet<>();

            for (DynaProperty property : dynaProperties) {
                if (!attributeNames.add(property.getName())) {
                    repeatedAttributes.add(property.getName());
                }
            }

            if (!repeatedAttributes.isEmpty()) {
                throw new ProvidedDataException(
                        "Attribute names must be unique. Repeated values: "
                                + repeatedAttributes.toString());
            }
        }
    }

    private void validateSchemaChildren(
            Schema schema, Set<SchemaRow> children, List<Schema> schemaParents, String key) {
        if (key.equals(schema.getKey())) {
            throw new ProvidedDataException(
                    String.format("The %s schema cannot be a subSchema of itself.", key));
        }

        boolean childIsParent = schemaParents.stream().anyMatch(item -> item.getKey().equals(key));
        if (childIsParent) {
            throw new ProvidedDataException(
                    String.format(
                            "Schema %s is a parent of %s. A sub-schema"
                                    + " cannot be a parent of the current"
                                    + " schema.",
                            key, schema.getKey()));
        }

        getSchemaRowByKey(key)
                .ifPresent(
                        childSchema -> {
                            children.add(childSchema);
                        });

        if (children.isEmpty()) {
            throw new NotFoundException("Child schema not found for key: " + key);
        }
    }

    public List<String> getDocumentProcessorsInSchemaPath(String path, Schema schema) {
        String[] pathArray = path.split("\\.");
        return getDocumentProcessorsInSchemaPath(pathArray, schema);
    }

    private List<String> getDocumentProcessorsInSchemaPath(String[] path, Schema schema) {
        List<AttributeConfiguration> attributeConfigurations;
        String currentPathPoint = path[0];

        if (path.length == 1 && dynaPropertyExists(schema.getDynaProperties(), currentPathPoint)) {
            attributeConfigurations = schema.getAttributeConfigurations().get(currentPathPoint);
            if (attributeConfigurations == null) {
                return Collections.emptyList();
            }
        } else if (schema.getRelatedSchemas().containsKey(currentPathPoint)) {
            Optional<Schema> subSchema =
                    getSchemaByKey(schema.getRelatedSchemas().get(currentPathPoint));
            if (!subSchema.isPresent()) {
                throw new ProvidedDataException(
                        "Schema not found: " + schema.getRelatedSchemas().get(currentPathPoint));
            }
            return getDocumentProcessorsInSchemaPath(
                    Arrays.copyOfRange(path, 1, path.length), subSchema.get());
        } else {
            throw new ProvidedDataException("Wrong data path");
        }

        return attributeConfigurations.stream()
                .filter(DocumentProcessingConfiguration.class::isInstance)
                .map(dp -> ((DocumentProcessingConfiguration) dp).getProcessorId())
                .toList();
    }

    public Map<String, List<Schema>> getAllRelatedSchemas(String mainSchemaKey) {
        return getAllRelatedSchemas(mainSchemaKey, new HashMap<>());
    }

    /**
     * Returns a map of all related schemas for a given schema key.
     *
     * @param mainSchemaKey key for root schema.
     * @param relatedSchemasMap map of related schemas, empty in first run.
     * @return Map of all related schemas.
     *
     * @throws NotFoundException if the schema is not found.
     */
    public Map<String, List<Schema>> getAllRelatedSchemas(
            String mainSchemaKey, Map<String, List<Schema>> relatedSchemasMap) {
        Optional<Schema> mainSchema = getSchemaByKey(mainSchemaKey);
        if (mainSchema.isEmpty()) {
            throw new NotFoundException("Schema not found: " + mainSchemaKey);
        }

        List<Schema> relatedSchemas =
                mainSchema.get().getRelatedSchemas().values().stream()
                        .map(this::getSchemaByKey)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList();

        relatedSchemasMap.put(mainSchemaKey, relatedSchemas);

        for (Schema relatedSchema : relatedSchemas) {
            if (!relatedSchema.getRelatedSchemas().isEmpty()) {
                getAllRelatedSchemas(relatedSchema.getKey(), relatedSchemasMap);
            }
        }

        return relatedSchemasMap;
    }

    /**
     * Deletes a schema identified by its unique key.
     * This method attempts to locate a schema in the repository based on the provided key. If a schema with
     * the specified key is found, it is deleted from the repository. If no schema is found with the given key,
     * a {@link NotFoundException} is thrown, indicating that the schema was not found.
     *
     *
     * @param schema Schema to delete.
     * @throws JsonProcessingException if the schema cannot be converted to JSON.
     */
    public void deleteSchema(Schema schema) throws JsonProcessingException {
        schemaRepository.delete(mapper.schemaToSchemaRow(schema));
    }

    private boolean dynaPropertyExists(DynaProperty[] properties, String propertyName) {
        return Arrays.stream(properties)
                .anyMatch(property -> property.getName().equals(propertyName));
    }

    /**
     * List all the parent schemas that have a relationship, directly or indirectly, with a given child schema key.
     *
     * @param schemaKey child schema key
     * @return list of parent schemas
     */
    public List<Schema> getSchemaParents(String schemaKey) {
        return schemaRepository.getSchemaParents(schemaKey).stream()
                .map(
                        item -> {
                            try {
                                return mapper.schemaRowToSchema(item);
                            } catch (JsonProcessingException e) {
                                throw new UnexpectedException(
                                        UNABLE_TO_PARSE_SCHEMA_JSON_STORED_IN_DATABASE, e);
                            }
                        })
                .filter(Objects::nonNull)
                .toList();
    }
}
