package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.config.exceptions.RecordLinkerException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.config.exceptions.model.RecordLinkerExceptionMessage;
import io.nuvalence.workmanager.service.domain.VersionedEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetDataRequirement;
import io.nuvalence.workmanager.service.domain.transaction.TransactionRecordLinker;
import io.nuvalence.workmanager.service.mapper.InvalidRegexPatternException;
import io.nuvalence.workmanager.service.models.TransactionDefinitionFilters;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaProperty;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.transaction.Transactional;

/**
 * Service layer to manage transaction definitions.
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class TransactionDefinitionService {
    private final TransactionDefinitionRepository repository;
    private final TransactionDefinitionSetService transactionDefinitionSetService;
    private final SchemaService schemaService;
    private final RecordDefinitionService recordDefinitionService;
    private final FormConfigurationService formConfigurationService;

    /**
     * Fetches a transaction definition from the database by id (primary key).
     *
     * @param id transaction definition id to fetch
     * @return transaction definition object
     */
    public Optional<TransactionDefinition> getTransactionDefinitionById(final UUID id) {
        return repository.findById(id);
    }

    /**
     * Fetches the latest version of a transaction definition from the database by key.
     *
     * @param key transaction definition key to fetch
     * @return transaction definition object
     */
    public Optional<TransactionDefinition> getTransactionDefinitionByKey(final String key) {
        // TODO When we implement versioned transaction configuration, this will need to select for
        // the newest version
        return repository.searchByKey(key).stream().findFirst();
    }

    /**
     * Returns a list of transaction definitions whose names match the query passed in.
     *
     * @param name Partial name query
     * @return List of transaction definitions matching query
     */
    public List<TransactionDefinition> getTransactionDefinitionsByPartialNameMatch(
            final String name) {
        if (name == null) {
            return repository.findAll();
        } else {
            return repository.searchByPartialName(name);
        }
    }

    /**
     * Returns a list of transaction definitions(paged) whose names match the query passed in.
     *
     * @param filter filters
     * @return List of transaction definitions matching query (paged)
     */
    public Page<TransactionDefinition> getTransactionDefinitionsByFilters(
            TransactionDefinitionFilters filter) {
        return repository.findAll(
                filter.getTransactionDefinitionSpecifications(), filter.getPageRequest());
    }

    /**
     * Returns a list of transaction definitions whose names match the query passed in.
     *
     * @param category Partial name query
     * @return List of transaction definitions matching query
     */
    public List<TransactionDefinition> getTransactionDefinitionsByPartialCategoryMatch(
            final String category) {
        if (category == null) {
            return repository.findAll();
        } else {
            return repository.searchByPartialCategory(category);
        }
    }

    /**
     * Saves a transaction definition.
     *
     * @param transactionDefinition transaction definition to save
     * @return post-save version of transaction definition
     *
     * @throws UnexpectedException if the transaction definition key is invalid
     */
    public TransactionDefinition saveTransactionDefinition(
            final TransactionDefinition transactionDefinition) {
        try {
            if (!VersionedEntity.isValidName(transactionDefinition.getKey())) {
                throw new InvalidRegexPatternException(
                        transactionDefinition.getKey(),
                        VersionedEntity.Constants.VALID_FILE_NAME_REGEX_PATTERN,
                        "transaction definition");
            }
        } catch (InvalidRegexPatternException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e.getMessage(), e);
        }

        if (transactionDefinition.getFormConfigurationSelectionRules() != null
                && !transactionDefinition.getFormConfigurationSelectionRules().isEmpty()) {
            transactionDefinition
                    .getFormConfigurationSelectionRules()
                    .forEach(rule -> rule.setTransactionDefinition(transactionDefinition));
        }

        if (transactionDefinition.getRecordLinkers() == null) {
            transactionDefinition.setRecordLinkers(Collections.emptyList());
        } else if (!transactionDefinition.getRecordLinkers().isEmpty()) {
            transactionDefinition
                    .getRecordLinkers()
                    .forEach(linker -> linker.setTransactionDefinition(transactionDefinition));
            validateRecordLinkers(transactionDefinition);
        }

        return repository.save(transactionDefinition);
    }

    private void validateRecordLinkers(TransactionDefinition transactionDefinition) {
        List<TransactionRecordLinker> recordLinkers = transactionDefinition.getRecordLinkers();
        if (recordLinkers == null) {
            return;
        }

        Set<String> recordDefKeysSet = new HashSet<>();
        for (var linker : recordLinkers) {
            String recordDefKey = linker.getRecordDefinitionKey();
            if (recordDefKey == null) {
                throw new ProvidedDataException(
                        "A 'recordDefinitionKey' is required for record linkers");
            }
            Optional<RecordDefinition> recordDefOptional =
                    recordDefinitionService.getRecordDefinitionByKey(recordDefKey);
            if (recordDefOptional.isEmpty()) {
                throw new ProvidedDataException(
                        String.format("RecordDefinition '%s' does not exist", recordDefKey));
            }
            if (!recordDefKeysSet.add(recordDefKey)) {
                throw new ProvidedDataException(
                        String.format(
                                "There is more than one RecordLinker for RecordDefinition '%s'",
                                recordDefKey));
            }
            RecordDefinition recordDef = recordDefOptional.get();

            if (linker.getFieldMappings() == null || linker.getFieldMappings().isEmpty()) {
                throw new ProvidedDataException("RecordLinkers require non-empty fieldMappings");
            }
            validateRecordRelatedConfig(recordDef, linker, recordDefKey);
            validateTransactionRelatedConfig(transactionDefinition, linker, recordDefKey);
        }
    }

    private void validateTransactionRelatedConfig(
            TransactionDefinition transactionDefinition,
            TransactionRecordLinker linker,
            String recordDefKey) {

        Set<Map<String, Object>> components = new HashSet<>();
        FormConfiguration formConfig =
                FormConfiguration.builder()
                        .schemaKey(transactionDefinition.getSchemaKey())
                        .transactionDefinitionKey(transactionDefinition.getKey())
                        .configuration(Map.of("components", components))
                        .build();

        linker.getFieldMappings()
                .values()
                .forEach(
                        value -> {
                            value = value.replaceFirst("^.*\\(", "");
                            value = value.replaceAll("\\).*$", "");
                            value = value.replaceAll("\s+", "");
                            List<String> tokens =
                                    Arrays.stream(value.split(",")).map(String::trim).toList();

                            tokens.forEach(
                                    token -> {
                                        if (token.startsWith("data.")) {
                                            components.add(
                                                    Map.of(
                                                            "key",
                                                            token.replace("data.", ""),
                                                            "input",
                                                            true));
                                        }
                                    });
                        });

        try {
            formConfigurationService.validateFormConfiguration(formConfig);
        } catch (NuvalenceFormioValidationException e) {
            var exception =
                    RecordLinkerException.builder()
                            .errorContents(
                                    RecordLinkerExceptionMessage.builder()
                                            .recordDefinitionKey(recordDefKey)
                                            .fieldValueErrors(
                                                    e.getFormioValidationErrors()
                                                            .getFormioValidationErrors())
                                            .build());
            throw exception.build();
        }
    }

    private void validateRecordRelatedConfig(
            RecordDefinition recordDefinition,
            TransactionRecordLinker linker,
            String recordDefKey) {

        List<Map<String, Object>> components = new ArrayList<>();
        FormConfiguration formConfig =
                FormConfiguration.builder()
                        .schemaKey(recordDefinition.getSchemaKey())
                        .recordDefinitionKey(recordDefinition.getKey())
                        .configuration(Map.of("components", components))
                        .build();

        linker.getFieldMappings()
                .keySet()
                .forEach(recordKey -> components.add(Map.of("key", recordKey, "input", true)));

        try {
            formConfigurationService.validateFormConfiguration(formConfig);
        } catch (NuvalenceFormioValidationException e) {
            var exception =
                    RecordLinkerException.builder()
                            .errorContents(
                                    RecordLinkerExceptionMessage.builder()
                                            .recordDefinitionKey(recordDefKey)
                                            .fieldKeyErrors(
                                                    e.getFormioValidationErrors()
                                                            .getFormioValidationErrors())
                                            .build());
            throw exception.build();
        }
    }

    /**
     * Creates an array of transaction definition keys for a given query.
     *
     * @param transactionDefinitionKey transaction definition set key to query
     * @param transactionDefinitionSetKey transaction definition key to query
     * @return List of transaction definition keys matching query
     */
    public List<String> createTransactionDefinitionKeysList(
            String transactionDefinitionKey, String transactionDefinitionSetKey) {

        if (transactionDefinitionSetKey == null) {
            return transactionDefinitionKey != null ? List.of(transactionDefinitionKey) : null;
        }

        List<String> transactionDefinitionKeysList =
                getTransactionDefinitionsBySetKey(transactionDefinitionSetKey).stream()
                        .map(TransactionDefinition::getKey)
                        .toList();

        if (transactionDefinitionKey != null
                && !transactionDefinitionKeysList.contains(transactionDefinitionKey)) {
            return Collections.emptyList();
        }

        return transactionDefinitionKeysList;
    }

    public List<TransactionDefinition> getTransactionDefinitionsBySetKey(
            String transactionDefinitionSetKey) {
        return repository.searchByTransactionDefinitionSetKey(transactionDefinitionSetKey);
    }

    /**
     * Validate that the definition schema conforms to the data requirements of the TransactionDefinitionSet.
     *
     * @param transactionDefinition transaction definition whose schema is to be validated
     *
     * @throws BusinessLogicException if the schema does not conform to the data requirements
     */
    public void validateTransactionDefinitionSetLink(TransactionDefinition transactionDefinition) {

        String transactionDefinitionSetKey = transactionDefinition.getTransactionDefinitionSetKey();
        if (transactionDefinitionSetKey != null) {

            Optional<TransactionDefinitionSet> optionalTransactionDefinitionSet =
                    transactionDefinitionSetService.getTransactionDefinitionSet(
                            transactionDefinitionSetKey);
            if (optionalTransactionDefinitionSet.isEmpty()) {
                throw new BusinessLogicException(
                        String.format(
                                "Transaction definition set with key %s does not exist",
                                transactionDefinitionSetKey));
            }

            TransactionDefinitionSet transactionDefinitionSet =
                    optionalTransactionDefinitionSet.get();

            Optional<Schema> optionalSchema =
                    schemaService.getSchemaByKey(transactionDefinition.getSchemaKey());
            if (optionalSchema.isEmpty()) {
                throw new BusinessLogicException(
                        String.format(
                                "Schema with key %s does not exist",
                                transactionDefinition.getSchemaKey()));
            }
            Schema schema = optionalSchema.get();

            validateTransactionDefinitionSetAndSchema(transactionDefinitionSet, schema);
        }
    }

    private void validateTransactionDefinitionSetAndSchema(
            TransactionDefinitionSet set, Schema schema) {
        for (TransactionDefinitionSetDataRequirement constraint : set.getConstraints()) {
            String[] tokens = constraint.getPath().split("\\.");
            DynaProperty currentDynaProperty =
                    identifyInitialDynaProperty(schema.getDynaProperties(), tokens[0]);
            validateTokenPath(tokens, constraint, currentDynaProperty, schema);
        }
    }

    private void validateTokenPath(
            String[] tokens,
            TransactionDefinitionSetDataRequirement constraint,
            DynaProperty currentDynaProperty,
            Schema currentSchema) {
        for (int i = 1; i <= tokens.length; i++) {
            if (currentDynaProperty == null) {
                throw new BusinessLogicException(
                        String.format(
                                "Schema property not found for path %s", constraint.getPath()));
            }

            if (currentDynaProperty.getType().isAssignableFrom(DynamicEntity.class)) {
                currentSchema = moveToRelatedSchema(currentDynaProperty, currentSchema);
                currentDynaProperty =
                        identifyInitialDynaProperty(currentSchema.getDynaProperties(), tokens[i]);
            } else if (currentDynaProperty.getType().isAssignableFrom(List.class)) {
                validateListType(currentDynaProperty, constraint);
            } else {
                validateSimpleType(currentDynaProperty, constraint);
            }
        }
    }

    private Schema moveToRelatedSchema(DynaProperty currentDynaProperty, Schema currentSchema) {
        String schemaKey = currentSchema.getRelatedSchemas().get(currentDynaProperty.getName());
        if (schemaKey == null) {
            throw new BusinessLogicException(
                    String.format(
                            "Related schema not found for property %s",
                            currentDynaProperty.getName()));
        }
        Optional<Schema> optionalNewSchema = schemaService.getSchemaByKey(schemaKey);
        return optionalNewSchema.orElseThrow(
                () ->
                        new BusinessLogicException(
                                String.format(
                                        "Related schema not found for schema key %s", schemaKey)));
    }

    private void validateListType(
            DynaProperty currentDynaProperty, TransactionDefinitionSetDataRequirement constraint) {
        if (!currentDynaProperty.getContentType().getSimpleName().equals(constraint.getType())) {
            throw new BusinessLogicException(
                    String.format(
                            "Schema and data requirement are not compatible for path %s, invalid"
                                    + " list type, constraint expected %s, got %s",
                            constraint.getPath(),
                            constraint.getType(),
                            currentDynaProperty.getContentType().getSimpleName()));
        }
    }

    private void validateSimpleType(
            DynaProperty currentDynaProperty, TransactionDefinitionSetDataRequirement constraint) {
        if (!currentDynaProperty.getType().getSimpleName().equals(constraint.getType())) {
            throw new BusinessLogicException(
                    String.format(
                            "Schema and data requirement are not compatible for path %s, constraint"
                                    + " expected %s, got %s",
                            constraint.getPath(),
                            constraint.getType(),
                            currentDynaProperty.getType().getSimpleName()));
        }
    }

    private DynaProperty identifyInitialDynaProperty(DynaProperty[] properties, String token) {
        for (DynaProperty property : properties) {
            if (property.getName().equals(token)) {
                return property;
            }
        }
        return null;
    }

    public void deleteTransactionDefinition(String key) {
        repository.deleteByKey(key);
    }

    public boolean isActiveFormConfiguration(String key, String formConfigurationKey) {
        return repository.findByKeyAndDefaultFormConfigurationKey(key, formConfigurationKey)
                != null;
    }
}
