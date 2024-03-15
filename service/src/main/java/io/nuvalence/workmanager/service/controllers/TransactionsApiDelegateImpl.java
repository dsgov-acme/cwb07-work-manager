package io.nuvalence.workmanager.service.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.audit.AuditableAction;
import io.nuvalence.workmanager.service.audit.transaction.AssignedToChangedAuditHandler;
import io.nuvalence.workmanager.service.audit.transaction.DynamicDataChangedAuditHandler;
import io.nuvalence.workmanager.service.audit.transaction.PriorityChangedAuditHandler;
import io.nuvalence.workmanager.service.audit.transaction.StatusChangedAuditHandler;
import io.nuvalence.workmanager.service.audit.transaction.TransactionNoteChangedAuditHandler;
import io.nuvalence.workmanager.service.config.exceptions.ApiException;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.Note;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReason;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.MissingTaskException;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionDefinitionException;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLink;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkNotAllowedException;
import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.generated.controllers.MyTransactionsApiDelegate;
import io.nuvalence.workmanager.service.generated.controllers.TransactionsApiDelegate;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelRequest;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelResponse;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationRenderModel;
import io.nuvalence.workmanager.service.generated.models.InitiateDocumentProcessingModelRequest;
import io.nuvalence.workmanager.service.generated.models.InitiateDocumentProcessingModelResponse;
import io.nuvalence.workmanager.service.generated.models.LinkedTransaction;
import io.nuvalence.workmanager.service.generated.models.NoteCreationModelRequest;
import io.nuvalence.workmanager.service.generated.models.NoteModelResponse;
import io.nuvalence.workmanager.service.generated.models.NoteUpdateModelRequest;
import io.nuvalence.workmanager.service.generated.models.PagedTransactionModel;
import io.nuvalence.workmanager.service.generated.models.PagedTransactionNoteModel;
import io.nuvalence.workmanager.service.generated.models.TransactionCountByStatusModel;
import io.nuvalence.workmanager.service.generated.models.TransactionCreationRequest;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkModel;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkModificationRequest;
import io.nuvalence.workmanager.service.generated.models.TransactionModel;
import io.nuvalence.workmanager.service.generated.models.TransactionUpdateRequest;
import io.nuvalence.workmanager.service.mapper.CustomerProvidedDocumentMapper;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.mapper.NoteMapper;
import io.nuvalence.workmanager.service.mapper.OffsetDateTimeMapper;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.mapper.TransactionLinkMapper;
import io.nuvalence.workmanager.service.mapper.TransactionMapper;
import io.nuvalence.workmanager.service.models.ByUserTransactionsFilters;
import io.nuvalence.workmanager.service.models.SearchTransactionsFilters;
import io.nuvalence.workmanager.service.models.TransactionFilters;
import io.nuvalence.workmanager.service.models.TransactionNoteFilters;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.service.AuditEventService;
import io.nuvalence.workmanager.service.service.DocumentManagementService;
import io.nuvalence.workmanager.service.service.EmployerUserLinkService;
import io.nuvalence.workmanager.service.service.FormConfigurationService;
import io.nuvalence.workmanager.service.service.IndividualService;
import io.nuvalence.workmanager.service.service.IndividualUserLinkService;
import io.nuvalence.workmanager.service.service.NoteService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionService;
import io.nuvalence.workmanager.service.service.TransactionLinkService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.service.TransactionTaskService;
import io.nuvalence.workmanager.service.service.WorkflowTasksService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.UserUtility;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Controller layer for Transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public class TransactionsApiDelegateImpl
        implements TransactionsApiDelegate, MyTransactionsApiDelegate {

    public static final String TRANSACTION_LINK_CERBOS_ACTION = "transaction_link";
    public static final String UPDATE_ACTN = "update";
    public static final String CREATE_ACTN = "create";
    public static final String TRANSACTION_NOT_FOUND_MSG = "Transaction not found";
    private static final String AUTH_NOT_SET_ERR_MESSAGE =
            "Security context authentication not set.";
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final CustomerProvidedDocumentMapper customerProvidedDocumentMapper;
    private final TransactionService transactionService;
    private final SchemaService schemaService;
    private final DocumentManagementService documentManagementService;
    private final TransactionMapper mapper;
    private final TransactionDefinitionService transactionDefinitionService;
    private final TransactionLinkService transactionLinkService;
    private final WorkflowTasksService workflowTasksService;
    private final EntityMapper entityMapper;
    private final FormConfigurationService formConfigurationService;
    private final PagingMetadataMapper pagingMetadataMapper;

    private final AuthorizationHandler authorizationHandler;
    private final AuditEventService transactionAuditEventService;
    private final RequestContextTimestamp requestContextTimestamp;
    private final UserManagementService userManagementService;
    private final TransactionTaskService transactionTaskService;
    private final NoteService noteService;
    private final NoteMapper noteMapper;
    private final IndividualUserLinkService individualUserLinkService;
    private final EmployerUserLinkService employerUserLinkService;
    private final IndividualService individualService;

    @PreDestroy
    public void preDestroy() {
        executorService.shutdown();
    }

    @Override
    public ResponseEntity<TransactionModel> getTransaction(UUID id) {
        final Optional<TransactionModel> entity =
                transactionService
                        .getTransactionById(id)
                        .filter(
                                transaction ->
                                        authorizationHandler.isAllowedForInstance(
                                                "view", transaction))
                        .map(this::createTransactionModel);

        return entity.map(transactionModel -> ResponseEntity.status(200).body(transactionModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<Map<String, FormConfigurationRenderModel>> getTransactionActiveForms(
            UUID id, String context) {
        if (!authorizationHandler.isAllowed("view", FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        final Transaction transaction =
                transactionService
                        .getTransactionById(id)
                        // TODO Re-enable after fix for Lazy loaded collection in cerbos auth
                        // handler
                        // .filter(t -> authorizationHandler.isAllowedForInstance("view", t))
                        .orElseThrow();
        final Map<String, FormConfiguration> results =
                formConfigurationService.getActiveFormConfiguration(transaction, context);
        final Map<String, FormConfigurationRenderModel> responseBody = new HashMap<>();
        for (Map.Entry<String, FormConfiguration> entry : results.entrySet()) {
            responseBody.put(
                    entry.getKey(),
                    FormConfigurationMapper.INSTANCE.mapFormConfigurationToRenderModel(
                            entry.getValue()));
        }

        return ResponseEntity.ok(responseBody);
    }

    @Override
    public ResponseEntity<Map<String, FormConfigurationRenderModel>> getFirstTasksForms(
            String transactionDefinitionKey, String context) {
        if (!authorizationHandler.isAllowed("view", FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        final TransactionDefinition transactionDef =
                transactionDefinitionService
                        .getTransactionDefinitionByKey(transactionDefinitionKey)
                        .filter(
                                definition ->
                                        authorizationHandler.isAllowedForInstance(
                                                "view", definition))
                        .orElseThrow(
                                () -> new NotFoundException("Transaction definition not found"));

        final Map<String, FormConfiguration> taskToForms =
                formConfigurationService.getFirstTasksFormConfigurations(transactionDef, context);
        final Map<String, FormConfigurationRenderModel> responseBody = new HashMap<>();
        for (Map.Entry<String, FormConfiguration> entry : taskToForms.entrySet()) {
            responseBody.put(
                    entry.getKey(),
                    FormConfigurationMapper.INSTANCE.mapFormConfigurationToRenderModel(
                            entry.getValue()));
        }

        return ResponseEntity.ok(responseBody);
    }

    @Override
    public ResponseEntity<PagedTransactionModel> getTransactionsForAuthenticatedUser(
            String transactionDefinitionKey,
            Boolean isCompleted,
            List<String> status,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        final String userId = SecurityContextUtility.getAuthenticatedUserId();
        List<Individual> individuals =
                individualService.getIndividualsByOwner(UUID.fromString(userId));

        TransactionFilters filters =
                ByUserTransactionsFilters.builder()
                        .subjectProfileId(individuals.stream().map(Individual::getId).toList())
                        .createdBy(userId)
                        .transactionDefinitionKey(transactionDefinitionKey)
                        .isCompleted(isCompleted)
                        .status(status)
                        .sortBy(sortBy)
                        .sortOrder(sortOrder)
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .build();

        Page<TransactionModel> transactions =
                authFilterTransactionsPage(transactionService.getFilteredTransactions(filters))
                        .map(mapper::transactionToTransactionModel);

        return ResponseEntity.ok(generatePagedTransactionModel(transactions));
    }

    @Override
    public ResponseEntity<PagedTransactionModel> getTransactions(
            String transactionDefinitionKey,
            String transactionDefinitionSetKey,
            String category,
            String startDate,
            String endDate,
            List<String> priority,
            List<String> status,
            List<String> assignedTo,
            String subjectUserId,
            UUID subjectProfileId,
            List<UUID> additionalParty,
            String stringProfileType,
            String externalId,
            Boolean assignedToMe,
            UUID xApplicationProfileID,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        ProfileType profileType =
                stringProfileType != null
                        ? ProfileType.fromValue(stringProfileType.toUpperCase(Locale.ROOT))
                        : null;

        assignedTo = getAssignedToList(assignedTo, assignedToMe);
        List<TransactionPriority> priorities = new ArrayList<>();

        if (priority != null) {
            for (String priorityString : priority) {
                priorities.add(TransactionPriority.fromStringValue(priorityString));
            }
        }

        List<String> transactionDefinitionKeysList =
                transactionDefinitionService.createTransactionDefinitionKeysList(
                        transactionDefinitionKey, transactionDefinitionSetKey);

        TransactionFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(transactionDefinitionKeysList)
                        .category(category)
                        .startDate(
                                OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeStartOfDay(startDate))
                        .endDate(OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeEndOfDay(endDate))
                        .priority(priorities)
                        .status(status)
                        .assignedTo(assignedTo)
                        .subjectUserId(subjectUserId)
                        .subjectProfileId(
                                subjectProfileId != null ? List.of(subjectProfileId) : null)
                        .additionalParties(additionalParty)
                        .externalId(externalId)
                        .sortBy(sortBy)
                        .sortOrder(sortOrder)
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .build();

        validatePublicUser(xApplicationProfileID, profileType, filters);

        Page<TransactionModel> results =
                authFilterTransactionsPage(transactionService.getFilteredTransactions(filters))
                        .map(mapper::transactionToTransactionModel);

        return ResponseEntity.ok(generatePagedTransactionModel(results));
    }

    private void validatePublicUser(
            UUID currentProfileId, ProfileType profileType, TransactionFilters filters) {
        if (UserUtility.getAuthenticatedUserType().equalsIgnoreCase(UserType.PUBLIC.getValue())) {
            final String userId = SecurityContextUtility.getAuthenticatedUserId();

            List<UUID> enrolledProfilesIds = getProfilesIds(profileType, userId);
            List<UUID> subjectProfilesIds = new ArrayList<>();
            List<UUID> additionalPartiesIds = new ArrayList<>();

            boolean hasSubjectProfileParam =
                    filters.getSubjectProfileId() != null
                            && !filters.getSubjectProfileId().isEmpty();
            boolean hasAdditionalPartyParam =
                    filters.getAdditionalParties() != null
                            && !filters.getAdditionalParties().isEmpty();

            if (currentProfileId != null && !hasSubjectProfileParam && !hasAdditionalPartyParam) {
                List<UUID> allowedProfileIds =
                        getAllowedProfileIds(enrolledProfilesIds, List.of(currentProfileId));
                filters.setOrOperatedRelatedParties(true);
                subjectProfilesIds = allowedProfileIds;
                additionalPartiesIds = allowedProfileIds;

            } else if (hasSubjectProfileParam || hasAdditionalPartyParam) {
                if (hasSubjectProfileParam) {
                    subjectProfilesIds =
                            getAllowedProfileIds(
                                    enrolledProfilesIds, filters.getSubjectProfileId());
                }
                if (hasAdditionalPartyParam) {
                    additionalPartiesIds =
                            getAllowedProfileIds(
                                    enrolledProfilesIds, filters.getAdditionalParties());
                }
            } else {
                subjectProfilesIds = enrolledProfilesIds;
                additionalPartiesIds = enrolledProfilesIds;
                filters.setOrOperatedRelatedParties(true);
            }
            filters.setSubjectProfileId(subjectProfilesIds);
            filters.setAdditionalParties(additionalPartiesIds);
        }
    }

    @NotNull private List<UUID> getAllowedProfileIds(
            List<UUID> enrolledProfilesIds, List<UUID> toInspectProdilesIds) {
        List<UUID> allowedProfileIds =
                enrolledProfilesIds.stream().filter(toInspectProdilesIds::contains).toList();

        if (allowedProfileIds.isEmpty()) {
            throw new ProvidedDataException(
                    "Provided profile id is not allowed for the current user");
        }
        return allowedProfileIds;
    }

    private List<UUID> getProfilesIds(ProfileType profileType, String userId) {
        List<UUID> profilesIds = new ArrayList<>();
        if (profileType != null) {
            if (profileType.equals(ProfileType.INDIVIDUAL)) {
                profilesIds.addAll(
                        individualUserLinkService.getIndividualLinksByUserId(userId).stream()
                                .map(iul -> iul.getProfile().getId())
                                .toList());
            } else if (profileType.equals(ProfileType.EMPLOYER)) {
                profilesIds.addAll(
                        employerUserLinkService.getEmployerLinksByUserId(userId).stream()
                                .map(eul -> eul.getProfile().getId())
                                .toList());
            }
        } else {
            profilesIds.addAll(
                    individualUserLinkService.getIndividualLinksByUserId(userId).stream()
                            .map(iul -> iul.getProfile().getId())
                            .toList());
            profilesIds.addAll(
                    employerUserLinkService.getEmployerLinksByUserId(userId).stream()
                            .map(eul -> eul.getProfile().getId())
                            .toList());
        }
        return profilesIds;
    }

    @Override
    public ResponseEntity<InitiateDocumentProcessingModelResponse> initiateDocumentProcessing(
            UUID transactionId, InitiateDocumentProcessingModelRequest request) {

        final Transaction transactionInstance =
                transactionService
                        .getTransactionById(transactionId)
                        .filter(
                                transaction ->
                                        authorizationHandler.isAllowedForInstance(
                                                "process-attachments", transaction))
                        .orElseThrow(() -> new NotFoundException(TRANSACTION_NOT_FOUND_MSG));

        log.debug(
                "Initiating document processing for transaction {} with document ids {}",
                transactionId,
                request.getDocuments());

        final Schema schema = transactionInstance.getData().getSchema();
        List<String> processorsNames =
                schemaService.getDocumentProcessorsInSchemaPath(request.getPath(), schema);

        request.getDocuments().stream()
                .map(
                        documentId ->
                                CustomerProvidedDocument.builder()
                                        .id(documentId)
                                        .reviewStatus(ReviewStatus.PENDING)
                                        .transactionId(transactionId)
                                        .dataPath(request.getPath())
                                        .active(true)
                                        .build())
                .forEach(
                        document ->
                                transactionService.saveCustomerProvidedDocumentIfDoesNotExists(
                                        document, transactionId.toString()));

        if (!processorsNames.isEmpty()) {
            request.getDocuments()
                    .forEach(
                            documentId ->
                                    documentManagementService.initiateDocumentProcessing(
                                            documentId, processorsNames));
        }

        InitiateDocumentProcessingModelResponse response =
                new InitiateDocumentProcessingModelResponse();
        response.setProcessors(processorsNames);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<TransactionCountByStatusModel>> getTransactionCountByStatus(
            String transactionDefinitionKey,
            String transactionDefinitionSetKey,
            String category,
            String startDate,
            String endDate,
            List<String> priority,
            List<String> status,
            List<String> assignedTo,
            Boolean assignedToMe) {
        if (!authorizationHandler.isAllowed("view", Transaction.class)) {
            throw new ForbiddenException();
        }

        assignedTo = getAssignedToList(assignedTo, assignedToMe);
        List<TransactionPriority> priorities = new ArrayList<>();

        if (priority != null) {
            for (String priorityString : priority) {
                priorities.add(TransactionPriority.fromStringValue(priorityString));
            }
        }
        List<String> transactionDefinitionKeysList =
                transactionDefinitionService.createTransactionDefinitionKeysList(
                        transactionDefinitionKey, transactionDefinitionSetKey);

        SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(transactionDefinitionKeysList)
                        .category(category)
                        .startDate(
                                OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeStartOfDay(startDate))
                        .endDate(OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeEndOfDay(endDate))
                        .priority(priorities)
                        .status(status)
                        .assignedTo(assignedTo)
                        .build();

        List<TransactionCountByStatusModel> counts =
                transactionService.getTransactionCountsByStatus(filters);
        return ResponseEntity.ok(counts);
    }

    @Override
    public ResponseEntity<TransactionModel> postTransaction(
            TransactionCreationRequest request, String authorization) {
        if (!authorizationHandler.isAllowed(CREATE_ACTN, Transaction.class)) {
            throw new ForbiddenException();
        }

        try {
            final TransactionDefinition definition =
                    transactionDefinitionService
                            .getTransactionDefinitionByKey(request.getTransactionDefinitionKey())
                            .orElseThrow(
                                    () ->
                                            new MissingTransactionDefinitionException(
                                                    request.getTransactionDefinitionKey()));

            final Schema schema =
                    schemaService
                            .getSchemaByKey(definition.getSchemaKey())
                            .orElseThrow(
                                    () -> new MissingSchemaException(definition.getSchemaKey()));

            DynamicEntity data = entityMapper.convertGenericMapToEntity(schema, request.getData());

            Individual individual = individualService.createOrGetIndividualForCurrentUser();

            final Transaction transaction =
                    transactionService.createTransactionWithIndividualSubject(
                            definition, data, individual);

            individualUserLinkService.createAdminUserLinkForProfile(transaction);

            postAuditEventForTransactionCreated(transaction);

            return ResponseEntity.ok(createTransactionModel(transaction));

        } catch (MissingSchemaException e) {
            log.error(
                    String.format(
                            "transaction definition [%s] references missing schema.",
                            request.getTransactionDefinitionKey()),
                    e);
            return ResponseEntity.status(424).build();
        } catch (MissingTransactionDefinitionException e) {
            log.error(
                    String.format(
                            "ID [%s] references missing transaction definition.",
                            request.getTransactionDefinitionKey()),
                    e);
            return ResponseEntity.status(424).build();
        }
    }

    private void postAuditEventForTransactionCreated(Transaction transaction) {
        try {
            transactionService.postAuditEventForTransactionCreated(transaction);

        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "An error has occurred when recording a creation audit event for a"
                                    + " transaction with user id %s for transaction with id %s.",
                            transaction.getCreatedBy(), transaction.getId());
            log.error(errorMessage, e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public ResponseEntity<TransactionModel> updateTransaction(
            final UUID id,
            final TransactionUpdateRequest request,
            final String taskId,
            final Boolean completeTask,
            final String formStepKey) {

        try {
            Transaction existingTransaction = getExistingTransaction(id);

            validateWorkflowPermission(existingTransaction, taskId);

            validateAdminData(existingTransaction, request);

            validateAssignedTo(request);

            Transaction transaction =
                    updateTransactionInternal(
                            id, existingTransaction, request, taskId, formStepKey, completeTask);

            return ResponseEntity.ok(createTransactionModel(transaction));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (UnexpectedException e) {
            return handleExceptions(e);
        }
    }

    @ExceptionHandler({
        MissingSchemaException.class,
        MissingTransactionException.class,
        MissingTaskException.class,
        JsonProcessingException.class,
        Exception.class
    })
    public ResponseEntity<TransactionModel> handleExceptions(Exception e) {
        HttpStatus status;

        if (e.getCause() instanceof MissingSchemaException) {
            status = HttpStatus.FAILED_DEPENDENCY;
        } else if (e.getCause() instanceof MissingTransactionException) {
            status = HttpStatus.NOT_FOUND;
        } else if (e.getCause() instanceof MissingTaskException) {
            status = HttpStatus.FAILED_DEPENDENCY;
        } else if (e.getCause() instanceof JsonProcessingException) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        } else if (e.getCause() instanceof ProvidedDataException) {
            throw (ProvidedDataException) e.getCause();
        } else {
            throw new UnexpectedException(e.getMessage());
        }
        return ResponseEntity.status(status).build();
    }

    private Transaction updateTransactionInternal(
            UUID id,
            Transaction existingTransaction,
            TransactionUpdateRequest request,
            String taskId,
            String formStepKey,
            Boolean completeTask) {
        try {
            final Transaction transaction =
                    AuditableAction.builder(Transaction.class)
                            .auditHandler(
                                    new AssignedToChangedAuditHandler(transactionAuditEventService))
                            .auditHandler(
                                    new PriorityChangedAuditHandler(transactionAuditEventService))
                            .auditHandler(
                                    new StatusChangedAuditHandler(transactionAuditEventService))
                            .auditHandler(
                                    new DynamicDataChangedAuditHandler(
                                            transactionAuditEventService, entityMapper))
                            .requestContextTimestamp(requestContextTimestamp)
                            .action(
                                    transactionIn -> {
                                        final Map<String, Object> mergedMap =
                                                transactionService.unifyAttributeMaps(
                                                        request.getData(),
                                                        entityMapper.convertAttributesToGenericMap(
                                                                transactionIn.getData()));
                                        final Schema schema = transactionIn.getData().getSchema();

                                        TransactionPriority priority = null;

                                        if (request.getPriority() != null) {
                                            priority =
                                                    TransactionPriority.fromStringValue(
                                                            request.getPriority());
                                        }
                                        final Transaction partialUpdate =
                                                Transaction.builder()
                                                        .id(id)
                                                        .assignedTo(request.getAssignedTo())
                                                        .priority(priority)
                                                        .data(
                                                                entityMapper
                                                                        .convertGenericMapToEntity(
                                                                                schema, mergedMap))
                                                        .build();

                                        partialUpdate.setTransactionDefinitionId(
                                                existingTransaction.getTransactionDefinitionId());

                                        if (formStepKey != null) {
                                            transactionService.validateFormStep(
                                                    formStepKey,
                                                    existingTransaction
                                                            .getTransactionDefinitionKey(),
                                                    partialUpdate,
                                                    taskId,
                                                    request.getContext());
                                        }

                                        return (completeTask && taskId != null)
                                                ? transactionService
                                                        .updateTransactionFromPartialUpdateAndCompleteTask(
                                                                partialUpdate,
                                                                taskId,
                                                                request.getAction(),
                                                                schema.getAttributeConfigurations())
                                                : transactionService
                                                        .updateTransactionFromPartialUpdate(
                                                                partialUpdate,
                                                                schema
                                                                        .getAttributeConfigurations());
                                    })
                            .build()
                            .execute(existingTransaction);

            Callable<String> triggerProcessor =
                    new TriggerDocumentProcessor(
                            transaction.getId(),
                            transactionService,
                            schemaService,
                            documentManagementService);
            executorService.submit(triggerProcessor);

            return transaction;
        } catch (MissingSchemaException e) {
            log.error(
                    String.format(
                            "Transaction [%s] contains an entity with missing schema(s).", id),
                    e);
            throw new UnexpectedException(e);
        } catch (MissingTransactionException e) {
            throw new NotFoundException(TRANSACTION_NOT_FOUND_MSG);
        } catch (MissingTaskException e) {
            log.error(
                    String.format(
                            "Unable to find task with key [%s] in transaction with ID %s",
                            taskId, id),
                    e);
            throw new UnexpectedException(e);
        } catch (JsonProcessingException e) {
            log.error(
                    String.format(
                            "Unable to save data for task with key [%s] in transaction with ID %s",
                            taskId, id),
                    e);
            throw new UnexpectedException(e);
        } catch (NuvalenceFormioValidationException e) {
            log.error(
                    String.format(
                            "Unable to save data for task with key [%s] in transaction with ID %s",
                            taskId, id),
                    e);
            throw e;
        } catch (Exception e) {
            log.error("An unexpected exception occurred during transaction update");
            throw new UnexpectedException(e);
        }
    }

    private Transaction getExistingTransaction(UUID id) {
        return transactionService
                .getTransactionById(id)
                .filter(
                        transaction ->
                                authorizationHandler.isAllowedForInstance(UPDATE_ACTN, transaction))
                .orElseThrow(() -> new NotFoundException(TRANSACTION_NOT_FOUND_MSG));
    }

    private void validateWorkflowPermission(Transaction existingTransaction, String taskId) {
        if (!doesWorkflowPermitEdit(existingTransaction, taskId)) {
            throw new ForbiddenException("Permission denied for workflow");
        }
    }

    private void validateAdminData(
            Transaction existingTransaction, TransactionUpdateRequest request) {
        String userType =
                CurrentUserUtility.getCurrentUser().map(UserToken::getUserType).orElse(null);

        if (isInvalidAdminData(userType)
                && transactionService.hasAdminDataChanges(
                        existingTransaction, request.getAssignedTo(), request.getPriority())) {
            throw new ForbiddenException("Permission denied for admin data changes");
        }
    }

    private boolean isInvalidAdminData(String userType) {
        return userType == null
                || !userType.equals(UserType.AGENCY.getValue())
                || !authorizationHandler.isAllowed("update-admin-data", Transaction.class);
    }

    private void validateAssignedTo(TransactionUpdateRequest request) {
        if (!StringUtils.isBlank(request.getAssignedTo())) {
            Optional<User> optionalUser;
            try {
                optionalUser =
                        userManagementService.getUserOptional(
                                UUID.fromString(request.getAssignedTo()));
                if (optionalUser.isEmpty()) {
                    throw new NotFoundException("User not found");
                }
            } catch (HttpClientErrorException e) {
                log.error("An error reaching user management occurred: ", e);
                throw new ApiException("Could not verify user existence");
            }
        }
    }

    @Override
    public ResponseEntity<TransactionLinkModel> linkTransactions(
            UUID id, UUID toId, TransactionLinkModificationRequest request) {
        if (!authorizationHandler.isAllowed(CREATE_ACTN, TRANSACTION_LINK_CERBOS_ACTION)) {
            throw new ForbiddenException();
        }

        try {
            final TransactionLink transactionLink =
                    transactionLinkService.saveTransactionLink(
                            TransactionLinkMapper.INSTANCE.transactionLinkRequestToTransactionLink(
                                    request, id, toId),
                            request.getTransactionLinkTypeId());

            return ResponseEntity.status(201)
                    .body(
                            TransactionLinkMapper.INSTANCE.transactionLinkToTransactionLinkModel(
                                    transactionLink));
        } catch (MissingTransactionDefinitionException e) {
            log.error("transaction contains an entity with missing schema(s).", e);
            return ResponseEntity.status(424).build();
        } catch (TransactionLinkNotAllowedException e) {
            log.error("transactions not allowed to be linked.", e);
            return ResponseEntity.status(400).build();
        } catch (MissingTransactionException e) {
            log.error("Link request references missing transaction.", e);
            return ResponseEntity.status(404).build();
        }
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return TransactionsApiDelegate.super.getRequest();
    }

    @Override
    public ResponseEntity<Void> deleteTransactionLink(
            UUID id, UUID toId, TransactionLinkModificationRequest request) {

        if (!authorizationHandler.isAllowed("delete", TRANSACTION_LINK_CERBOS_ACTION)) {
            throw new ForbiddenException();
        }
        transactionLinkService.removeTransactionLink(request.getTransactionLinkTypeId(), id, toId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<LinkedTransaction>> getLinkedTransactionsById(UUID id) {
        if (!authorizationHandler.isAllowed("view", TRANSACTION_LINK_CERBOS_ACTION)) {
            throw new ForbiddenException();
        }

        final List<LinkedTransaction> results =
                transactionLinkService.getLinkedTransactionsById(id);

        return ResponseEntity.status(200).body(results);
    }

    @Override
    public ResponseEntity<List<String>> getAvailableStatuses(
            String type, String category, String key) {
        if (!authorizationHandler.isAllowed("view", "transaction_config")) {
            throw new ForbiddenException();
        }

        List<String> keys = new ArrayList<>();
        if (key != null) {
            keys = List.of(key);
        }

        final List<String> results = workflowTasksService.getCamundaStatuses(type, category, keys);
        return ResponseEntity.status(200).body(results);
    }

    @Override
    public ResponseEntity<CustomerProvidedDocumentModelResponse> updateCustomerProvidedDocument(
            String transactionId,
            String documentId,
            CustomerProvidedDocumentModelRequest customerProvidedDocumentModelRequest) {
        if (!authorizationHandler.isAllowed(UPDATE_ACTN, "customer_provided_document")) {
            throw new ForbiddenException();
        }

        Transaction transaction = transactionService.getTransactionIfExists(transactionId);

        CustomerProvidedDocument customerProvidedDocument =
                transactionService.getCustomerProvidedDocumentInTransactionById(
                        transaction, documentId);

        List<RejectionReason> rejectionReasons = null;
        if (customerProvidedDocumentModelRequest.getRejectionReasons() != null) {
            rejectionReasons =
                    customerProvidedDocumentModelRequest.getRejectionReasons().stream()
                            .map(
                                    requestRejection ->
                                            RejectionReason.builder()
                                                    .rejectionReasonValue(
                                                            RejectionReasonType.valueOf(
                                                                    requestRejection))
                                                    .build())
                            .toList();
        }
        ReviewStatus existingDocumentStatus = customerProvidedDocument.getReviewStatus();

        customerProvidedDocument.setReviewStatus(
                customerProvidedDocumentModelRequest.getReviewStatus() != null
                        ? ReviewStatus.valueOf(
                                customerProvidedDocumentModelRequest.getReviewStatus())
                        : null);
        if (rejectionReasons != null) {
            customerProvidedDocument.setRejectionReasons(rejectionReasons);
        } else {
            customerProvidedDocument.getRejectionReasons().clear();
        }

        CustomerProvidedDocument savedDocument =
                transactionService.updateCustomerProvidedDocument(
                        customerProvidedDocument, transaction);

        postDocumentChangesAuditEvent(existingDocumentStatus, savedDocument, transaction);

        return ResponseEntity.ok(
                customerProvidedDocumentMapper.customerProvidedDocumentToModel(savedDocument));
    }

    @Override
    public ResponseEntity<NoteModelResponse> getTransactionNote(UUID transactionId, UUID noteId) {
        if (!authorizationHandler.isAllowed("view", TransactionNote.class)) {
            throw new ForbiddenException();
        }
        return ResponseEntity.ok(
                noteMapper.noteToNoteModelResponse(
                        noteService.getByTransactionIdAndId(transactionId, noteId)));
    }

    @Override
    public ResponseEntity<PagedTransactionNoteModel> getTransactionNotes(
            UUID transactionId,
            String startDate,
            String endDate,
            String type,
            Boolean includeDeleted,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("view", TransactionNote.class)) {
            throw new ForbiddenException();
        }

        TransactionNoteFilters filters =
                TransactionNoteFilters.builder()
                        .transactionId(transactionId)
                        .type(type)
                        .sortBy(sortBy)
                        .sortOrder(sortOrder)
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .startDate(
                                OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeStartOfDay(startDate))
                        .endDate(OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeEndOfDay(endDate))
                        .includeDeleted(includeDeleted)
                        .build();

        Page<NoteModelResponse> results =
                noteService
                        .getFilteredTransactionNotes(filters)
                        .map(noteMapper::noteToNoteModelResponse);

        return ResponseEntity.ok().body(generatePagedTransactionNoteModel(results));
    }

    @Override
    public ResponseEntity<NoteModelResponse> postTransactionNote(
            UUID id, NoteCreationModelRequest noteModelRequest) {

        if (!authorizationHandler.isAllowed(CREATE_ACTN, TransactionNote.class)) {
            throw new ForbiddenException();
        }

        if (noteModelRequest.getDocuments() != null) {
            noteModelRequest.getDocuments().stream()
                    .forEach(
                            document -> {
                                if (document == null) {
                                    throw new BusinessLogicException(
                                            "Provided document ids could not be mapped to UUIDs");
                                }
                            });
        }
        Note note =
                noteService.createTransactionNote(
                        id,
                        noteMapper.noteModelRequestToNote(noteModelRequest),
                        noteModelRequest.getType().getId());

        NoteModelResponse noteResponse = noteMapper.noteToNoteModelResponse(note);

        try {
            noteService.postAuditEventForTransactionNote(id, note, AuditActivityType.NOTE_ADDED);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "An error has occurred when recording an audit event for note creation"
                                    + " with id %s for transaction with id %s.",
                            note.getId(), id);
            log.error(errorMessage, e);
        }

        return ResponseEntity.ok(noteResponse);
    }

    @Override
    public ResponseEntity<NoteModelResponse> updateTransactionNote(
            UUID transactionId, UUID noteId, NoteUpdateModelRequest request) {

        if (!authorizationHandler.isAllowed(UPDATE_ACTN, TransactionNote.class)) {
            throw new ForbiddenException();
        }

        Note requestNote = noteMapper.noteUpdateModelRequestToNote(request);
        requestNote.setId(noteId);

        TransactionNote existingNote =
                noteService.getByTransactionIdAndId(transactionId, requestNote.getId());

        try {
            final TransactionNote updatedTransactionNote =
                    AuditableAction.builder(TransactionNote.class)
                            .auditHandler(
                                    new TransactionNoteChangedAuditHandler(
                                            transactionAuditEventService))
                            .requestContextTimestamp(requestContextTimestamp)
                            .action(
                                    transactionNoteIn ->
                                            noteService.updateTransactionNote(
                                                    existingNote,
                                                    requestNote,
                                                    request.getType().getId()))
                            .build()
                            .execute(existingNote);

            NoteModelResponse noteResponse =
                    noteMapper.noteToNoteModelResponse(updatedTransactionNote);

            return ResponseEntity.ok(noteResponse);

        } catch (Exception e) {
            throw new UnexpectedException("", e);
        }
    }

    @Override
    public ResponseEntity<Void> softDeleteTransactionNote(UUID transactionId, UUID noteId) {
        if (!authorizationHandler.isAllowed("delete", TransactionNote.class)) {
            throw new ForbiddenException();
        }

        Note note = noteService.softDeleteTransactionNote(transactionId, noteId);

        try {
            noteService.postAuditEventForTransactionNote(
                    transactionId, note, AuditActivityType.NOTE_DELETED);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "An error has occurred when recording an audit event for note deletion"
                                    + " with id %s for transaction with id %s.",
                            noteId, transactionId);
            log.error(errorMessage, e);
        }

        return ResponseEntity.ok().build();
    }

    private PagedTransactionNoteModel generatePagedTransactionNoteModel(
            Page<NoteModelResponse> transactionNotes) {
        PagedTransactionNoteModel model = new PagedTransactionNoteModel();
        model.items(transactionNotes.toList());
        model.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(transactionNotes));
        return model;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private void postDocumentChangesAuditEvent(
            ReviewStatus beforeValue, CustomerProvidedDocument after, Transaction transaction) {

        ReviewStatus afterValue = after.getReviewStatus();

        if (beforeValue != null && afterValue != null && !beforeValue.equals(afterValue)) {

            AuditActivityType auditActivityType = null;
            String summary = null;
            Map<String, String> flattenData =
                    entityMapper.flattenDynaDataMap(transaction.getData());
            String documentFieldPath =
                    flattenData.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(after.getId().toString()))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);

            try {
                if (afterValue.equals(ReviewStatus.ACCEPTED)) {
                    summary = "Document accepted";
                    auditActivityType = AuditActivityType.DOCUMENT_ACCEPTED;
                } else if (afterValue.equals(ReviewStatus.NEW)
                        && beforeValue.equals(ReviewStatus.ACCEPTED)) {
                    summary = "Document unaccepted";
                    auditActivityType = AuditActivityType.DOCUMENT_UNACCEPTED;
                } else if (afterValue.equals(ReviewStatus.REJECTED)) {
                    summary = "Document rejected";
                    auditActivityType = AuditActivityType.DOCUMENT_REJECTED;
                } else if (afterValue.equals(ReviewStatus.NEW)
                        && beforeValue.equals(ReviewStatus.REJECTED)) {
                    summary = "Document unrejected";
                    auditActivityType = AuditActivityType.DOCUMENT_UNREJECTED;
                }

                transactionService.postAuditEventForDocumentStatusChanged(
                        after, documentFieldPath, auditActivityType, summary);
            } catch (Exception e) {
                String errorMessage =
                        String.format(
                                "An error has occurred when recording a change audit event for a"
                                        + " document with id %s for transaction with id %s,"
                                        + " activityType: %s",
                                after.getId(), after.getTransactionId());
                log.error(errorMessage, e);
            }
        }
    }

    private TransactionModel createTransactionModel(Transaction t) {

        if (t.getTransactionDefinition() == null) {
            t = transactionService.getTransactionById(t.getId()).orElseThrow();
        }

        return mapper.transactionToTransactionModel(t);
    }

    private List<String> getAssignedToList(List<String> assignedTo, Boolean assignedToMe) {
        if (assignedToMe != null && assignedToMe) {
            try {
                Authentication authentication =
                        SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof UserToken userToken) {
                    final Optional<String> userId =
                            Optional.ofNullable(userToken.getApplicationUserId());

                    if (userId.isPresent()) {
                        assignedTo = List.of(userId.get());
                    }
                }
                throw new ForbiddenException();
            } catch (ForbiddenException e) {
                log.error(AUTH_NOT_SET_ERR_MESSAGE);
            }
        }
        return assignedTo;
    }

    private PagedTransactionModel generatePagedTransactionModel(
            Page<TransactionModel> transactions) {
        PagedTransactionModel model = new PagedTransactionModel();
        model.items(transactions.toList());
        model.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(transactions));
        return model;
    }

    private Page<Transaction> authFilterTransactionsPage(Page<Transaction> transactions) {

        List<Transaction> transactionList =
                transactions.getContent().stream()
                        .filter(t -> authorizationHandler.isAllowedForInstance("view", t))
                        .toList();

        return new PageImpl<>(
                transactionList, transactions.getPageable(), transactions.getTotalElements());
    }

    /*
     * Validates that a user has access to a required workflow task to perform this update.
     *  - If a specific taskId is given, the user must have access to that task
     *  - otherwise the user must have access to any currently active task
     */
    private boolean doesWorkflowPermitEdit(Transaction transaction, String taskId) {
        List<WorkflowTask> userActiveTasks =
                transactionTaskService.getActiveTasksForCurrentUser(transaction);

        return (taskId != null)
                ? userActiveTasks.stream().anyMatch(task -> task.getKey().equals(taskId))
                : !userActiveTasks.isEmpty();
    }
}
