package io.nuvalence.workmanager.service.controllers;

import static io.nuvalence.workmanager.service.service.FormConfigurationService.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.workmanager.service.config.exceptions.ConflictException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.AllowedLink;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkType;
import io.nuvalence.workmanager.service.generated.controllers.AdminApiDelegate;
import io.nuvalence.workmanager.service.generated.models.AllowedLinkCreationRequest;
import io.nuvalence.workmanager.service.generated.models.AllowedLinkModel;
import io.nuvalence.workmanager.service.generated.models.DashboardCountsModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationCreateModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationResponseModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationUpdateModel;
import io.nuvalence.workmanager.service.generated.models.PagedRecordDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.PagedSchemaModel;
import io.nuvalence.workmanager.service.generated.models.PagedTransactionDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.PagedTransactionDefinitionSetModel;
import io.nuvalence.workmanager.service.generated.models.PagedWorkflowModel;
import io.nuvalence.workmanager.service.generated.models.ParentSchemas;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionUpdateModel;
import io.nuvalence.workmanager.service.generated.models.SchemaCreateModel;
import io.nuvalence.workmanager.service.generated.models.SchemaModel;
import io.nuvalence.workmanager.service.generated.models.SchemaUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TaskModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardResultModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkTypeModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowModel;
import io.nuvalence.workmanager.service.mapper.AllowedLinkMapper;
import io.nuvalence.workmanager.service.mapper.DashboardConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.mapper.RecordDefinitionMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionSetMapper;
import io.nuvalence.workmanager.service.mapper.TransactionLinkTypeMapper;
import io.nuvalence.workmanager.service.mapper.WorkflowAndTaskMapper;
import io.nuvalence.workmanager.service.models.RecordDefinitionFilters;
import io.nuvalence.workmanager.service.models.SchemaFilters;
import io.nuvalence.workmanager.service.models.TransactionDefinitionFilters;
import io.nuvalence.workmanager.service.models.TransactionDefinitionSetFilter;
import io.nuvalence.workmanager.service.service.AllowedLinkService;
import io.nuvalence.workmanager.service.service.DashboardConfigurationService;
import io.nuvalence.workmanager.service.service.FormConfigurationService;
import io.nuvalence.workmanager.service.service.RecordDefinitionService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionSetOrderService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionSetService;
import io.nuvalence.workmanager.service.service.TransactionLinkTypeService;
import io.nuvalence.workmanager.service.service.WorkflowTasksService;
import io.nuvalence.workmanager.service.utils.ConfigurationUtility;
import io.nuvalence.workmanager.service.utils.camunda.ConsistencyChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Controller layer for API.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public class AdminApiDelegateImpl implements AdminApiDelegate {
    public static final String CREATE_CERBOS_ACTION = "create";
    public static final String UPDATE_CERBOS_ACTION = "update";
    public static final String DELETE_CERBOS_ACTION = "delete";

    public static final String VIEW_CERBOS_ACTION = "view";
    public static final String CONFIGURATION_CERBOS_RESORCE = "configuration";
    public static final String DASHBOARD_CONFIGURATION = "dashboard_configuration";
    public static final String EXPORT_CERBOS_ACTION = "export";
    public static final String TRANSACTION_NOT_FOUND_MSG = "Transaction definition not found";
    public static final String FORM_CONFIG_NOT_FOUND_MSG = "Form configuration not found";
    public static final String RECORD_DEF_NOT_FOUND_MSG = "Record definition not found";
    private final AllowedLinkService allowedLinkService;
    private final SchemaService schemaService;
    private final TransactionDefinitionService transactionDefinitionService;
    private final RecordDefinitionService recordDefinitionService;
    private final TransactionDefinitionSetService transactionDefinitionSetService;
    private final TransactionLinkTypeService transactionLinkTypeService;
    private final FormConfigurationService formConfigurationService;
    private final DynamicSchemaMapper dynamicSchemaMapper;
    private final ConsistencyChecker consistencyChecker;
    private final ConfigurationUtility configurationUtility;
    private final AuthorizationHandler authorizationHandler;
    private final WorkflowTasksService workflowTasksService;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final WorkflowAndTaskMapper workflowAndTaskMapper;
    private final DashboardConfigurationService dashboardConfigurationService;
    private final DashboardConfigurationMapper dashboardConfigurationMapper;
    private final TransactionDefinitionSetOrderService transactionDefinitionSetOrderService;

    @Override
    public ResponseEntity<SchemaModel> getSchema(String key, Boolean includeChildren) {
        final Optional<SchemaModel> schema =
                schemaService
                        .getSchemaByKey(key)
                        .filter(
                                schemaInstance ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_CERBOS_ACTION, schemaInstance))
                        .map(
                                authorizedSchema ->
                                        dynamicSchemaMapper.schemaToSchemaModel(
                                                authorizedSchema,
                                                Boolean.TRUE.equals(includeChildren)
                                                        ? schemaService.getAllRelatedSchemas(key)
                                                        : null));

        return schema.map(schemaModel -> ResponseEntity.status(200).body(schemaModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<Void> deleteSchema(String key) {
        if (!authorizationHandler.isAllowed(DELETE_CERBOS_ACTION, Schema.class)) {
            throw new ForbiddenException();
        }

        Optional<Schema> schema = schemaService.getSchemaByKey(key);

        if (schema.isEmpty()) {
            throw new NotFoundException("Schema not found: " + key);
        }

        try {
            schemaService.deleteSchema(schema.get());
        } catch (RuntimeException e) {
            throw new ConflictException("Schema is being used and cannot be deleted");
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Failed to delete schema", e);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<FormConfigurationResponseModel>> getListOfFormConfigurations(
            String transactionDefinitionKey) {
        return transactionDefinitionService
                .getTransactionDefinitionByKey(transactionDefinitionKey)
                .filter(
                        authorizationHandler.getAuthFilter(
                                VIEW_CERBOS_ACTION, TransactionDefinition.class))
                .map(
                        definition ->
                                formConfigurationService
                                        .getFormConfigurationsByTransactionDefinitionKey(
                                                definition.getKey())
                                        .stream()
                                        .filter(
                                                authorizationHandler.getAuthFilter(
                                                        VIEW_CERBOS_ACTION,
                                                        FormConfiguration.class))
                                        .map(
                                                FormConfigurationMapper.INSTANCE
                                                        ::mapFormConfigurationToModel)
                                        .toList())
                .map(results -> ResponseEntity.status(200).body(results))
                .orElse(ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<PagedSchemaModel> getSchemas(
            String name,
            String key,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, Schema.class)) {
            throw new ForbiddenException();
        }

        Page<SchemaModel> results =
                schemaService
                        .getSchemasByFilters(
                                new SchemaFilters(
                                        name, key, sortBy, sortOrder, pageNumber, pageSize))
                        .map(dynamicSchemaMapper::schemaToSchemaModel);

        PagedSchemaModel response = new PagedSchemaModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<SchemaModel> updateSchema(
            String key, SchemaUpdateModel schemaUpdateModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, Schema.class)) {
            throw new ForbiddenException();
        }

        UUID schemaId;
        Optional<Schema> existingSchema = schemaService.getSchemaByKey(key);
        if (existingSchema.isEmpty()) {
            throw new NotFoundException("Schema not found");
        }
        schemaId = existingSchema.get().getId();
        schemaService.saveSchema(
                dynamicSchemaMapper.schemaUpdateModelToSchema(schemaUpdateModel, key, schemaId));

        Schema schema =
                schemaService
                        .getSchemaByKey(key)
                        .orElseThrow(
                                () -> new UnexpectedException("Schema not found after saving"));

        SchemaModel schemaModel = dynamicSchemaMapper.schemaToSchemaModel(schema);

        return ResponseEntity.status(200).body(schemaModel);
    }

    @Override
    public ResponseEntity<SchemaModel> createSchema(SchemaCreateModel schemaCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, Schema.class)) {
            throw new ForbiddenException();
        }

        if (schemaService.getSchemaByKey(schemaCreateModel.getKey()).isPresent()) {
            throw new ConflictException("Schema already exists");
        }

        Schema schema =
                schemaService.saveSchema(
                        dynamicSchemaMapper.schemaCreateModelToSchema(schemaCreateModel));

        SchemaModel schemaModel = dynamicSchemaMapper.schemaToSchemaModel(schema);

        return ResponseEntity.status(200).body(schemaModel);
    }

    @Override
    public ResponseEntity<ParentSchemas> getSchemaParents(String key) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, Schema.class)) {
            throw new ForbiddenException();
        }

        Schema schema =
                schemaService
                        .getSchemaByKey(key)
                        .orElseThrow(() -> new NotFoundException("Schema not found: " + key));

        List<String> parents =
                schemaService.getSchemaParents(schema.getKey()).stream()
                        .map(Schema::getKey)
                        .toList();
        ParentSchemas response = new ParentSchemas();
        response.setParentSchemas(parents);

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<Resource> exportConfiguration() {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }

        String zipFileName =
                String.format("backup-%s.zip", ConfigurationUtility.getImportTimestampString());

        try {
            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            String.format("attachment; filename=\"%s\"", zipFileName))
                    .contentType(MediaType.valueOf("application/zip"))
                    .body(configurationUtility.getConfiguration());
        } catch (IOException e) {
            throw new NotFoundException("Failed to get Configuration", e);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<TransactionDefinitionResponseModel> getTransactionDefinition(String key) {
        final Optional<TransactionDefinitionResponseModel> transactionDefinition =
                transactionDefinitionService
                        .getTransactionDefinitionByKey(key)
                        .filter(
                                definition ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_CERBOS_ACTION, definition))
                        .map(
                                TransactionDefinitionMapper.INSTANCE
                                        ::transactionDefinitionToResponseModel);

        return transactionDefinition
                .map(
                        transactionDefinitionResponseModel ->
                                ResponseEntity.status(200).body(transactionDefinitionResponseModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<PagedTransactionDefinitionResponseModel> getTransactionDefinitions(
            String name, String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {

        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, TransactionDefinition.class)) {
            throw new ForbiddenException();
        }

        TransactionDefinitionFilters filters =
                new TransactionDefinitionFilters(name, sortBy, sortOrder, pageNumber, pageSize);

        Page<TransactionDefinitionResponseModel> results =
                transactionDefinitionService
                        .getTransactionDefinitionsByFilters(filters)
                        .map(
                                TransactionDefinitionMapper.INSTANCE
                                        ::transactionDefinitionToResponseModel);

        PagedTransactionDefinitionResponseModel response =
                new PagedTransactionDefinitionResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<TransactionDefinitionResponseModel> putTransactionDefinition(
            String key, TransactionDefinitionUpdateModel transactionDefinitionModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, TransactionDefinition.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinition> existingTransactionDefinition =
                transactionDefinitionService.getTransactionDefinitionByKey(key);

        if (existingTransactionDefinition.isEmpty()) {
            throw new NotFoundException(TRANSACTION_NOT_FOUND_MSG);
        }

        // Set key from request
        final TransactionDefinition transactionDefinition =
                TransactionDefinitionMapper.INSTANCE.updateModelToTransactionDefinition(
                        transactionDefinitionModel);
        transactionDefinition.setKey(key);

        // ensure that any existing ID is used for the given key
        transactionDefinition.setId(existingTransactionDefinition.get().getId());
        transactionDefinition.setCreatedBy(existingTransactionDefinition.get().getCreatedBy());

        if (transactionDefinition.getFormConfigurations() == null
                && existingTransactionDefinition.get().getFormConfigurations() != null) {
            transactionDefinition.setFormConfigurations(
                    existingTransactionDefinition.get().getFormConfigurations());
        }

        if (transactionDefinition.getSubjectType() == null
                && existingTransactionDefinition.get().getSubjectType() != null) {
            transactionDefinition.setSubjectType(
                    existingTransactionDefinition.get().getSubjectType());
        }

        if (transactionDefinition.getAllowedRelatedPartyTypes() == null
                && existingTransactionDefinition.get().getAllowedRelatedPartyTypes() != null) {
            transactionDefinition.setAllowedRelatedPartyTypes(
                    existingTransactionDefinition.get().getAllowedRelatedPartyTypes());
        }

        if (transactionDefinition.getTransactionDefinitionSetKey() == null
                && existingTransactionDefinition.get().getTransactionDefinitionSetKey() != null) {
            transactionDefinition.setTransactionDefinitionSetKey(
                    existingTransactionDefinition.get().getTransactionDefinitionSetKey());
        }

        if (transactionDefinition.getDefaultFormConfigurationKey() != null) {
            FormConfiguration formConfiguration =
                    formConfigurationService
                            .getFormConfigurationByKey(
                                    transactionDefinition.getDefaultFormConfigurationKey())
                            .orElseThrow(
                                    () ->
                                            new NotFoundException(
                                                    "Default form configuration not found"));

            transactionDefinition.addFormConfiguration(formConfiguration);
        }

        transactionDefinition.setCreatedTimestamp(
                existingTransactionDefinition.get().getCreatedTimestamp());

        // validate link with transaction definition set
        transactionDefinitionService.validateTransactionDefinitionSetLink(transactionDefinition);

        final TransactionDefinition updated =
                transactionDefinitionService.saveTransactionDefinition(transactionDefinition);

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionMapper.INSTANCE.transactionDefinitionToResponseModel(
                                updated));
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> postFormConfiguration(
            String transactionDefinitionKey,
            FormConfigurationCreateModel formConfigurationCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        Optional<FormConfiguration> existingFormConfig =
                formConfigurationService.getFormConfigurationByKey(
                        formConfigurationCreateModel.getKey());

        if (existingFormConfig.isPresent()) {
            throw new ConflictException("Form configuration already exists");
        }

        TransactionDefinition transactionDefinition =
                transactionDefinitionService
                        .getTransactionDefinitionByKey(transactionDefinitionKey)
                        .orElseThrow(() -> new NotFoundException(TRANSACTION_NOT_FOUND_MSG));

        final FormConfiguration formConfiguration =
                FormConfigurationMapper.INSTANCE.mapCreationModelToFormConfiguration(
                        formConfigurationCreateModel);
        formConfiguration.setTransactionDefinition(transactionDefinition);

        return ResponseEntity.status(200)
                .body(
                        FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(
                                formConfigurationService.saveFormConfiguration(formConfiguration)));
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> getFormConfiguration(
            String transactionDefinitionKey, String formKey) {
        final Optional<FormConfigurationResponseModel> result =
                formConfigurationService
                        .getFormConfigurationByKeys(transactionDefinitionKey, formKey)
                        .filter(
                                formConfiguration ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_CERBOS_ACTION, formConfiguration))
                        .map(FormConfigurationMapper.INSTANCE::mapFormConfigurationToModel);

        return result.map(
                        formConfigurationResponseModel ->
                                ResponseEntity.status(200).body(formConfigurationResponseModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> putFormConfiguration(
            String transactionDefinitionKey,
            String formKey,
            FormConfigurationUpdateModel formConfigurationUpdateModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        Optional<FormConfiguration> existingFormConfig =
                formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, formKey);

        if (existingFormConfig.isEmpty()) {
            throw new NotFoundException(FORM_CONFIG_NOT_FOUND_MSG);
        }

        TransactionDefinition transactionDefinition =
                transactionDefinitionService
                        .getTransactionDefinitionByKey(transactionDefinitionKey)
                        .orElseThrow(() -> new NotFoundException(TRANSACTION_NOT_FOUND_MSG));

        final FormConfiguration formConfiguration =
                FormConfigurationMapper.INSTANCE.mapModelToFormConfiguration(
                        formConfigurationUpdateModel);
        formConfiguration.setTransactionDefinitionKey(transactionDefinition.getKey());
        formConfiguration.setKey(formKey);

        return ResponseEntity.ok(
                FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(
                        formConfigurationService.saveFormConfiguration(formConfiguration)));
    }

    @Override
    public ResponseEntity<Void> deleteFormConfiguration(
            String transactionDefinitionKey, String key) {
        if (!authorizationHandler.isAllowed(DELETE_CERBOS_ACTION, FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        Optional<FormConfiguration> existingFormConfig =
                formConfigurationService.getFormConfigurationByKeys(transactionDefinitionKey, key);

        if (existingFormConfig.isEmpty()) {
            throw new NotFoundException(FORM_CONFIG_NOT_FOUND_MSG);
        }

        if (transactionDefinitionService.isActiveFormConfiguration(transactionDefinitionKey, key)) {
            throw new ConflictException("Form configuration is being used and cannot be deleted");
        }

        existingFormConfig.get().setTransactionDefinition(null);
        existingFormConfig.get().setRecordDefinition(null);
        formConfigurationService.saveFormConfiguration(existingFormConfig.get());

        try {
            formConfigurationService.deleteFormConfiguration(existingFormConfig.get());
        } catch (RuntimeException e) {
            throw new ConflictException("Form configuration is being used and cannot be deleted");
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<TransactionDefinitionResponseModel> postTransactionDefinition(
            TransactionDefinitionCreateModel transactionDefinitionCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionDefinition.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinition> existingTransactionDefinition =
                transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinitionCreateModel.getKey());

        if (existingTransactionDefinition.isPresent()) {
            throw new ConflictException("Transaction definition already exists");
        }

        final TransactionDefinition transactionDefinition =
                TransactionDefinitionMapper.INSTANCE.createModelToTransactionDefinition(
                        transactionDefinitionCreateModel);

        transactionDefinitionService.validateTransactionDefinitionSetLink(transactionDefinition);

        if (transactionDefinition.getFormConfigurations() == null) {
            FormConfiguration formConfiguration =
                    createDefaultFormConfiguration(transactionDefinition);

            transactionDefinition.addFormConfiguration(formConfiguration);
        }

        TransactionDefinition savedTransaction =
                transactionDefinitionService.saveTransactionDefinition(transactionDefinition);

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionMapper.INSTANCE.transactionDefinitionToResponseModel(
                                savedTransaction));
    }

    @Override
    public ResponseEntity<TransactionLinkTypeModel> postTransactionLinkType(
            TransactionLinkTypeModel transactionLinkTypeModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionLinkType.class)) {
            throw new ForbiddenException();
        }

        final TransactionLinkType transactionLinkType =
                transactionLinkTypeService.saveTransactionLinkType(
                        TransactionLinkTypeMapper.INSTANCE
                                .transactionLinkTypeModelToTransactionLinkType(
                                        transactionLinkTypeModel));

        return ResponseEntity.status(201)
                .body(
                        TransactionLinkTypeMapper.INSTANCE
                                .transactionLinkTypeToTransactionLinkTypeModel(
                                        transactionLinkType));
    }

    @Override
    public ResponseEntity<List<TransactionLinkTypeModel>> getTransactionLinkTypes() {
        final List<TransactionLinkTypeModel> results =
                transactionLinkTypeService.getTransactionLinkTypes().stream()
                        .filter(
                                authorizationHandler.getAuthFilter(
                                        VIEW_CERBOS_ACTION, TransactionLinkType.class))
                        .map(
                                TransactionLinkTypeMapper.INSTANCE
                                        ::transactionLinkTypeToTransactionLinkTypeModel)
                        .toList();

        return ResponseEntity.status(200).body(results);
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetResponseModel> getTransactionSet(String key) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, "transaction_definition_set")) {
            throw new ForbiddenException();
        }
        return transactionDefinitionSetService
                .getTransactionDefinitionSet(key)
                .map(
                        TransactionDefinitionSetMapper.INSTANCE
                                ::transactionDefinitionSetToResponseModel)
                .map(
                        transactionDefinitionSetResponseModel ->
                                ResponseEntity.status(200)
                                        .body(transactionDefinitionSetResponseModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<RecordDefinitionResponseModel> postRecordDefinition(
            RecordDefinitionCreateModel recordDefinitionCreateModel) {

        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, RecordDefinition.class)) {
            throw new ForbiddenException();
        }

        Optional<RecordDefinition> existingRecordDefinition =
                recordDefinitionService.getRecordDefinitionByKey(
                        recordDefinitionCreateModel.getKey());

        if (existingRecordDefinition.isPresent()) {
            throw new ConflictException("Record definition already exists");
        }

        RecordDefinition toSaveRecordDef =
                RecordDefinitionMapper.INSTANCE.createModelToRecordDefinition(
                        recordDefinitionCreateModel);

        RecordDefinition savedRecordDef =
                recordDefinitionService.saveRecordDefinition(toSaveRecordDef);

        return ResponseEntity.ok(
                RecordDefinitionMapper.INSTANCE.recordDefinitionToResponseModel(savedRecordDef));
    }

    @Override
    public ResponseEntity<PagedRecordDefinitionResponseModel> getRecordDefinitions(
            String name, String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, RecordDefinition.class)) {
            throw new ForbiddenException();
        }

        RecordDefinitionFilters filters =
                new RecordDefinitionFilters(name, sortBy, sortOrder, pageNumber, pageSize);

        Page<RecordDefinitionResponseModel> results =
                recordDefinitionService
                        .getRecordDefinitionsByFilters(filters)
                        .map(RecordDefinitionMapper.INSTANCE::recordDefinitionToResponseModel);

        PagedRecordDefinitionResponseModel response = new PagedRecordDefinitionResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<RecordDefinitionResponseModel> getRecordDefinition(String key) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, RecordDefinition.class)) {
            throw new ForbiddenException();
        }

        return recordDefinitionService
                .getRecordDefinitionByKey(key)
                .map(RecordDefinitionMapper.INSTANCE::recordDefinitionToResponseModel)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<RecordDefinitionResponseModel> putRecordDefinition(
            String key, RecordDefinitionUpdateModel recordDefinitionUpdateModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, RecordDefinition.class)) {
            throw new ForbiddenException();
        }

        Optional<RecordDefinition> existingRecordDefinitionOpt =
                recordDefinitionService.getRecordDefinitionByKey(key);

        if (existingRecordDefinitionOpt.isEmpty()) {
            throw new NotFoundException(RECORD_DEF_NOT_FOUND_MSG);
        }
        RecordDefinition existingRecordDefinition = existingRecordDefinitionOpt.get();

        final RecordDefinition recordDefinition =
                RecordDefinitionMapper.INSTANCE.updateModelToRecordDefinition(
                        recordDefinitionUpdateModel);

        recordDefinition.setKey(key);
        recordDefinition.setId(existingRecordDefinition.getId());
        recordDefinitionService.saveRecordDefinition(recordDefinition);

        RecordDefinition returnRecordDefinition =
                recordDefinitionService
                        .getRecordDefinitionById(existingRecordDefinition.getId())
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "Record definition not found after saving"));

        return ResponseEntity.ok(
                RecordDefinitionMapper.INSTANCE.recordDefinitionToResponseModel(
                        returnRecordDefinition));
    }

    @Override
    public ResponseEntity<PagedTransactionDefinitionSetModel> getTransactionSets(
            String sortOrder, Integer pageNumber, Integer pageSize) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, "transaction_definition_set")) {
            throw new ForbiddenException();
        }
        TransactionDefinitionSetFilter filters =
                new TransactionDefinitionSetFilter(sortOrder, pageNumber, pageSize);

        final Page<TransactionDefinitionSetResponseModel> results =
                transactionDefinitionSetService
                        .getAllTransactionDefinitionSets(filters)
                        .map(
                                TransactionDefinitionSetMapper.INSTANCE
                                        ::transactionDefinitionSetToResponseModel);

        PagedTransactionDefinitionSetModel response = new PagedTransactionDefinitionSetModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetResponseModel> putTransactionSet(
            String key, TransactionDefinitionSetUpdateModel transactionDefinitionSetRequestModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionDefinitionSet.class)) {
            throw new ForbiddenException();
        }
        Optional<TransactionDefinitionSet> existingTransactionDefinitionSet =
                transactionDefinitionSetService.getTransactionDefinitionSet(key);

        if (existingTransactionDefinitionSet.isEmpty()) {
            throw new NotFoundException("Transaction definition set not found");
        }

        TransactionDefinitionSet saved =
                transactionDefinitionSetService.save(
                        key,
                        TransactionDefinitionSetMapper.INSTANCE
                                .updateModelToTransactionDefinitionSet(
                                        transactionDefinitionSetRequestModel));

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionSetMapper.INSTANCE
                                .transactionDefinitionSetToResponseModel(saved));
    }

    @Override
    public ResponseEntity<Void> deleteTransactionSet(String key) {
        if (!authorizationHandler.isAllowed(DELETE_CERBOS_ACTION, TransactionDefinitionSet.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinitionSet> transactionDefinitionSet =
                transactionDefinitionSetService.getTransactionDefinitionSet(key);

        if (transactionDefinitionSet.isEmpty()) {
            throw new NotFoundException("Transaction definition set not found: " + key);
        }

        try {
            transactionDefinitionSetService.deleteTransactionDefinitionSet(
                    transactionDefinitionSet.get());
        } catch (RuntimeException e) {
            throw new ConflictException(
                    "Transaction definition set is being used and cannot be deleted");
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetResponseModel> postTransactionSet(
            TransactionDefinitionSetCreateModel transactionDefinitionSetCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionDefinitionSet.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinitionSet> existingTransactionDefinitionSet =
                transactionDefinitionSetService.getTransactionDefinitionSet(
                        transactionDefinitionSetCreateModel.getKey());

        if (existingTransactionDefinitionSet.isPresent()) {
            throw new ConflictException("Transaction definition set already exists");
        }

        TransactionDefinitionSet saved =
                transactionDefinitionSetService.save(
                        transactionDefinitionSetCreateModel.getKey(),
                        TransactionDefinitionSetMapper.INSTANCE
                                .createModelToTransactionDefinitionSet(
                                        transactionDefinitionSetCreateModel));

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionSetMapper.INSTANCE
                                .transactionDefinitionSetToResponseModel(saved));
    }

    @Override
    public ResponseEntity<AllowedLinkModel> postAllowedLinkToDefinition(
            AllowedLinkCreationRequest request) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, AllowedLink.class)) {
            throw new ForbiddenException();
        }

        final AllowedLink allowedLink =
                allowedLinkService.saveAllowedLink(
                        AllowedLinkMapper.INSTANCE.allowedLinkRequestToAllowedLink(request),
                        request.getTransactionLinkTypeId());

        return ResponseEntity.status(201)
                .body(AllowedLinkMapper.INSTANCE.allowedLinkToAllowedLinkModel(allowedLink));
    }

    @Override
    public ResponseEntity<List<AllowedLinkModel>> getTransactionDefinitionAllowedLinksByKey(
            String key) {
        final List<AllowedLinkModel> results =
                allowedLinkService.getAllowedLinksByDefinitionKey(key).stream()
                        .filter(
                                authorizationHandler.getAuthFilter(
                                        VIEW_CERBOS_ACTION, AllowedLink.class))
                        .map(AllowedLinkMapper.INSTANCE::allowedLinkToAllowedLinkModel)
                        .toList();

        return ResponseEntity.status(200).body(results);
    }

    @Override
    public ResponseEntity<List<String>> consistencyCheck() {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, "admin_dashboard")) {
            throw new ForbiddenException();
        }

        final List<String> results = consistencyChecker.check();
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<PagedWorkflowModel> listWorkflows(
            String sortOrder, Integer pageNumber, Integer pageSize) {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<WorkflowModel> workflowModels =
                workflowTasksService
                        .getAllWorkflows(pageable, sortOrder)
                        .map(workflowAndTaskMapper::processDefinitionToWorkflowModel);
        return ResponseEntity.ok(generatePagedWorkflowModel(workflowModels));
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflowByProcessDefinitionKey(
            String processDefinitionKey) {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                workflowAndTaskMapper.processDefinitionToWorkflowModel(
                        workflowTasksService.getSingleWorkflow(processDefinitionKey)));
    }

    @Override
    public ResponseEntity<List<TaskModel>> getUsersTasksByProcessDefinitionKey(
            String processDefinitionKey) {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                workflowTasksService
                        .getListOfTasksByProcessDefinitionKey(processDefinitionKey)
                        .stream()
                        .map(workflowAndTaskMapper::userTaskToTaskModel)
                        .toList());
    }

    @Override
    public ResponseEntity<List<TransactionDefinitionSetDashboardResultModel>> getDashboards() {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, DASHBOARD_CONFIGURATION)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                dashboardConfigurationService.getAllDashboards().stream()
                        .map(
                                dashboardConfiguration -> {
                                    List<String> transactionDefinitionKeys =
                                            transactionDefinitionService
                                                    .getTransactionDefinitionsBySetKey(
                                                            dashboardConfiguration
                                                                    .getTransactionDefinitionSet()
                                                                    .getKey())
                                                    .stream()
                                                    .map(TransactionDefinition::getKey)
                                                    .toList();
                                    return dashboardConfigurationMapper
                                            .dashboardConfigurationToDashboardResultModel(
                                                    dashboardConfiguration,
                                                    transactionDefinitionKeys);
                                })
                        .toList());
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetDashboardResultModel>
            getDashboardByTransactionSetKey(String key) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, DASHBOARD_CONFIGURATION)) {
            throw new ForbiddenException();
        }

        DashboardConfiguration dashboardConfiguration =
                dashboardConfigurationService.getDashboardByTransactionSetKey(key);
        List<String> transactionDefinitionKeys =
                transactionDefinitionService
                        .getTransactionDefinitionsBySetKey(
                                dashboardConfiguration.getTransactionDefinitionSet().getKey())
                        .stream()
                        .map(TransactionDefinition::getKey)
                        .toList();

        return ResponseEntity.ok(
                dashboardConfigurationMapper.dashboardConfigurationToDashboardResultModel(
                        dashboardConfiguration, transactionDefinitionKeys));
    }

    @Override
    public ResponseEntity<List<String>> getDashboardOrder() {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, DASHBOARD_CONFIGURATION)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                transactionDefinitionSetOrderService.getTransactionDefinitionSetOrderAsString());
    }

    @Override
    public ResponseEntity<Void> updateDashboardOrder(List<String> newOrder) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, DASHBOARD_CONFIGURATION)) {
            throw new ForbiddenException();
        }

        transactionDefinitionSetOrderService.updateTransactionSetKeyOrder(newOrder);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<List<DashboardCountsModel>> getTransactionDefinitionSetCounts(
            String transactionSetKey) {
        if (!authorizationHandler.isAllowed(VIEW_CERBOS_ACTION, DASHBOARD_CONFIGURATION)) {
            throw new ForbiddenException();
        }

        List<DashboardCountsModel> result =
                dashboardConfigurationService
                        .countTabsForDashboard(transactionSetKey)
                        .entrySet()
                        .stream()
                        .map(
                                countEntry ->
                                        dashboardConfigurationMapper.mapCount(
                                                countEntry.getKey(), countEntry.getValue()))
                        .toList();

        return ResponseEntity.ok().body(result);
    }

    @Override
    public ResponseEntity<Void> deleteTransactionDefinition(String key) {
        if (!authorizationHandler.isAllowed(DELETE_CERBOS_ACTION, TransactionDefinition.class)) {
            throw new ForbiddenException();
        }

        try {
            transactionDefinitionService.deleteTransactionDefinition(key);
        } catch (RuntimeException e) {
            throw new ConflictException(
                    "This transaction definition is being used and cannot be deleted");
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> postRecordDefinitionFormConfiguration(
            String recordDefinitionKey, FormConfigurationCreateModel formConfigurationCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        Optional<FormConfiguration> existingFormConfig =
                formConfigurationService.getFormConfigurationByKey(
                        formConfigurationCreateModel.getKey());

        if (existingFormConfig.isPresent()) {
            throw new ConflictException("Form configuration already exists");
        }

        RecordDefinition recordDefinition =
                recordDefinitionService
                        .getRecordDefinitionByKey(recordDefinitionKey)
                        .orElseThrow(() -> new NotFoundException(RECORD_DEF_NOT_FOUND_MSG));

        final FormConfiguration formConfiguration =
                FormConfigurationMapper.INSTANCE.mapCreationModelToFormConfiguration(
                        formConfigurationCreateModel);
        formConfiguration.setRecordDefinition(recordDefinition);

        return ResponseEntity.status(200)
                .body(
                        FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(
                                formConfigurationService.saveFormConfiguration(formConfiguration)));
    }

    @Override
    public ResponseEntity<List<FormConfigurationResponseModel>>
            getListOfRecordDefinitionFormConfigurations(String recordDefinitionKey) {
        return recordDefinitionService
                .getRecordDefinitionByKey(recordDefinitionKey)
                .filter(
                        authorizationHandler.getAuthFilter(
                                VIEW_CERBOS_ACTION, RecordDefinition.class))
                .map(
                        definition ->
                                formConfigurationService
                                        .getFormConfigurationsByRecordDefinitionKey(
                                                definition.getKey())
                                        .stream()
                                        .filter(
                                                authorizationHandler.getAuthFilter(
                                                        VIEW_CERBOS_ACTION,
                                                        FormConfiguration.class))
                                        .map(
                                                FormConfigurationMapper.INSTANCE
                                                        ::mapFormConfigurationToModel)
                                        .toList())
                .map(results -> ResponseEntity.status(200).body(results))
                .orElse(ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> putRecordDefinitionFormConfiguration(
            String recordDefinitionKey,
            String formKey,
            FormConfigurationUpdateModel formConfigurationUpdateModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        Optional<FormConfiguration> existingFormConfig =
                formConfigurationService.getFormConfigurationByKeyAndRecordDefinitionKey(
                        recordDefinitionKey, formKey);

        if (existingFormConfig.isEmpty()) {
            throw new NotFoundException(FORM_CONFIG_NOT_FOUND_MSG);
        }

        RecordDefinition recordDefinition =
                recordDefinitionService
                        .getRecordDefinitionByKey(recordDefinitionKey)
                        .orElseThrow(() -> new NotFoundException(RECORD_DEF_NOT_FOUND_MSG));

        FormConfiguration formConfiguration =
                FormConfigurationMapper.INSTANCE.mapModelToFormConfiguration(
                        formConfigurationUpdateModel);
        formConfiguration.setRecordDefinitionKey(recordDefinition.getKey());
        formConfiguration.setKey(formKey);

        formConfigurationService.saveFormConfiguration(formConfiguration);

        formConfiguration =
                formConfigurationService
                        .getFormConfigurationByKeyAndRecordDefinitionKey(
                                recordDefinitionKey, formKey)
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "Form configuration not found after saving"));

        return ResponseEntity.ok(
                FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(formConfiguration));
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> getRecordDefinitionFormConfiguration(
            String recordDefinitionKey, String formKey) {
        final Optional<FormConfigurationResponseModel> result =
                formConfigurationService
                        .getFormConfigurationByKeyAndRecordDefinitionKey(
                                recordDefinitionKey, formKey)
                        .filter(
                                formConfiguration ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_CERBOS_ACTION, formConfiguration))
                        .map(FormConfigurationMapper.INSTANCE::mapFormConfigurationToModel);

        return result.map(
                        formConfigurationResponseModel ->
                                ResponseEntity.status(200).body(formConfigurationResponseModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    private PagedWorkflowModel generatePagedWorkflowModel(Page<WorkflowModel> workflows) {
        PagedWorkflowModel model = new PagedWorkflowModel();
        model.items(workflows.toList());
        model.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(workflows));
        return model;
    }
}
