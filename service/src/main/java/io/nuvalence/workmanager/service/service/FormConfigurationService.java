package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationCreateModel;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.repository.FormConfigurationRepository;
import io.nuvalence.workmanager.service.utils.UserUtility;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.DynaProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;

/**
 * Service layer to manage Form Configurations.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class FormConfigurationService {

    private static final String COMPLETED_PROCESS_TASK_KEY = "fallback";

    private final FormConfigurationRepository repository;
    private final TransactionTaskService transactionTaskService;
    private final SchemaService schemaService;

    /**
     * Returns a form configuration given its key as well as the key for its parent Transaction Definition.
     *
     * @param transactionDefinitionKey Parent Transaction Definition key
     * @param formConfigurationKey Form Configuration Key
     * @return FormConfiguration if exists
     */
    public Optional<FormConfiguration> getFormConfigurationByKeys(
            final String transactionDefinitionKey, final String formConfigurationKey) {
        // For now we only expect 1 record per given key pair. When we
        // implement config versioning, we will need to address this logic.
        return repository.searchByKeys(transactionDefinitionKey, formConfigurationKey).stream()
                .findFirst();
    }

    /**
     * Returns a form configuration given its key as well as the key for its parent Record Definition.
     *
     * @param recordDefinitionKey Parent Record Definition key
     * @param formConfigurationKey Form Configuration Key
     *
     * @return FormConfiguration if exists
     */
    public Optional<FormConfiguration> getFormConfigurationByKeyAndRecordDefinitionKey(
            final String recordDefinitionKey, final String formConfigurationKey) {
        return repository
                .findByKeyAndRecordDefinitionKey(formConfigurationKey, recordDefinitionKey)
                .stream()
                .findFirst();
    }

    /**
     * Returns a form configuration given its key.
     *
     * @param formConfigurationKey Form Configuration Key
     *
     * @return FormConfiguration if exists
     */
    public Optional<FormConfiguration> getFormConfigurationByKey(
            final String formConfigurationKey) {
        return repository.findByKey(formConfigurationKey).stream().findFirst();
    }

    /**
     * Returns a list of form configurations given the key for its parent Transaction Definition.
     *
     * @param transactionDefinitionKey Parent Transaction Definition key
     * @return FormConfiguration if exists
     */
    public List<FormConfiguration> getFormConfigurationsByTransactionDefinitionKey(
            final String transactionDefinitionKey) {
        return repository.findByTransactionDefinitionKey(transactionDefinitionKey);
    }

    public List<FormConfiguration> getFormConfigurationsByRecordDefinitionKey(
            final String recordDefinitionKey) {
        return repository.findByRecordDefinitionKey(recordDefinitionKey);
    }

    /**
     * Creates a default form configuration for a given transaction definition.
     * This method generates a default form configuration for the specified
     * transaction definition. The default form configuration includes a set
     * of predefined properties and settings.
     *
     *
     * @param transactionDefinition The transaction definition for which to create
     *                             the default form configuration.
     * @return The generated default form configuration.
     * @see FormConfigurationCreateModel
     * @see FormConfigurationMapper
     */
    public static FormConfiguration createDefaultFormConfiguration(
            TransactionDefinition transactionDefinition) {

        String defaultFromConfigKey = transactionDefinition.getDefaultFormConfigurationKey();
        if (defaultFromConfigKey == null || defaultFromConfigKey.isBlank()) {
            defaultFromConfigKey = transactionDefinition.getKey().concat("Default");
        }

        FormConfigurationCreateModel defaultFormConfiguration = new FormConfigurationCreateModel();
        defaultFormConfiguration.setSchemaKey(transactionDefinition.getSchemaKey());
        defaultFormConfiguration.setName("Default Form");
        defaultFormConfiguration.setDescription(
                "Default form for".concat(" ").concat(transactionDefinition.getName()));
        defaultFormConfiguration.setConfiguration(new HashMap<>());
        defaultFormConfiguration.setConfigurationSchema("formio");
        defaultFormConfiguration.setKey(defaultFromConfigKey);
        FormConfiguration formConfiguration =
                FormConfigurationMapper.INSTANCE.mapCreationModelToFormConfiguration(
                        defaultFormConfiguration);
        formConfiguration.setTransactionDefinition(transactionDefinition);
        return formConfiguration;
    }

    /**
     * Saves the provided FormConfiguration.
     *
     * @param formConfiguration FormConfiguration to save
     * @return updated FormConfiguration
     */
    public FormConfiguration saveFormConfiguration(final FormConfiguration formConfiguration) {
        // match existing database ID if form configuration exists with matching keys
        getFormConfigurationByKey(formConfiguration.getKey())
                .ifPresent(existing -> formConfiguration.setId(existing.getId()));

        if (formConfiguration.getConfiguration() == null) {
            formConfiguration.setConfiguration(new HashMap<>());
        }

        validateFormConfiguration(formConfiguration);

        return repository.save(formConfiguration);
    }

    /**
    * Validates the provided FormConfiguration, against the schema associated with it.
    *
    * @param formConfiguration FormConfiguration to validate
    */
    public void validateFormConfiguration(FormConfiguration formConfiguration) {
        if (formConfiguration.getConfiguration().isEmpty()) {
            return;
        }

        NuvalenceFormioComponent rootFormConfigComponent =
                FormConfigurationMapper.INSTANCE.formConfigToValidationConfig(formConfiguration);

        String rootSchemaKey = formConfiguration.getSchemaKey();

        Set<String> keyUniquenessSet = new HashSet<>();
        List<NuvalenceFormioValidationExItem> validationErrors = new ArrayList<>();

        validateParentComponentConfig(
                rootFormConfigComponent, rootSchemaKey, keyUniquenessSet, validationErrors);

        if (!validationErrors.isEmpty()) {
            throw new NuvalenceFormioValidationException(
                    NuvalenceFormioValidationExMessage.builder()
                            .formioValidationErrors(validationErrors)
                            .build());
        }
    }

    private void validateParentComponentConfig(
            NuvalenceFormioComponent parentComponent,
            String schemaKey,
            Set<String> keyUniquenessSet,
            List<NuvalenceFormioValidationExItem> validationErrors) {

        if (schemaKey == null) {
            addConfigErrorMessage(parentComponent.getKey(), "Null schema key", validationErrors);
            return;
        }

        Optional<Schema> schemaOptional = schemaService.getSchemaByKey(schemaKey);
        if (schemaOptional.isEmpty()) {
            addConfigErrorMessage(
                    parentComponent.getKey(),
                    "Schema with key '" + schemaKey + "' not found.",
                    validationErrors);
            return;
        }

        Schema schema = schemaOptional.get();

        for (NuvalenceFormioComponent childConfigComponent : parentComponent.getComponents()) {
            validateChildComponentConfig(
                    childConfigComponent, schema, keyUniquenessSet, validationErrors);
        }
    }

    private void validateChildComponentConfig(
            NuvalenceFormioComponent childComponent,
            Schema schema,
            Set<String> keyUniquenessSet,
            List<NuvalenceFormioValidationExItem> validationErrors) {

        if (!childComponent.isInput()) {
            return;
        } else if (childComponent.getKey() == null || childComponent.getKey().isBlank()) {
            addConfigErrorMessage(null, "A component is missing its key", validationErrors);
            return;
        } else if (!keyUniquenessSet.add(childComponent.getKey())) {
            addConfigErrorMessage(
                    childComponent.getKey(),
                    "Component key is found more than once in the form configuration",
                    validationErrors);
            return;
        }

        String[] keyParts = childComponent.getKey().split("\\.");
        DynaProperty schemaProperty = schema.getDynaProperty(keyParts[keyParts.length - 1]);
        if (schemaProperty == null) {
            addConfigErrorMessage(
                    childComponent.getKey(),
                    "Component key not found in schema with key '" + schema.getKey() + "'",
                    validationErrors);
            return;
        }

        validateFormConfigurationAgainstSchema(
                childComponent, schema, keyUniquenessSet, validationErrors, schemaProperty);
    }

    private void validateFormConfigurationAgainstSchema(
            NuvalenceFormioComponent childComponent,
            Schema schema,
            Set<String> keyUniquenessSet,
            List<NuvalenceFormioValidationExItem> validationErrors,
            DynaProperty schemaProperty) {
        boolean isListType = List.class.equals(schemaProperty.getType());
        boolean isDynamicEntity = DynamicEntity.class.equals(schemaProperty.getType());
        boolean isListContent = List.class.equals(schemaProperty.getContentType());
        boolean isDynamicEntityContent =
                DynamicEntity.class.equals(schemaProperty.getContentType());
        boolean isListContentDynamic = isListType && (isDynamicEntityContent || isListContent);

        if (isListType && !isDynamicEntityContent && !isListContent) {
            return;
        }

        if (childComponent.getComponents() != null && !childComponent.getComponents().isEmpty()) {
            if (isDynamicEntity || isListContentDynamic) {
                String childSchemaKey = schema.getRelatedSchemas().get(schemaProperty.getName());
                validateParentComponentConfig(
                        childComponent, childSchemaKey, keyUniquenessSet, validationErrors);
            } else {
                addConfigErrorMessage(
                        childComponent.getKey(),
                        "The component has at least one child component, but is not a parent"
                                + " component in the schema",
                        validationErrors);
            }
        }
    }

    private void addConfigErrorMessage(
            String componentKey,
            String message,
            List<NuvalenceFormioValidationExItem> validationErrors) {
        NuvalenceFormioValidationExItem validationError =
                NuvalenceFormioValidationExItem.builder()
                        .controlName(componentKey)
                        .errorMessage(message)
                        .build();
        validationErrors.add(validationError);
    }

    /**
     * Returns a map of active tasks to their form configurations given the userType of the current user and an
     * optional context.
     *
     * @param transaction Transaction with active tasks
     * @param context optional UI context
     * @return map of tasks to config configurations
     */
    public Map<String, FormConfiguration> getActiveFormConfiguration(
            final Transaction transaction, final String context) {

        final TransactionDefinition transactionDefinition = transaction.getTransactionDefinition();
        if (transactionDefinition == null) {
            throw new IllegalArgumentException("Transaction must have a transaction definition");
        }

        final List<WorkflowTask> tasks =
                transactionTaskService.getActiveTasksForCurrentUser(transaction);

        Map<String, FormConfiguration> results =
                getTaskFormsForCurrentUserType(transactionDefinition, tasks, context);

        if (results.isEmpty()) {
            // allowing the return of the default config by the internal private method when there
            // is no active task
            results =
                    getTaskFormsForCurrentUserType(
                            transactionDefinition,
                            List.of(WorkflowTask.builder().key(COMPLETED_PROCESS_TASK_KEY).build()),
                            context);
        }

        return results;
    }

    private Map<String, FormConfiguration> getTaskFormsForCurrentUserType(
            TransactionDefinition transactionDefinition,
            List<WorkflowTask> tasksWanted,
            String context) {

        final String userType = UserUtility.getAuthenticatedUserType();

        final Map<String, FormConfiguration> results = new HashMap<>();

        tasksWanted.stream()
                .map(WorkflowTask::getKey)
                .forEach(
                        taskKey ->
                                transactionDefinition
                                        .getFormConfigurationKey(taskKey, userType, context)
                                        .flatMap(
                                                formConfigurationKey ->
                                                        getFormConfigurationByKeys(
                                                                transactionDefinition.getKey(),
                                                                formConfigurationKey))
                                        .ifPresent(
                                                formConfiguration ->
                                                        results.put(taskKey, formConfiguration)));

        return results;
    }

    /**
     * Returns a map of first tasks to their form configurations based on transaction definition, and an optional context.
     *
     * @param transactionDef Transaction Definition to search for first tasks
     * @param context optional UI context
     * @return map of tasks to form configurations
     */
    public Map<String, FormConfiguration> getFirstTasksFormConfigurations(
            final TransactionDefinition transactionDef, final String context) {

        List<WorkflowTask> firstTasks =
                transactionTaskService.getFirstTasksForCurrentUser(transactionDef);

        return getTaskFormsForCurrentUserType(transactionDef, firstTasks, context);
    }

    public void deleteFormConfiguration(FormConfiguration formConfiguration) {
        repository.delete(formConfiguration);
    }
}
