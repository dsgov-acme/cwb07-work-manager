package io.nuvalence.workmanager.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentClassifierConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.MissingTaskException;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.generated.models.TransactionCountByStatusModel;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.models.SearchTransactionsFilters;
import io.nuvalence.workmanager.service.models.TransactionFilters;
import io.nuvalence.workmanager.service.models.auditevents.*;
import io.nuvalence.workmanager.service.repository.CustomerProvidedDocumentRepository;
import io.nuvalence.workmanager.service.repository.TransactionRepository;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import io.nuvalence.workmanager.service.utils.formconfig.formio.NuvalenceFormioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Service for managing transactions.
 */
@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({
    "checkstyle:ClassFanOutComplexity",
    "checkstyle:ClassDataAbstractionCoupling",
    "checkstyle:cyclomaticcomplexity"
})
public class TransactionService {
    private final TransactionRepository repository;

    private final CustomerProvidedDocumentRepository customerProvidedDocumentRepository;

    private final TransactionDefinitionService transactionDefinitionService;
    private final TransactionFactory factory;
    private final TransactionTaskService transactionTaskService;
    private final WorkflowTasksService workflowTasksService;
    private final AuditEventService transactionAuditEventService;

    private final SchemaService schemaService;
    private final RequestContextTimestamp requestContextTimestamp;
    private final FormConfigurationService formConfigurationService;

    private final FormConfigurationMapper formConfigMapper;
    private final NuvalenceFormioValidator formIoValidator;
    private final IndividualService individualService;
    private final EmployerService employerService;

    /**
     * Create a new transaction for a given transaction definition.
     *
     * @param definition Type of transaction to create
     * @return The newly created transaction
     * @throws MissingSchemaException if the transaction definition references a schema that does not exist
     */
    public Transaction createTransaction(final TransactionDefinition definition)
            throws MissingSchemaException {
        return this.createTransaction(definition, null);
    }

    /**
     * Create a new transaction for a given transaction definition.
     *
     * @param definition Type of transaction to create
     * @param jwt        JSON Web Token from HTTP request
     * @return The newly created transaction
     * @throws MissingSchemaException if the transaction definition references a schema that does not exist
     */
    public Transaction createTransaction(final TransactionDefinition definition, String jwt)
            throws MissingSchemaException {

        Transaction savedTransaction = repository.save(factory.createTransaction(definition));
        startTask(savedTransaction, definition.getProcessDefinitionKey());
        return savedTransaction;
    }

    /**
     * Create a new transaction for a given transaction definition and individual.
     *
     * @param definition Type of transaction to create
     * @param data Dynamic transaction data
     * @param individual Individual to associate with transaction
     * @return The newly created transaction
     * @throws MissingSchemaException if the transaction definition references a schema that does not exist
     */
    public Transaction createTransactionWithIndividualSubject(
            final TransactionDefinition definition, final DynamicEntity data, Individual individual)
            throws MissingSchemaException {
        // TODO in the future we may add support for other subject types
        if (!definition.getSubjectType().equals(ProfileType.INDIVIDUAL)) {
            throw new BusinessLogicException(
                    "Subject type is not supported for transaction creation");
        }

        Transaction transaction = factory.createTransaction(definition);
        transaction.setSubjectProfileId(individual.getId());
        transaction.setSubjectProfileType(ProfileType.INDIVIDUAL);
        transaction.setData(data);
        Transaction savedTransaction = repository.save(transaction);
        startTask(savedTransaction, definition.getProcessDefinitionKey());

        return savedTransaction;
    }

    private void startTask(Transaction transaction, String processDefinitionKey) {
        transactionTaskService.startTask(transaction, processDefinitionKey);
    }

    /**
     * Looks up a transaction by ID.
     *
     * @param id ID of transaction to find
     * @return Optional wrapping transaction
     */
    public Optional<Transaction> getTransactionById(final UUID id) {
        return repository.findById(id);
    }

    /**
     * Looks up a transaction by processInstanceID.
     *
     * @param processInstanceId ID of the process instance
     * @return Optional wrapping transaction
     */
    public Optional<Transaction> getTransactionByProcessInstanceId(String processInstanceId) {
        return repository.findByProcessInstanceId(processInstanceId);
    }

    /**
     * Updates the transaction in the database.
     *
     * @param transaction Transaction containing updated data.
     * @return Transaction post-update
     */
    public Transaction updateTransaction(final Transaction transaction) {
        return repository.save(transaction);
    }

    /**
     * Applies a partial update to a given transaction. Currently supports updating:
     * <ul>
     *     <li>assingedTo - if not null</li>
     *     <li>priority - if not null</li>
     *     <li>data - if not null</li>
     * </ul>
     *
     * @param partialUpdate Transaction instance carrying fields to update
     * @param attributeConfigurations Map of attribute configurations
     * @return Updated version of transaction
     * @throws MissingTransactionException If transaction matching update by id does not exist
     */
    public Transaction updateTransactionFromPartialUpdate(
            final Transaction partialUpdate,
            Map<String, List<AttributeConfiguration>> attributeConfigurations)
            throws MissingTransactionException {
        final Transaction transaction =
                getTransactionById(partialUpdate.getId())
                        .orElseThrow(() -> new MissingTransactionException(partialUpdate.getId()));

        if (partialUpdate.getPriority() != null) {
            transaction.setPriority(partialUpdate.getPriority());
        }

        if (partialUpdate.getSubjectProfileType() != null) {
            if (transaction
                    .getTransactionDefinition()
                    .getSubjectType()
                    .equals(partialUpdate.getSubjectProfileType())) {
                transaction.setSubjectProfileType(partialUpdate.getSubjectProfileType());
            } else {
                throw new BusinessLogicException("Not supported subject profile type");
            }
        }

        if (partialUpdate.getSubjectProfileId() != null) {
            boolean isIndividual =
                    transaction.getSubjectProfileType().equals(ProfileType.INDIVIDUAL);
            boolean isEmployer = transaction.getSubjectProfileType().equals(ProfileType.EMPLOYER);
            boolean missingIndividualProfile =
                    isIndividual
                            && individualService
                                    .getIndividualById(partialUpdate.getSubjectProfileId())
                                    .isEmpty();
            boolean missingEmployerProfile =
                    isEmployer
                            && employerService
                                    .getEmployerById(partialUpdate.getSubjectProfileId())
                                    .isEmpty();

            if (missingIndividualProfile) {
                throw new BusinessLogicException("Missing individual profile");
            } else if (missingEmployerProfile) {
                throw new BusinessLogicException("Missing employer profile");
            }

            transaction.setSubjectProfileId(partialUpdate.getSubjectProfileId());
        }

        if (StringUtils.isNotBlank(partialUpdate.getAssignedTo())) {
            transaction.setAssignedTo(partialUpdate.getAssignedTo());
        } else if (partialUpdate.getAssignedTo() != null) {
            // TODO come up with better way to unset assigned To.
            transaction.setAssignedTo(null);
        }

        if (partialUpdate.getData() != null) {
            transaction.setData(partialUpdate.getData());
        }

        updateDocuments(partialUpdate, transaction, attributeConfigurations);

        return updateTransaction(transaction);
    }

    private Map<UUID, String> getAllDocumentsMap(Transaction transaction) {
        Map<UUID, String> mapUuidPath = new HashMap<>();
        DynamicEntity baseDynamicEntity = transaction.getData();
        List<String> path = new ArrayList<>();

        for (DynaProperty dynaProperty : baseDynamicEntity.getSchema().getDynaProperties()) {
            final Object value = baseDynamicEntity.get(dynaProperty.getName());

            if (value != null) {
                processObjectsForDataPathCreation(value, mapUuidPath, path, dynaProperty.getName());
            }
        }

        return mapUuidPath;
    }

    private Object processObjectsForDataPathCreation(
            Object value, Map<UUID, String> mapUuidPath, List<String> path, String attributeName) {
        if (value instanceof DynamicEntity dynamicEntity) {
            return processDynamicEntitiesForDataPathCreation(
                    dynamicEntity, mapUuidPath, path, attributeName);
        } else if (List.class.isAssignableFrom(value.getClass())) {
            return processListsForDataPathCreation(value, mapUuidPath, path, attributeName);
        } else if (value instanceof Document document) {
            String pathString = getPathFromString(path);
            if (!pathString.isEmpty()) {
                pathString += ".";
            }
            mapUuidPath.put((document).getDocumentId(), pathString + attributeName);
        }

        return value;
    }

    private Object processDynamicEntitiesForDataPathCreation(
            DynamicEntity dynamicEntity,
            Map<UUID, String> mapUuidPath,
            List<String> path,
            String attributeName) {
        path.add(attributeName);
        for (DynaProperty dynaProperty : dynamicEntity.getSchema().getDynaProperties()) {
            final Object value = dynamicEntity.get(dynaProperty.getName());
            if (value != null) {
                processObjectsForDataPathCreation(value, mapUuidPath, path, dynaProperty.getName());
            }
        }
        path.remove(attributeName);

        return dynamicEntity;
    }

    private Object processListsForDataPathCreation(
            Object list, Map<UUID, String> mapUuidPath, List<String> path, String attributeName) {
        for (Object o : ((List<?>) list)) {
            processObjectsForDataPathCreation(o, mapUuidPath, path, attributeName);
        }

        return list;
    }

    private String getPathFromString(List<String> stringList) {
        StringJoiner stringJoiner = new StringJoiner("\\.");
        for (String str : stringList) {
            stringJoiner.add(str);
        }
        return stringJoiner.toString();
    }

    private Set<CustomerProvidedDocument> findDocumentsToAdd(
            Transaction transaction,
            Map<UUID, String> documentProperties,
            List<CustomerProvidedDocument> existingDocuments,
            Map<String, List<AttributeConfiguration>> attributeConfigurations) {
        Map<UUID, List<AttributeConfiguration>> mapDocumentIdToAttributeConfiguration =
                mapDocumentIdToAttributeConfiguration(transaction, attributeConfigurations);

        return documentProperties.keySet().stream()
                .filter(
                        dp ->
                                !existingDocuments.stream()
                                        .map(CustomerProvidedDocument::getId)
                                        .toList()
                                        .contains(dp))
                .map(
                        dp -> {
                            Optional<DocumentClassifierConfiguration> classifier =
                                    getClassifier(dp, mapDocumentIdToAttributeConfiguration);
                            return CustomerProvidedDocument.builder()
                                    .id(dp)
                                    .reviewStatus(ReviewStatus.NEW)
                                    .transactionId(transaction.getId())
                                    .dataPath(documentProperties.get(dp))
                                    .active(true)
                                    .classifier(
                                            classifier
                                                    .map(
                                                            DocumentClassifierConfiguration
                                                                    ::getClassifierName)
                                                    .orElse(null))
                                    .build();
                        })
                .collect(Collectors.toSet());
    }

    /**
     * Updates documents for a given transaction.
     *
     * @param partialUpdate Transaction instance carrying documents to update
     * @param transaction Transaction found in db
     * @param attributeConfigurations List of attribute configurations ot be updated
     * @return Updated list of documents
     */
    public List<CustomerProvidedDocument> updateDocuments(
            final Transaction partialUpdate,
            final Transaction transaction,
            Map<String, List<AttributeConfiguration>> attributeConfigurations) {
        if (transaction.getCustomerProvidedDocuments() == null) {
            transaction.setCustomerProvidedDocuments(new ArrayList<>());
        }

        Map<UUID, String> mapOfAllDocuments = getAllDocumentsMap(partialUpdate);

        setDocumentsFromPendingToNew(mapOfAllDocuments);

        Set<CustomerProvidedDocument> documentsToAdd =
                findDocumentsToAdd(
                        transaction,
                        mapOfAllDocuments,
                        transaction.getCustomerProvidedDocuments(),
                        attributeConfigurations);

        Set<UUID> documentsToRemove = new HashSet<>();
        Set<UUID> idsForDocumentsInPartialUpdate = mapOfAllDocuments.keySet();

        for (CustomerProvidedDocument existingDocument :
                transaction.getCustomerProvidedDocuments()) {
            UUID documentId = existingDocument.getId();
            String dataPath = existingDocument.getDataPath();

            if (!idsForDocumentsInPartialUpdate.contains(documentId)
                    || !dataPath.equals(mapOfAllDocuments.get(documentId))) {
                documentsToRemove.add(documentId);
            }
        }

        transaction.getCustomerProvidedDocuments().addAll(documentsToAdd);

        transaction.getCustomerProvidedDocuments().stream()
                .filter(cpd -> documentsToRemove.contains(cpd.getId()))
                .forEach(cpd -> cpd.setActive(false));

        return transaction.getCustomerProvidedDocuments();
    }

    /**
     * Applies a partial update to a given transaction. Currently supports updating:
     * <ul>
     *     <li>assingedTo - if not null</li>
     *     <li>priority - if not null</li>
     *     <li>data - if not null</li>
     * </ul>
     * <p/>
     * Additionally, this method will evaluate a the given task (with action) for completeion.
     *
     * @param partialUpdate Transaction instance carrying fields to update
     * @param taskId ID of task to complete
     * @param action optional workflow action passed that influences decisions in workflow
     * @param attributeConfigurations List of attribute configurations ot be updated
     * @return Updated version of transaction
     * @throws MissingTransactionException If transaction matching update by id does not exist
     * @throws MissingTaskException If the task targeted by taskId doesn't exist in the workflow
     * @throws JsonProcessingException If the data could not be serialized to JSON
     */
    public Transaction updateTransactionFromPartialUpdateAndCompleteTask(
            final Transaction partialUpdate,
            final String taskId,
            final String action,
            Map<String, List<AttributeConfiguration>> attributeConfigurations)
            throws MissingTransactionException, MissingTaskException, JsonProcessingException {
        final Transaction transaction =
                updateTransactionFromPartialUpdate(partialUpdate, attributeConfigurations);
        completeTask(transaction, taskId, action);

        return updateTransaction(transaction);
    }

    /**
     * Completes the given task, posting to the workflow the data in the transaction.
     *
     * @param transaction Transaction to complete task on
     * @param taskId      ID of task to complete
     * @param action   optional workflow action passed that influences decisions in workflow
     * @throws MissingTaskException    If the process instance for this transaction does not have a task matching taskId
     * @throws JsonProcessingException If the data could not be serialized to JSON
     */
    public void completeTask(
            final Transaction transaction, final String taskId, final String action)
            throws MissingTaskException, JsonProcessingException {
        transactionTaskService.completeTask(transaction, taskId, action);
    }

    /**
     * Gets a list of filtered transactions.
     *
     * @param filters What to filter/sort the transactions by
     * @return List of transactions
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public Page<Transaction> getFilteredTransactions(final TransactionFilters filters) {
        Map<String, List<String>> statusMap =
                workflowTasksService.getStatusMap(
                        filters.getCategory(), filters.getTransactionDefinitionKeys());

        filters.setStatus(addInternalStatusesToPublicStatusList(filters.getStatus(), statusMap));

        return repository.findAll(filters.getTransactionSpecifications(), filters.getPageRequest());
    }

    /**
     * Get list of statuses with a count of how many transactions have each status.
     *
     * @param filters What to filter the transactions by
     * @return List of statuses and counts of transactions per status
     */
    public List<TransactionCountByStatusModel> getTransactionCountsByStatus(
            final SearchTransactionsFilters filters) {
        List<String> publicStatuses =
                workflowTasksService.getCamundaStatuses(
                        WorkflowTasksService.StatusType.PUBLIC.name(),
                        filters.getCategory(),
                        filters.getTransactionDefinitionKeys());

        Map<String, List<String>> publicToInternalStatusMap =
                workflowTasksService.getStatusMap(
                        filters.getCategory(), filters.getTransactionDefinitionKeys());

        filters.setStatus(
                addInternalStatusesToPublicStatusList(
                        filters.getStatus(), publicToInternalStatusMap));

        List<TransactionCountByStatusModel> transactionCounters =
                repository.getTransactionCountsByStatus(filters.getTransactionSpecifications());

        // replace the internal status by the public status
        transactionCounters.forEach(
                tc -> {
                    for (Map.Entry<String, List<String>> entry :
                            publicToInternalStatusMap.entrySet()) {
                        if (entry.getValue().stream()
                                .anyMatch(
                                        internalStatus ->
                                                internalStatus.equalsIgnoreCase(tc.getStatus()))) {
                            tc.setStatus(entry.getKey());
                        }
                    }
                });

        // add any public statuses that don't have transactions
        List<TransactionCountByStatusModel> notFoundTransactionCounters = new ArrayList<>();
        publicStatuses.forEach(
                ps -> {
                    // only consider public statuses matching filter statuses
                    if ((filters.getStatus() != null && !filters.getStatus().isEmpty())
                            && (filters.getStatus().stream()
                                    .noneMatch(fs -> fs.equalsIgnoreCase(ps)))) {
                        return;
                    }

                    // if status is not found, add it anyway since we'll need to know the count
                    // (which will be 0)
                    Optional<TransactionCountByStatusModel> foundTransactionCount =
                            transactionCounters.stream()
                                    .filter(tc -> tc.getStatus().equalsIgnoreCase(ps))
                                    .findFirst();

                    if (foundTransactionCount.isEmpty()) {
                        TransactionCountByStatusModel tc = new TransactionCountByStatusModel();
                        tc.setCount(0);
                        tc.setStatus(ps);
                        notFoundTransactionCounters.add(tc);
                    }
                });

        var finalTransactionCounters = new ArrayList<TransactionCountByStatusModel>();
        finalTransactionCounters.addAll(transactionCounters);
        finalTransactionCounters.addAll(notFoundTransactionCounters);

        // since statuses could potentially be repeated (since multiple internal statuses can have
        // the same public
        // status) we will need to group them
        Map<String, TransactionCountByStatusModel> groupedCounts = new HashMap<>();
        finalTransactionCounters.forEach(
                tc -> {
                    if (!groupedCounts.containsKey(tc.getStatus())) {
                        TransactionCountByStatusModel count = new TransactionCountByStatusModel();
                        count.setStatus(tc.getStatus());
                        count.setCount(0);
                        groupedCounts.put(tc.getStatus(), count);
                    }

                    Integer currentCount = groupedCounts.get(tc.getStatus()).getCount();
                    groupedCounts.get(tc.getStatus()).setCount(currentCount + tc.getCount());
                });

        // sort by status then return
        return groupedCounts.values().stream()
                .sorted(Comparator.comparing(s -> s.getStatus().toLowerCase(Locale.ENGLISH)))
                .toList();
    }

    /**
     * Finds a CustomerProvidedDocument in a given transaction.
     *
     * @param transaction Transaction with documents.
     * @param documentId ID of the document to retrieve.
     * @return Desired CustomerProvidedDocument or empty.
     */
    public CustomerProvidedDocument getCustomerProvidedDocumentInTransactionById(
            Transaction transaction, String documentId) {
        Optional<CustomerProvidedDocument> optionalExistingCustomerProvidedDocument =
                getCustomerProvidedDocumentModelById(transaction, documentId);

        return optionalExistingCustomerProvidedDocument.orElseThrow(
                () -> new NotFoundException("Document not found"));
    }

    private Optional<CustomerProvidedDocument> getCustomerProvidedDocumentModelById(
            Transaction transaction, String documentId) {
        return transaction.getCustomerProvidedDocuments().stream()
                .filter(
                        customerProvidedDocument ->
                                customerProvidedDocument
                                        .getId()
                                        .equals(UUID.fromString(documentId)))
                .findFirst();
    }

    /**
     * Validates if a Customer Document Upsert Request is valid.
     *
     * @param customerProvidedDocument Object with modification requested data.
     * @param schema linked to transaction
     */
    private void customerProvidedDocumentUpsertValidation(
            CustomerProvidedDocument customerProvidedDocument, Schema schema) {
        validateRejectionReasons(customerProvidedDocument);
        validateDataPath(customerProvidedDocument, schema);
    }

    private void validateRejectionReasons(CustomerProvidedDocument customerProvidedDocument) {
        if (customerProvidedDocument.getReviewStatus().equals(ReviewStatus.REJECTED)
                && customerProvidedDocument.getRejectionReasons() == null) {
            throw new ProvidedDataException(
                    "A customer provided document was rejected, but no reason was given");
        }

        if (customerProvidedDocument.getRejectionReasons() != null
                && !customerProvidedDocument.getRejectionReasons().isEmpty()
                && !customerProvidedDocument.getReviewStatus().equals(ReviewStatus.REJECTED)) {
            throw new ProvidedDataException(
                    "A rejection reason was given but the document was not rejected");
        }
    }

    private void validateDataPath(
            CustomerProvidedDocument customerProvidedDocument, Schema schema) {
        if (customerProvidedDocument.getDataPath() != null) {
            String[] keyParts = customerProvidedDocument.getDataPath().split("\\.");

            if (schema == null) {
                throw new ProvidedDataException("Schema does not exist");
            }

            validateKeyParts(keyParts, schema);
        }
    }

    private void validateKeyParts(String[] keyParts, Schema schema) {
        for (String keyPart : keyParts) {
            DynaProperty property = schema.getDynaProperty(keyPart);

            if (property == null) {
                throw new ProvidedDataException("Invalid Data Path");
            }

            if (isDynamicProperty(property)) {

                Optional<Schema> optionalSchema =
                        schemaService.getSchemaByKey(
                                schema.getRelatedSchemas().get(property.getName()));
                if (optionalSchema.isEmpty()) {
                    throw new ProvidedDataException("Invalid Data Path In Nested Schema");
                }

                schema = optionalSchema.get();
            } else {
                if (isDocumentProperty(property)) {
                    throw new ProvidedDataException("Dotted path does not lead to a document");
                }
            }
        }
    }

    private static boolean isDocumentProperty(DynaProperty property) {
        return !property.getType().equals(Document.class)
                && !(property.getType().equals(List.class)
                        && property.getContentType() != null
                        && property.getContentType().equals(Document.class));
    }

    private static boolean isDynamicProperty(DynaProperty property) {
        return property.getType().equals(DynamicEntity.class)
                || (property.getType().isArray()
                        && property.getType().getComponentType().equals(DynamicEntity.class));
    }

    /**
     * Updates a CustomerProvidedDocument according to a request.
     *
     * @param requestCustomerProvidedDocument Object with the data to be modified
     * @param transaction transaction that contains the document to be updated.
     * @return Modified CustomerProvidedDocument.
     *
     * @throws NotFoundException if the document does not exist
     */
    public CustomerProvidedDocument updateCustomerProvidedDocument(
            CustomerProvidedDocument requestCustomerProvidedDocument, Transaction transaction) {

        customerProvidedDocumentUpsertValidation(
                requestCustomerProvidedDocument, transaction.getData().getSchema());

        CustomerProvidedDocument existingCustomerProvidedDocument =
                getCustomerProvidedDocumentInTransactionById(
                        transaction, requestCustomerProvidedDocument.getId().toString());

        if (Boolean.FALSE.equals(existingCustomerProvidedDocument.getActive())) {
            throw new NotFoundException("Document has been deleted");
        }

        String agencyUserId = SecurityContextUtility.getAuthenticatedUserId();
        existingCustomerProvidedDocument.setReviewedBy(agencyUserId);
        existingCustomerProvidedDocument.setReviewedOn(
                requestContextTimestamp.getCurrentTimestamp());

        existingCustomerProvidedDocument.setRejectionReasons(
                requestCustomerProvidedDocument.getRejectionReasons());

        existingCustomerProvidedDocument.setReviewStatus(
                requestCustomerProvidedDocument.getReviewStatus());

        List<CustomerProvidedDocument> documents =
                new ArrayList<>(transaction.getCustomerProvidedDocuments());
        documents.replaceAll(
                document ->
                        document.getId().equals(existingCustomerProvidedDocument.getId())
                                ? existingCustomerProvidedDocument
                                : document);
        transaction.setCustomerProvidedDocuments(documents);

        transaction.setLastUpdatedTimestamp(requestContextTimestamp.getCurrentTimestamp());
        repository.save(transaction);

        return existingCustomerProvidedDocument;
    }

    /**
     * Saves a CustomerProvidedDocument according to a request if it does not exists.
     *
     * @param requestCustomerProvidedDocument document to be saved
     * @param transactionId ID of the transaction that contains the document to be updated.
     * @return Modified CustomerProvidedDocument.
     *
     * @throws BusinessLogicException if the document already exists
     */
    public Optional<CustomerProvidedDocument> saveCustomerProvidedDocumentIfDoesNotExists(
            CustomerProvidedDocument requestCustomerProvidedDocument, String transactionId) {

        Transaction transaction = getTransactionIfExists(transactionId);

        customerProvidedDocumentUpsertValidation(
                requestCustomerProvidedDocument, transaction.getData().getSchema());

        Optional<CustomerProvidedDocument> optionalCustomerProvidedDocument =
                getCustomerProvidedDocumentModelById(
                        transaction, requestCustomerProvidedDocument.getId().toString());
        Optional<CustomerProvidedDocument> optionalSavedCustomerProvidedDocument = Optional.empty();
        if (optionalCustomerProvidedDocument.isEmpty()) {
            CustomerProvidedDocument existingCustomerProvidedDocument =
                    requestCustomerProvidedDocument;
            existingCustomerProvidedDocument.setActive(true);

            optionalSavedCustomerProvidedDocument =
                    Optional.of(
                            customerProvidedDocumentRepository.save(
                                    existingCustomerProvidedDocument));
        }

        return optionalSavedCustomerProvidedDocument;
    }

    /**
     * Queries a transaction from the db, and throw a NotFoundException if it can't find it.
     *
     * @param transactionId transaction ID to query
     * @return found transaction
     *
     * @throws NotFoundException if transaction is not found
     */
    public Transaction getTransactionIfExists(String transactionId) {
        Optional<Transaction> optionalTransaction =
                getTransactionById(UUID.fromString(transactionId));
        if (optionalTransaction.isEmpty()) {
            throw new NotFoundException("Transaction not found");
        }
        return optionalTransaction.get();
    }

    private List<String> addInternalStatusesToPublicStatusList(
            List<String> statuses, Map<String, List<String>> statusMap) {
        if (statuses == null || statuses.isEmpty()) {
            return statuses;
        }

        // this assumes the statuses being passed in are public
        List<String> internalStatuses = new ArrayList<>();
        statuses.forEach(
                s -> {
                    if (statusMap.containsKey(s)) {
                        internalStatuses.addAll(statusMap.get(s));
                    }
                });

        // add the "public" statuses (could have been internal statuses instead)
        internalStatuses.addAll(statuses);

        return internalStatuses;
    }

    /**
     * Gets the classifier for a given document id.
     *
     * @param documentId  document id
     * @param mapDocumentIdToAttributeConfiguration map of document id to attribute configuration
     *
     * @return Optional wrapping classifier
     */
    private Optional<DocumentClassifierConfiguration> getClassifier(
            UUID documentId,
            Map<UUID, List<AttributeConfiguration>> mapDocumentIdToAttributeConfiguration) {
        if (!mapDocumentIdToAttributeConfiguration.containsKey(documentId)) {
            return Optional.empty();
        }
        return mapDocumentIdToAttributeConfiguration.get(documentId).stream()
                .filter(DocumentClassifierConfiguration.class::isInstance)
                .map(ac -> (DocumentClassifierConfiguration) ac)
                .findFirst();
    }

    /**
     * Maps document id to attribute configuration.
     *
     * @param transaction transaction
     * @param attributeConfigurations Map containing attribute configurations for each attribute in the transaction
     *                                
     * @return Map containing document id to attribute configuration
     */
    private Map<UUID, List<AttributeConfiguration>> mapDocumentIdToAttributeConfiguration(
            Transaction transaction,
            Map<String, List<AttributeConfiguration>> attributeConfigurations) {
        Map<UUID, List<AttributeConfiguration>> mapDocumentIdToAttributeConfiguration =
                new HashMap<>();
        attributeConfigurations
                .keySet()
                .forEach(
                        attributeName -> {
                            if (transaction.getData().get(attributeName)
                                    instanceof Document document) {
                                mapDocumentIdToAttributeConfiguration.put(
                                        document.getDocumentId(),
                                        attributeConfigurations.get(attributeName));
                            }
                        });
        return mapDocumentIdToAttributeConfiguration;
    }

    /**
     * Posts an audit event for transaction being created.
     *
     * @param transaction transaction information.
     * @return id of posted audit event.
     */
    public UUID postAuditEventForTransactionCreated(Transaction transaction) {

        TransactionCreatedAuditEventDto transactionInfo =
                new TransactionCreatedAuditEventDto(transaction.getCreatedBy());

        final String summary = "Transaction Created.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(transaction.getCreatedBy())
                        .userId(transaction.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(transaction.getId())
                        .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                        .data(transactionInfo.toJson(), AuditActivityType.TRANSACTION_CREATED)
                        .build();

        return transactionAuditEventService.sendAuditEvent(auditEvent);
    }

    /**
     * Posts an audit event for a customer provided document change.
     *
     * @param document document information
     * @param documentFieldPath path of the document field in the schema
     * @param auditActivityType attribute that classifies the event
     * @param summary summary of what happened in the event
     * @return id of posted audit event.
     */
    public UUID postAuditEventForDocumentStatusChanged(
            CustomerProvidedDocument document,
            String documentFieldPath,
            AuditActivityType auditActivityType,
            String summary) {

        List<String> rejectionReasons = new ArrayList<>();
        if (document.getRejectionReasons() != null) {
            rejectionReasons.addAll(
                    document.getRejectionReasons().stream()
                            .map(
                                    rejectionReason ->
                                            rejectionReason.getRejectionReasonValue().getValue())
                            .toList());
        }

        DocumentStatusChangedAuditEventDto documentInfo =
                new DocumentStatusChangedAuditEventDto(
                        document.getId().toString(),
                        document.getTransactionId().toString(),
                        documentFieldPath,
                        rejectionReasons);

        String userId = SecurityContextUtility.getAuthenticatedUserId();

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary(summary)
                        .businessObjectId(document.getTransactionId())
                        .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                        .data(documentInfo.toJson(), auditActivityType)
                        .build();

        return transactionAuditEventService.sendAuditEvent(auditEvent);
    }

    /**
     * Updates a data map to include new values.
     *
     * @param update Map with the desired udpate.
     * @param existing Map with the previous map state.
     * @return updated map.
     */
    public Map<String, Object> unifyAttributeMaps(
            Map<String, Object> update, Map<String, Object> existing) {
        for (Map.Entry<String, Object> entry : update.entrySet()) {
            if (existing.containsKey(entry.getKey())
                    && (entry.getValue() instanceof Map
                            && existing.get(entry.getKey()) instanceof Map)) {
                unifyAttributeMaps(
                        (Map<String, Object>) entry.getValue(),
                        (Map<String, Object>) existing.get(entry.getKey()));
            } else {
                existing.put(entry.getKey(), entry.getValue());
            }
        }
        return existing;
    }

    /**
     * Validates a given form step.
     *
     * @param formStepKey Not-null Identifier for the specific step in the form to be validated.
     * @param transactionDefinitionKey Identifier for the transaction definition.
     * @param transaction Transaction whose data is to be validated.
     * @param task Workflow task to be validated.
     * @param context Context for form configuration.
     *
     * @throws NuvalenceFormioValidationException if the form step is invalid.
     * @throws BusinessLogicException If the formStepKey is null, or if the form configuration is invalid
     */
    public void validateFormStep(
            String formStepKey,
            String transactionDefinitionKey,
            Transaction transaction,
            String task,
            String context) {

        if (formStepKey == null) {
            throw new BusinessLogicException("Form step key cannot be null");
        }
        final String userType =
                CurrentUserUtility.getCurrentUser().map(UserToken::getUserType).orElse(null);

        final TransactionDefinition transactionDefinition =
                transactionDefinitionService
                        .getTransactionDefinitionById(transaction.getTransactionDefinitionId())
                        .orElseThrow();

        Optional<String> optionalFormConfigurationKey =
                transactionDefinition.getFormConfigurationKey(task, userType, context);
        if (optionalFormConfigurationKey.isEmpty()) {
            throw new BusinessLogicException("Invalid form configuration key found");
        }
        String formKey = optionalFormConfigurationKey.get();

        Optional<FormConfiguration> formConfigurationOptional =
                formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, formKey);

        if (formConfigurationOptional.isPresent()) {
            FormConfiguration formConfiguration = formConfigurationOptional.get();

            NuvalenceFormioComponent formioComponent =
                    formConfigMapper.formConfigToValidationConfigForStep(
                            formConfiguration, formStepKey, false);

            List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();
            formIoValidator.validateDataAgainstFormConfig(
                    formioComponent, transaction.getData(), formioValidationErrors);
            if (!formioValidationErrors.isEmpty()) {
                NuvalenceFormioValidationExMessage formioValidationExMessage =
                        NuvalenceFormioValidationExMessage.builder()
                                .formioValidationErrors(formioValidationErrors)
                                .build();

                throw new NuvalenceFormioValidationException(formioValidationExMessage);
            }
        }
    }

    /**
     * Validates if a transaction update request contains admin level changes.
     *
     * @param originalTransaction The existing transaction to be updated.
     * @param assignedTo Assigned agent from the update request.
     * @param priority Transaction priority from the update request.
     *
     * @return true if there are admin level changes requested, false otherwise.
     */
    public boolean hasAdminDataChanges(
            Transaction originalTransaction, String assignedTo, String priority) {

        if (assignedTo != null && !assignedTo.equals(originalTransaction.getAssignedTo())) {
            return true;
        }

        return priority != null && !priority.equals(originalTransaction.getPriority().getValue());
    }

    private void setDocumentsFromPendingToNew(Map<UUID, String> customerProvidedDocumentsMap) {
        for (Map.Entry<UUID, String> entry : customerProvidedDocumentsMap.entrySet()) {
            UUID key = entry.getKey();

            Optional<CustomerProvidedDocument> optionalCustomerProvidedDocument =
                    customerProvidedDocumentRepository.findById(key);
            if (optionalCustomerProvidedDocument.isPresent()) {
                CustomerProvidedDocument customerProvidedDocument =
                        optionalCustomerProvidedDocument.get();
                if (!customerProvidedDocument
                        .getDataPath()
                        .equals(customerProvidedDocumentsMap.get(key))) {
                    customerProvidedDocument.setDataPath(customerProvidedDocumentsMap.get(key));
                }
                if (customerProvidedDocument.getReviewStatus().equals(ReviewStatus.PENDING)) {
                    customerProvidedDocument.setReviewStatus(ReviewStatus.NEW);
                }
            }
        }
    }

    public int updateCompletedTransactions() {
        return repository.markTransactionsAsCompleted();
    }

    public List<Transaction> getTransactionsBySubjectProfileIdAndType(
            UUID subjectProfileId, ProfileType subjectProfileType) {
        return repository.findBySubjectProfileIdAndSubjectProfileType(
                subjectProfileId, subjectProfileType);
    }
}
