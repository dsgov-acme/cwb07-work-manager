package io.nuvalence.workmanager.service.service;

import static io.nuvalence.workmanager.service.utils.testutils.TransactionUtilsForTest.createRequestCustomerProvidedDocument;
import static io.nuvalence.workmanager.service.utils.testutils.TransactionUtilsForTest.getCommonTransactionBuilder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReason;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
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
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.generated.models.TransactionCountByStatusModel;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapperImpl;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.models.ByUserTransactionsFilters;
import io.nuvalence.workmanager.service.models.SearchTransactionsFilters;
import io.nuvalence.workmanager.service.models.TransactionFilters;
import io.nuvalence.workmanager.service.models.auditevents.*;
import io.nuvalence.workmanager.service.repository.CustomerProvidedDocumentRepository;
import io.nuvalence.workmanager.service.repository.TransactionRepository;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import io.nuvalence.workmanager.service.utils.formconfig.formio.NuvalenceFormioValidator;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "mockUserId")
class TransactionServiceTest {
    @Mock private TransactionRepository repository;

    @Mock private CustomerProvidedDocumentRepository customerProvidedDocumentRepository;

    @Mock private TransactionFactory factory;

    @Mock private TransactionTaskService transactionTaskService;

    @Mock private WorkflowTasksService workflowTasksService;

    @Mock private SchemaService schemaService;

    @Mock private AuditEventService transactionAuditEventService;

    @Mock private RequestContextTimestamp requestContextTimestamp;

    @Mock private FormConfigurationService formConfigurationService;

    @Mock private TransactionDefinitionService transactionDefinitionService;

    @Mock private FormConfigurationMapperImpl formConfigMapper;

    @Mock private NuvalenceFormioValidator formioValidator;
    @Mock private IndividualService individualService;
    @Mock private EmployerService employerService;

    private TransactionService service;

    @BeforeEach
    void setup() {
        service =
                spy(
                        new TransactionService(
                                repository,
                                customerProvidedDocumentRepository,
                                transactionDefinitionService,
                                factory,
                                transactionTaskService,
                                workflowTasksService,
                                transactionAuditEventService,
                                schemaService,
                                requestContextTimestamp,
                                formConfigurationService,
                                formConfigMapper,
                                formioValidator,
                                individualService,
                                employerService));
    }

    @Test
    void createTransaction() throws MissingSchemaException {
        // Arrange
        final TransactionDefinition definition = TransactionDefinition.builder().build();
        final Transaction transaction = Transaction.builder().build();
        when(factory.createTransaction(definition)).thenReturn(transaction);

        // Act
        service.createTransaction(definition);

        // Assert
        Mockito.verify(repository).save(transaction);
    }

    @Test
    void createTransactionWithToken() throws MissingSchemaException {
        // Arrange
        final TransactionDefinition definition = TransactionDefinition.builder().build();
        final Transaction transaction = Transaction.builder().build();
        final String token = "token";
        when(factory.createTransaction(definition)).thenReturn(transaction);

        // Act
        service.createTransaction(definition, token);

        // Assert
        Mockito.verify(repository).save(transaction);
    }

    @Test
    void getTransactionByIdFound() {
        // Arrange
        final DynamicEntity entity = new DynamicEntity(Schema.builder().build());
        final Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).data(entity).build();
        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        // Act and Assert
        assertEquals(Optional.of(transaction), service.getTransactionById(transaction.getId()));
    }

    @Test
    void getTransactionByIdNotFound() {
        // Arrange
        final UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act and Assert
        assertTrue(service.getTransactionById(id).isEmpty());
    }

    @Test
    void updateTransaction() {
        // Arrange
        final DynamicEntity entity = new DynamicEntity(Schema.builder().build());
        final Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).data(entity).build();
        when(repository.save(transaction)).thenReturn(transaction);

        // Act
        final Transaction result = service.updateTransaction(transaction);

        // Assert
        assertEquals(transaction, result);
    }

    @Test
    void completeTask() throws MissingTaskException, JsonProcessingException {
        // Arrange
        final Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();

        // Act
        service.completeTask(transaction, "taskId", "foo");

        // Assert
        Mockito.verify(transactionTaskService).completeTask(transaction, "taskId", "foo");
    }

    @Test
    void getTransactionsByUser() {
        // Arrange
        final DynamicEntity entity1 = new DynamicEntity(Schema.builder().build());
        final Transaction transaction1 =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .data(entity1)
                        .createdBy("user")
                        .externalId("y")
                        .build();
        final DynamicEntity entity2 = new DynamicEntity(Schema.builder().build());
        final Transaction transaction2 =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .data(entity2)
                        .createdBy("user")
                        .externalId("y")
                        .build();

        TransactionFilters filters =
                ByUserTransactionsFilters.builder()
                        .subjectUserId("user")
                        .createdBy("user")
                        .sortBy("id")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(25)
                        .build();

        when(repository.findAll(any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(transaction1, transaction2)));

        // Act and Assert
        assertEquals(
                new PageImpl<>(List.of(transaction1, transaction2)),
                service.getFilteredTransactions(filters));
    }

    @Test
    void getFilteredTransactions() {
        // Arrange
        final DynamicEntity entity1 = new DynamicEntity(Schema.builder().build());
        final Transaction transaction1 =
                Transaction.builder().id(UUID.randomUUID()).data(entity1).createdBy("user").build();
        final DynamicEntity entity2 = new DynamicEntity(Schema.builder().build());
        final Transaction transaction2 =
                Transaction.builder().id(UUID.randomUUID()).data(entity2).createdBy("user").build();
        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(List.of("dummy"))
                        .category("test")
                        .startDate(OffsetDateTime.now())
                        .endDate(OffsetDateTime.now())
                        .priority(List.of(TransactionPriority.MEDIUM))
                        .status(List.of("new"))
                        .assignedTo(List.of(UUID.randomUUID().toString()))
                        .sortBy("id")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(25)
                        .build();

        final Page<Transaction> pagedResults = new PageImpl<>(List.of(transaction1, transaction2));

        when(repository.findAll(ArgumentMatchers.any(), ArgumentMatchers.<Pageable>any()))
                .thenReturn(pagedResults);

        // Act and Assert
        assertEquals(pagedResults, service.getFilteredTransactions(filters));
    }

    @Test
    void getTransactionCountsByStatus() {
        // Arrange
        String withTasksPublicStatus = "withTasksPublicStatus";
        String withTasksInternalStatus = "withTasksInternalStatus";

        String configuredButNoTasksStatus = "configuredButNoTasksStatus";

        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(List.of("dummy"))
                        .category("test")
                        .startDate(OffsetDateTime.now())
                        .endDate(OffsetDateTime.now())
                        .priority(List.of(TransactionPriority.MEDIUM))
                        .status(List.of(withTasksPublicStatus, configuredButNoTasksStatus))
                        .assignedTo(List.of(UUID.randomUUID().toString()))
                        .build();

        final TransactionCountByStatusModel count = new TransactionCountByStatusModel();
        count.setStatus(withTasksInternalStatus);
        count.setCount(123);

        // handlers
        when(workflowTasksService.getCamundaStatuses(
                        WorkflowTasksService.StatusType.PUBLIC.name(),
                        filters.getCategory(),
                        filters.getTransactionDefinitionKeys()))
                .thenReturn(Arrays.asList(withTasksPublicStatus, configuredButNoTasksStatus));

        Map<String, List<String>> statusMap = new HashMap<>();
        statusMap.put(withTasksPublicStatus, Arrays.asList(withTasksInternalStatus));

        when(workflowTasksService.getStatusMap(
                        filters.getCategory(), filters.getTransactionDefinitionKeys()))
                .thenReturn(statusMap);

        when(repository.getTransactionCountsByStatus(ArgumentMatchers.any()))
                .thenReturn(List.of(count));

        // Act
        List<TransactionCountByStatusModel> response =
                service.getTransactionCountsByStatus(filters);

        // Assert
        assertEquals(2, response.size());

        assertEquals(0, response.get(0).getCount());
        assertEquals(configuredButNoTasksStatus, response.get(0).getStatus());

        assertEquals(123, response.get(1).getCount());
        assertEquals(withTasksPublicStatus, response.get(1).getStatus());
    }

    @Test
    void updateCustomerProvidedDocumentTest_ValidationFails_RejectedButNoReasonGiven() {
        Transaction transaction = getCommonTransactionBuilder().build();
        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setReviewStatus(ReviewStatus.REJECTED);
        String expectedErrorMessage =
                "A customer provided document was rejected, but no reason was given";

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () -> service.updateCustomerProvidedDocument(request, transaction));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void
            updateCustomerProvidedDocumentTest_ValidationFails_RejectedReasonGivenButStatusWasNotRejected() {
        Transaction transaction = getCommonTransactionBuilder().build();
        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setReviewStatus(ReviewStatus.NEW);
        request.setRejectionReasons(
                List.of(
                        RejectionReason.builder()
                                .rejectionReasonValue(RejectionReasonType.POOR_QUALITY)
                                .customerProvidedDocument(request)
                                .build()));
        String expectedErrorMessage =
                "A rejection reason was given but the document was not rejected";

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () -> service.updateCustomerProvidedDocument(request, transaction));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void updateCustomerProvidedDocumentTest_ValidationFails_InvalidDataPath() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("documentPath1", Document.class)
                        .property("documentPath2", Document.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        UUID documentID1 = UUID.randomUUID();
        UUID documentID2 = UUID.randomUUID();
        data.set("documentPath1", new Document(documentID1, "filename"));
        data.set("documentPath2", new Document(documentID2, "filename"));

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("invalid");

        String expectedErrorMessage = "Invalid Data Path";

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () -> service.updateCustomerProvidedDocument(request, transaction));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void updateCustomerProvidedDocumentTest_ValidationFails_PointsToNoDocument() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("noDocumentPath", String.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        UUID documentID1 = UUID.randomUUID();
        data.set("noDocumentPath", "test string");

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("noDocumentPath");

        String expectedErrorMessage = "Dotted path does not lead to a document";

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () -> service.updateCustomerProvidedDocument(request, transaction));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void updateCustomerProvidedDocumentTest_ValidationFails_ListDoesNotPointToDocument() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("listOfStrings", List.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        List<String> strings = Arrays.asList("Hello", "World");
        data.set("listOfStrings", strings);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("listOfStrings");

        String expectedErrorMessage = "Dotted path does not lead to a document";

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () -> service.updateCustomerProvidedDocument(request, transaction));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void updateCustomerProvidedDocumentTest_ValidationFails_NestedDoesNotPointToDocument() {
        UUID schemaId1 = UUID.randomUUID();
        UUID schemaId2 = UUID.randomUUID();

        Schema schema2 =
                Schema.builder()
                        .id(schemaId2)
                        .key("NestedSchemaKey")
                        .name("NestedSchemaName")
                        .description("NestedSchemaDescription")
                        .property("notNestedDocumentPath", String.class)
                        .build();

        Schema schema1 =
                Schema.builder()
                        .id(schemaId1)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("nestedDynamicEntity", DynamicEntity.class)
                        .relatedSchemas(Map.of("NestedSchemaKey", String.valueOf(schema2)))
                        .build();

        DynamicEntity nestedData = new DynamicEntity(schema2);
        nestedData.set("notNestedDocumentPath", "test");

        DynamicEntity data = new DynamicEntity(schema1);
        data.set("nestedDynamicEntity", nestedData);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("nestedDynamicEntity.nothing");

        String expectedErrorMessage = "Invalid Data Path In Nested Schema";

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () -> service.updateCustomerProvidedDocument(request, transaction));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void updateCustomerProvidedDocumentTest_ValidationFails_DocumentDeleted() {
        UUID schemaId1 = UUID.randomUUID();
        UUID schemaId2 = UUID.randomUUID();

        Schema schema2 =
                Schema.builder()
                        .id(schemaId2)
                        .key("NestedSchemaKey")
                        .name("NestedSchemaName")
                        .description("NestedSchemaDescription")
                        .property("nestedDocumentPath", Document.class)
                        .build();

        Schema schema1 =
                Schema.builder()
                        .id(schemaId1)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("nestedDynamicEntity", DynamicEntity.class)
                        .relatedSchemas(Map.of("nestedDynamicEntity", "NestedSchemaKey"))
                        .build();

        DynamicEntity nestedData = new DynamicEntity(schema2);
        UUID nestedDocumentID = UUID.randomUUID();
        nestedData.set("nestedDocumentPath", new Document(nestedDocumentID, "filename"));

        DynamicEntity data = new DynamicEntity(schema1);
        data.set("nestedDynamicEntity", nestedData);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);
        transaction.setCustomerProvidedDocuments(
                List.of(
                        CustomerProvidedDocument.builder()
                                .id(nestedDocumentID)
                                .active(false)
                                .build()));

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(nestedDocumentID);
        request.setReviewStatus(ReviewStatus.NEW);
        request.setDataPath("nestedDynamicEntity.nestedDocumentPath");

        when(schemaService.getSchemaByKey("NestedSchemaKey")).thenReturn(Optional.of(schema2));
        String expectedErrorMessage = "Document has been deleted";

        NotFoundException thrownException =
                assertThrows(
                        NotFoundException.class,
                        () -> service.updateCustomerProvidedDocument(request, transaction));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void updateCustomerProvidedDocumentTest_UpdateSucceeds() {
        UUID schemaId1 = UUID.randomUUID();
        UUID schemaId2 = UUID.randomUUID();

        Schema schema2 =
                Schema.builder()
                        .id(schemaId2)
                        .key("NestedSchemaKey")
                        .name("NestedSchemaName")
                        .description("NestedSchemaDescription")
                        .property("nestedDocumentPath", Document.class)
                        .build();

        Schema schema1 =
                Schema.builder()
                        .id(schemaId1)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("nestedDynamicEntity", DynamicEntity.class)
                        .relatedSchemas(Map.of("nestedDynamicEntity", "NestedSchemaKey"))
                        .build();

        DynamicEntity nestedData = new DynamicEntity(schema2);
        UUID nestedDocumentID = UUID.randomUUID();
        nestedData.set("nestedDocumentPath", new Document(nestedDocumentID, "filename"));

        DynamicEntity data = new DynamicEntity(schema1);
        data.set("nestedDynamicEntity", nestedData);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);
        transaction.setCustomerProvidedDocuments(
                List.of(
                        CustomerProvidedDocument.builder()
                                .id(nestedDocumentID)
                                .active(true)
                                .build()));

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(nestedDocumentID);
        request.setReviewStatus(ReviewStatus.REJECTED);
        request.setRejectionReasons(
                List.of(
                        RejectionReason.builder()
                                .rejectionReasonValue(
                                        RejectionReasonType.DOES_NOT_SATISFY_REQUIREMENTS)
                                .customerProvidedDocument(request)
                                .build()));
        request.setDataPath("nestedDynamicEntity.nestedDocumentPath");

        when(schemaService.getSchemaByKey("NestedSchemaKey")).thenReturn(Optional.of(schema2));

        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String updaterId = UUID.randomUUID().toString();
        Mockito.when(securityContext.getAuthentication())
                .thenReturn(
                        UserToken.builder()
                                .applicationUserId(updaterId)
                                .roles(Collections.emptyList())
                                .build());

        CustomerProvidedDocument result =
                service.updateCustomerProvidedDocument(request, transaction);

        assertEquals(nestedDocumentID, result.getId());
        assertEquals(ReviewStatus.REJECTED, result.getReviewStatus());
        assertEquals(
                RejectionReasonType.DOES_NOT_SATISFY_REQUIREMENTS.getValue(),
                result.getRejectionReasons().get(0).getRejectionReasonValue().getValue());
        assertEquals(updaterId, result.getReviewedBy());
    }

    @Test
    void saveCustomerProvidedDocumentIfDoesNotExistsTest_FailsTransactionNotFound() {
        final Transaction transaction = getCommonTransactionBuilder().build();
        final CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        final String expectedErrorMessage = "Transaction not found";

        assertThrowsNotFoundForSaveDoc(
                request, transaction.getId().toString(), expectedErrorMessage);
    }

    private void assertThrowsNotFoundForSaveDoc(
            CustomerProvidedDocument request, String transactionId, String expectedErrorMessage) {

        NotFoundException thrownException =
                assertThrows(
                        NotFoundException.class,
                        () ->
                                service.saveCustomerProvidedDocumentIfDoesNotExists(
                                        request, transactionId));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void
            saveCustomerProvidedDocumentIfDoesNotExistsTest_ValidationFails_RejectedButNoReasonGiven() {
        Transaction transaction = getCommonTransactionBuilder().build();
        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setReviewStatus(ReviewStatus.REJECTED);
        String expectedErrorMessage =
                "A customer provided document was rejected, but no reason was given";

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrowsProvidedDataExceptionForSaveDoc(
                request, transaction.getId().toString(), expectedErrorMessage);
    }

    private void assertThrowsProvidedDataExceptionForSaveDoc(
            CustomerProvidedDocument request, String transactionId, String expectedErrorMessage) {

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () ->
                                service.saveCustomerProvidedDocumentIfDoesNotExists(
                                        request, transactionId));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void
            saveCustomerProvidedDocumentIfDoesNotExistsTest_RejectedReasonGivenButStatusWasNotRejected() {
        Transaction transaction = getCommonTransactionBuilder().build();
        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setReviewStatus(ReviewStatus.NEW);
        request.setRejectionReasons(
                List.of(
                        RejectionReason.builder()
                                .rejectionReasonValue(RejectionReasonType.POOR_QUALITY)
                                .customerProvidedDocument(request)
                                .build()));
        String expectedErrorMessage =
                "A rejection reason was given but the document was not rejected";

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrowsProvidedDataExceptionForSaveDoc(
                request, transaction.getId().toString(), expectedErrorMessage);
    }

    @Test
    void saveCustomerProvidedDocumentIfDoesNotExistsTest_ValidationFails_InvalidDataPath() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("documentPath1", Document.class)
                        .property("documentPath2", Document.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        UUID documentID1 = UUID.randomUUID();
        UUID documentID2 = UUID.randomUUID();
        data.set("documentPath1", new Document(documentID1, "filename"));
        data.set("documentPath2", new Document(documentID2, "filename"));

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("invalid");

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        String expectedErrorMessage = "Invalid Data Path";

        String transactionId = transaction.getId().toString();

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () ->
                                service.saveCustomerProvidedDocumentIfDoesNotExists(
                                        request, transactionId));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void saveCustomerProvidedDocumentIfDoesNotExistsTest_ValidationFails_PointsToNoDocument() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("noDocumentPath", String.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        UUID documentID1 = UUID.randomUUID();
        data.set("noDocumentPath", "test string");

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("noDocumentPath");

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        String expectedErrorMessage = "Dotted path does not lead to a document";

        assertThrowsProvidedDataExceptionForSaveDoc(
                request, transaction.getId().toString(), expectedErrorMessage);
    }

    @Test
    void
            saveCustomerProvidedDocumentIfDoesNotExistsTest_ValidationFails_ListDoesNotPointToDocument() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("listOfStrings", List.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        List<String> strings = Arrays.asList("Hello", "World");
        data.set("listOfStrings", strings);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("listOfStrings");

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        String expectedErrorMessage = "Dotted path does not lead to a document";
        String transactionId = transaction.getId().toString();

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () ->
                                service.saveCustomerProvidedDocumentIfDoesNotExists(
                                        request, transactionId));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void saveCustomerProvidedDocumentIfDoesNotExistsTest_allowListOfDocuments() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("listOfDocuments", List.class, Document.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        List<String> strings = Arrays.asList("Hello", "World");
        data.set("listOfDocuments", strings);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        List<CustomerProvidedDocument> documents = new ArrayList<>();
        documents.add(request);
        transaction.setCustomerProvidedDocuments(documents);
        request.setDataPath("listOfDocuments");

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        String transactionId = transaction.getId().toString();

        assertDoesNotThrow(
                () -> service.saveCustomerProvidedDocumentIfDoesNotExists(request, transactionId),
                "Exception was thrown, but it was not expected.");
    }

    @Test
    void
            saveCustomerProvidedDocumentIfDoesNotExistsTest_ValidationFails_NestedDoesNotPointToDocument() {
        UUID schemaId1 = UUID.randomUUID();
        UUID schemaId2 = UUID.randomUUID();

        Schema schema2 =
                Schema.builder()
                        .id(schemaId2)
                        .key("NestedSchemaKey")
                        .name("NestedSchemaName")
                        .description("NestedSchemaDescription")
                        .property("notNestedDocumentPath", String.class)
                        .build();

        Schema schema1 =
                Schema.builder()
                        .id(schemaId1)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("nestedDynamicEntity", DynamicEntity.class)
                        .relatedSchemas(Map.of("NestedSchemaKey", String.valueOf(schema2)))
                        .build();

        DynamicEntity nestedData = new DynamicEntity(schema2);
        nestedData.set("notNestedDocumentPath", "test");

        DynamicEntity data = new DynamicEntity(schema1);
        data.set("nestedDynamicEntity", nestedData);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setDataPath("nestedDynamicEntity.nothing");

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        String expectedErrorMessage = "Invalid Data Path In Nested Schema";
        String transactionId = transaction.getId().toString();

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () ->
                                service.saveCustomerProvidedDocumentIfDoesNotExists(
                                        request, transactionId));

        assertEquals(expectedErrorMessage, thrownException.getMessage());
    }

    @Test
    void saveCustomerProvidedDocumentIfDoesNotExistsTest_CreationSucceeds() {
        UUID schemaId1 = UUID.randomUUID();
        UUID schemaId2 = UUID.randomUUID();

        Schema schema2 =
                Schema.builder()
                        .id(schemaId2)
                        .key("NestedSchemaKey")
                        .name("NestedSchemaName")
                        .description("NestedSchemaDescription")
                        .property("nestedDocumentPath", Document.class)
                        .build();

        Schema schema1 =
                Schema.builder()
                        .id(schemaId1)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("nestedDynamicEntity", DynamicEntity.class)
                        .relatedSchemas(Map.of("nestedDynamicEntity", "NestedSchemaKey"))
                        .build();

        DynamicEntity nestedData = new DynamicEntity(schema2);
        UUID nestedDocumentID = UUID.randomUUID();
        nestedData.set("nestedDocumentPath", new Document(nestedDocumentID, "filename"));

        DynamicEntity data = new DynamicEntity(schema1);
        data.set("nestedDynamicEntity", nestedData);

        Transaction transaction = getCommonTransactionBuilder().build();
        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setSchemaKey("TestSchemaKey");
        transaction.setData(data);
        transaction.setCustomerProvidedDocuments(List.of());

        CustomerProvidedDocument request = createRequestCustomerProvidedDocument(null);
        request.setTransactionId(transaction.getId());
        request.setReviewStatus(ReviewStatus.NEW);
        request.setDataPath("nestedDynamicEntity.nestedDocumentPath");

        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(schemaService.getSchemaByKey("NestedSchemaKey")).thenReturn(Optional.of(schema2));
        when(customerProvidedDocumentRepository.save(any())).thenReturn(request);
        Optional<CustomerProvidedDocument> result =
                service.saveCustomerProvidedDocumentIfDoesNotExists(
                        request, transaction.getId().toString());

        assertTrue(result.isPresent());
        assertEquals(request.getId(), result.get().getId());
        assertEquals(ReviewStatus.NEW, result.get().getReviewStatus());
        assertNull(result.get().getRejectionReasons());
        assertEquals(transaction.getId(), result.get().getTransactionId());
        assertTrue(result.get().getActive());
    }

    @Test
    void testUpdateTransactionFromPartialUpdate_FlatStructure() {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("documentPath1", Document.class)
                        .property("documentPath2", Document.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        UUID documentID1 = UUID.randomUUID();
        UUID documentID2 = UUID.randomUUID();
        data.set("documentPath1", new Document(documentID1, "filename"));
        data.set("documentPath2", new Document(documentID2, "filename"));

        Transaction partialUpdate = getCommonTransactionBuilder().build();
        partialUpdate.setData(data);
        partialUpdate.setCustomerProvidedDocuments(new ArrayList<>());

        List<CustomerProvidedDocument> result =
                service.updateDocuments(partialUpdate, partialUpdate, Map.of());
        Map<UUID, String> resultMap = new HashMap<>();
        for (CustomerProvidedDocument document : result) {
            resultMap.put(document.getId(), document.getDataPath());
        }

        assertEquals(2, result.size());
        assertTrue(resultMap.keySet().contains(documentID1));
        assertTrue(resultMap.keySet().contains(documentID2));
        assertEquals("documentPath1", resultMap.get(documentID1));
        assertEquals("documentPath2", resultMap.get(documentID2));
    }

    @Test
    void testUpdateTransactionFromPartialUpdate_NestedDynamicEntity() {
        UUID schemaId1 = UUID.randomUUID();
        UUID schemaId2 = UUID.randomUUID();

        Schema schema2 =
                Schema.builder()
                        .id(schemaId2)
                        .key("NestedSchemaKey")
                        .name("NestedSchemaName")
                        .description("NestedSchemaDescription")
                        .property("nestedDocumentPath", Document.class)
                        .build();

        Schema schema1 =
                Schema.builder()
                        .id(schemaId1)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("documentPath", Document.class)
                        .property("nestedDynamicEntity", DynamicEntity.class)
                        .build();

        DynamicEntity nestedData = new DynamicEntity(schema2);
        UUID nestedDocumentID = UUID.randomUUID();
        nestedData.set("nestedDocumentPath", new Document(nestedDocumentID, "filename"));

        DynamicEntity data = new DynamicEntity(schema1);
        UUID documentID = UUID.randomUUID();
        data.set("documentPath", new Document(documentID, "filename"));
        data.set("nestedDynamicEntity", nestedData);

        Transaction partialUpdate = getCommonTransactionBuilder().build();
        partialUpdate.setData(data);
        partialUpdate.setCustomerProvidedDocuments(new ArrayList<>());

        List<CustomerProvidedDocument> result =
                service.updateDocuments(partialUpdate, partialUpdate, Map.of());
        Map<UUID, String> resultMap = new HashMap<>();
        for (CustomerProvidedDocument document : result) {
            resultMap.put(document.getId(), document.getDataPath());
        }

        assertEquals(2, result.size());
        assertTrue(resultMap.keySet().contains(documentID));
        assertTrue(resultMap.keySet().contains(nestedDocumentID));
        assertEquals("documentPath", resultMap.get(documentID));
        assertEquals("nestedDynamicEntity.nestedDocumentPath", resultMap.get(nestedDocumentID));
    }

    @Test
    void testUpdateTransactionFromPartialUpdate_Lists() throws MissingTransactionException {
        UUID schemaId = UUID.randomUUID();

        Schema schema =
                Schema.builder()
                        .id(schemaId)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("listOfDocuments", List.class)
                        .property("listOfStrings", List.class)
                        .build();

        DynamicEntity data = new DynamicEntity(schema);
        UUID documentID1 = UUID.randomUUID();
        UUID documentID2 = UUID.randomUUID();
        List<Document> documents =
                Arrays.asList(
                        new Document(documentID1, "filename"),
                        new Document(documentID2, "filename"));
        List<String> strings = Arrays.asList("Hello", "World");
        data.set("listOfDocuments", documents);
        data.set("listOfStrings", strings);

        Transaction partialUpdate = getCommonTransactionBuilder().build();
        partialUpdate.setData(data);
        partialUpdate.setCustomerProvidedDocuments(new ArrayList<>());

        List<CustomerProvidedDocument> result =
                service.updateDocuments(partialUpdate, partialUpdate, Map.of());
        Map<UUID, String> resultMap = new HashMap<>();
        for (CustomerProvidedDocument document : result) {
            resultMap.put(document.getId(), document.getDataPath());
        }

        assertEquals(2, result.size());
        assertTrue(resultMap.keySet().contains(documentID1));
        assertTrue(resultMap.keySet().contains(documentID2));
        assertEquals("listOfDocuments", resultMap.get(documentID1));
        assertEquals("listOfDocuments", resultMap.get(documentID2));
    }

    @Test
    void testUpdateTransactionFromPartialUpdate_ListOfDynamicEntities()
            throws MissingTransactionException {
        UUID schemaId1 = UUID.randomUUID();
        UUID schemaId2 = UUID.randomUUID();

        Schema schema2 =
                Schema.builder()
                        .id(schemaId2)
                        .key("NestedSchemaKey")
                        .name("NestedSchemaName")
                        .description("NestedSchemaDescription")
                        .property("nestedDocumentPath", Document.class)
                        .build();

        Schema schema1 =
                Schema.builder()
                        .id(schemaId1)
                        .key("TestSchemaKey")
                        .name("TestSchemaName")
                        .description("TestSchemaDescription")
                        .property("listOfDynamicEntities", List.class)
                        .build();

        DynamicEntity nestedData1 = new DynamicEntity(schema2);
        UUID nestedDocumentID1 = UUID.randomUUID();
        nestedData1.set("nestedDocumentPath", new Document(nestedDocumentID1, "filename"));

        DynamicEntity nestedData2 = new DynamicEntity(schema2);
        UUID nestedDocumentID2 = UUID.randomUUID();
        nestedData2.set("nestedDocumentPath", new Document(nestedDocumentID2, "filename"));

        List<DynamicEntity> nestedDataList = Arrays.asList(nestedData1, nestedData2);

        DynamicEntity data = new DynamicEntity(schema1);
        data.set("listOfDynamicEntities", nestedDataList);

        Transaction partialUpdate = getCommonTransactionBuilder().build();
        partialUpdate.setData(data);
        partialUpdate.setCustomerProvidedDocuments(new ArrayList<>());

        List<CustomerProvidedDocument> result =
                service.updateDocuments(partialUpdate, partialUpdate, Map.of());
        Map<UUID, String> resultMap = new HashMap<>();
        for (CustomerProvidedDocument document : result) {
            resultMap.put(document.getId(), document.getDataPath());
        }

        assertEquals(2, result.size());
        assertTrue(resultMap.keySet().contains(nestedDocumentID1));
        assertTrue(resultMap.keySet().contains(nestedDocumentID2));
        assertEquals("listOfDynamicEntities.nestedDocumentPath", resultMap.get(nestedDocumentID1));
        assertEquals("listOfDynamicEntities.nestedDocumentPath", resultMap.get(nestedDocumentID2));
    }

    @Test
    void testUpdateTransactionFromPartialUpdate_NoDocumentsProvided()
            throws MissingTransactionException {
        Transaction transaction = getCommonTransactionBuilder().build();
        transaction.setTransactionDefinition(
                TransactionDefinition.builder().subjectType(ProfileType.INDIVIDUAL).build());

        when(repository.findById((transaction.getId()))).thenReturn(Optional.of(transaction));
        when(individualService.getIndividualById(any()))
                .thenReturn(Optional.ofNullable(Individual.builder().build()));
        when(repository.save(any())).thenReturn(transaction);

        Transaction result = service.updateTransactionFromPartialUpdate(transaction, Map.of());

        assertNotNull(result);
    }

    @Test
    void updateTransactionFromPartialUpdate() throws MissingTransactionException {
        Schema schema =
                Schema.builder()
                        .key("testSchemaKey")
                        .properties(List.of(new DynaProperty("document1", Document.class)))
                        .build();

        DynamicEntity data1 = new DynamicEntity(schema);
        Document document1 = new Document(UUID.randomUUID(), "filename");

        data1.set("document1", document1);

        Transaction partialUpdate =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .priority(TransactionPriority.MEDIUM)
                        .data(data1)
                        .assignedTo("test")
                        .build();

        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setId(UUID.randomUUID());
        transactionDefinition.setSchemaKey(schema.getKey());

        Transaction existingTransaction = Mockito.mock(Transaction.class);
        when(existingTransaction.getId()).thenReturn(partialUpdate.getId());
        when(existingTransaction.getCustomerProvidedDocuments()).thenReturn(new ArrayList<>());

        when(repository.findById(partialUpdate.getId()))
                .thenReturn(Optional.of(existingTransaction));

        service.updateTransactionFromPartialUpdate(partialUpdate, new HashMap<>());

        Mockito.verify(repository).save(existingTransaction);
        verify(existingTransaction).setPriority(partialUpdate.getPriority());
        verify(existingTransaction).setAssignedTo(partialUpdate.getAssignedTo());
        verify(existingTransaction).setData(partialUpdate.getData());
    }

    @Test
    void updateTransactionFromPartialUpdateWithBlankAssignedTo()
            throws MissingTransactionException {
        Schema schema =
                Schema.builder()
                        .key("testSchemaKey")
                        .properties(List.of(new DynaProperty("document1", Document.class)))
                        .build();

        DynamicEntity data1 = new DynamicEntity(schema);
        Document document1 = new Document(UUID.randomUUID(), "filename");

        data1.set("document1", document1);

        Transaction partialUpdate =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .priority(TransactionPriority.MEDIUM)
                        .data(data1)
                        .assignedTo("")
                        .build();

        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setId(UUID.randomUUID());
        transactionDefinition.setSchemaKey(schema.getKey());

        Transaction existingTransaction = Mockito.mock(Transaction.class);
        when(existingTransaction.getId()).thenReturn(partialUpdate.getId());
        when(existingTransaction.getCustomerProvidedDocuments()).thenReturn(new ArrayList<>());

        when(repository.findById(partialUpdate.getId()))
                .thenReturn(Optional.of(existingTransaction));

        service.updateTransactionFromPartialUpdate(partialUpdate, new HashMap<>());

        Mockito.verify(repository).save(existingTransaction);
        verify(existingTransaction).setAssignedTo(null);
    }

    @Test
    void testGetTransactionByProcessInstanceId_WhenTransactionExists() {
        Transaction transaction = getCommonTransactionBuilder().build();

        when(repository.findByProcessInstanceId(transaction.getProcessInstanceId()))
                .thenReturn(Optional.of(transaction));

        Optional<Transaction> result =
                service.getTransactionByProcessInstanceId(transaction.getProcessInstanceId());

        assertTrue(result.isPresent());
        assertEquals(transaction, result.get());

        verify(repository, times(1)).findByProcessInstanceId(transaction.getProcessInstanceId());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void testGetTransactionByProcessInstanceId_WhenTransactionDoesNotExist() {
        String processInstanceId = "54321";

        when(repository.findByProcessInstanceId(processInstanceId)).thenReturn(Optional.empty());

        Optional<Transaction> result = service.getTransactionByProcessInstanceId(processInstanceId);

        assertFalse(result.isPresent());

        verify(repository, times(1)).findByProcessInstanceId(processInstanceId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void testUpdateTransactionFromPartialUpdateAndCompleteTask()
            throws MissingTransactionException, MissingTaskException, JsonProcessingException {
        Schema schema =
                Schema.builder()
                        .key("testSchemaKey")
                        .properties(List.of(new DynaProperty("document1", Document.class)))
                        .build();

        DynamicEntity data1 = new DynamicEntity(schema);
        Document document1 = new Document(UUID.randomUUID(), "filename");

        data1.set("document1", document1);

        Transaction partialUpdate =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .priority(TransactionPriority.MEDIUM)
                        .data(data1)
                        .assignedTo("test")
                        .build();

        TransactionDefinition transactionDefinition = new TransactionDefinition();
        transactionDefinition.setId(UUID.randomUUID());
        transactionDefinition.setSchemaKey(schema.getKey());

        Transaction existingTransaction = Mockito.mock(Transaction.class);
        when(existingTransaction.getId()).thenReturn(partialUpdate.getId());
        when(existingTransaction.getCustomerProvidedDocuments()).thenReturn(new ArrayList<>());

        when(repository.findById(partialUpdate.getId()))
                .thenReturn(Optional.of(existingTransaction));

        when(service.updateTransactionFromPartialUpdate(partialUpdate, new HashMap<>()))
                .thenReturn(partialUpdate);

        when(service.updateTransaction(partialUpdate)).thenReturn(partialUpdate);

        Transaction result =
                service.updateTransactionFromPartialUpdateAndCompleteTask(
                        partialUpdate, "taskId", "condition", new HashMap<>());

        assertEquals(partialUpdate, result);

        verify(service, times(2))
                .updateTransactionFromPartialUpdate(partialUpdate, new HashMap<>());
        verify(service, times(1)).completeTask(partialUpdate, "taskId", "condition");
        verify(service, times(2)).updateTransaction(partialUpdate);
    }

    @Test
    void testPostAuditEventForTransactionCreated() {
        Transaction transaction = getCommonTransactionBuilder().build();

        TransactionCreatedAuditEventDto transactionInfo =
                new TransactionCreatedAuditEventDto(transaction.getCreatedBy());

        String summary = "Transaction Created.";

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(transaction.getCreatedBy())
                        .userId(transaction.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(transaction.getId())
                        .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                        .data(transactionInfo.toJson(), AuditActivityType.TRANSACTION_CREATED)
                        .build();

        when(transactionAuditEventService.sendAuditEvent(any(AuditEventRequestObjectDto.class)))
                .thenReturn(UUID.randomUUID());

        // Act
        service.postAuditEventForTransactionCreated(transaction);

        // Assert
        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void testUnifyAttributeMaps_sameKeysUpdatedValues() {
        Map<String, Object> existing = new HashMap<>();

        existing.put("key1", "value1");
        existing.put("key2", "value2");

        Map<String, Object> nestedExisting = new HashMap<>();
        nestedExisting.put("nestedKey", "nestedValue");
        existing.put("key3", nestedExisting);

        Map<String, Object> update = new HashMap<>();
        update.put("key1", "newValue1");
        update.put("key2", "newValue2");

        Map<String, Object> nestedUpdate = new HashMap<>();
        nestedUpdate.put("nestedKey", "newNestedValue");
        update.put("key3", nestedUpdate);

        Map<String, Object> result = service.unifyAttributeMaps(update, existing);

        assertEquals("newValue1", result.get("key1"));
        assertEquals("newValue2", result.get("key2"));
        assertEquals("newNestedValue", ((Map) result.get("key3")).get("nestedKey"));
    }

    @Test
    void testUnifyAttributeMaps_preserveExistingKeys() {
        Map<String, Object> existing = new HashMap<>();
        Map<String, Object> update = new HashMap<>();

        existing.put("key1", "value1");
        existing.put("key2", "value2");

        update.put("key1", "newValue1");

        Map<String, Object> result = service.unifyAttributeMaps(update, existing);

        assertEquals("newValue1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void testUnifyAttributeMaps_newKeysInUpdate() {
        Map<String, Object> existing = new HashMap<>();
        Map<String, Object> update = new HashMap<>();

        existing.put("key1", "value1");

        update.put("key1", "newValue1");
        update.put("key2", "newValue2");

        Map<String, Object> result = service.unifyAttributeMaps(update, existing);

        assertEquals("newValue1", result.get("key1"));
        assertEquals("newValue2", result.get("key2"));
    }

    @Test
    void testPostAuditEventForDocumentRejected() {
        final UUID documentId = UUID.randomUUID();

        final CustomerProvidedDocument document = createDocument(ReviewStatus.REJECTED);

        final List<RejectionReason> rejectionReasons =
                new ArrayList<>(
                        List.of(
                                new RejectionReason(
                                        documentId, RejectionReasonType.POOR_QUALITY, document),
                                new RejectionReason(
                                        documentId, RejectionReasonType.INCORRECT_TYPE, document)));
        document.setRejectionReasons(rejectionReasons);

        List<String> rejectionReasonsStrings =
                document.getRejectionReasons().stream()
                        .map(
                                rejectionReason ->
                                        rejectionReason.getRejectionReasonValue().getValue())
                        .collect(Collectors.toList());

        final String documentFieldPath = "document";
        final AuditActivityType auditActivityType = AuditActivityType.DOCUMENT_REJECTED;
        final String summary = "Transaction rejected";

        setupAndVerifyPostAuditEvent(
                document, documentFieldPath, auditActivityType, summary, rejectionReasonsStrings);
    }

    @Test
    void testPostAuditEventForDocumentAccepted() {
        final CustomerProvidedDocument document = createDocument(ReviewStatus.ACCEPTED);

        final String documentFieldPath = "document";
        final AuditActivityType auditActivityType = AuditActivityType.DOCUMENT_ACCEPTED;
        final String summary = "Transaction accepted";

        setupAndVerifyPostAuditEvent(
                document, documentFieldPath, auditActivityType, summary, new ArrayList<>());
    }

    @Test
    void testPostAuditEventForDocumentUnaccepted() {
        final CustomerProvidedDocument document = createDocument(ReviewStatus.NEW);

        final String documentFieldPath = "document";
        final AuditActivityType auditActivityType = AuditActivityType.DOCUMENT_UNACCEPTED;
        final String summary = "Transaction unaccepted";

        setupAndVerifyPostAuditEvent(
                document, documentFieldPath, auditActivityType, summary, new ArrayList<>());
    }

    @Test
    void testPostAuditEventForDocumentUnrejected() {
        final CustomerProvidedDocument document = createDocument(ReviewStatus.NEW);

        final String documentFieldPath = "document";
        final AuditActivityType auditActivityType = AuditActivityType.DOCUMENT_UNREJECTED;
        final String summary = "Transaction unrejected";

        setupAndVerifyPostAuditEvent(
                document, documentFieldPath, auditActivityType, summary, new ArrayList<>());
    }

    private CustomerProvidedDocument createDocument(ReviewStatus reviewStatus) {
        final UUID documentId = UUID.randomUUID();
        return CustomerProvidedDocument.builder()
                .id(documentId)
                .active(true)
                .reviewStatus(reviewStatus)
                .dataPath("document")
                .transactionId(UUID.randomUUID())
                .build();
    }

    private void setupAndVerifyPostAuditEvent(
            CustomerProvidedDocument document,
            String documentFieldPath,
            AuditActivityType auditActivityType,
            String summary,
            List<String> rejectionReasonsStrings) {
        DocumentStatusChangedAuditEventDto documentInfo =
                new DocumentStatusChangedAuditEventDto(
                        document.getId().toString(),
                        document.getTransactionId().toString(),
                        documentFieldPath,
                        rejectionReasonsStrings);

        String userId = UUID.randomUUID().toString();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary(summary)
                        .businessObjectId(document.getTransactionId())
                        .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                        .data(documentInfo.toJson(), auditActivityType)
                        .build();

        ;
        when(transactionAuditEventService.sendAuditEvent(any(AuditEventRequestObjectDto.class)))
                .thenReturn(UUID.randomUUID());

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(userId);

            // Act
            service.postAuditEventForDocumentStatusChanged(
                    document, documentFieldPath, auditActivityType, summary);
        }

        // Assert
        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    private Transaction getTransactionForValidateFormTest(
            String formStepKey, String transactionDefinitionKey, String formKey) {

        Map<String, Object> component = new HashMap<>();
        component.put("key", formStepKey);
        component.put("input", true);
        Map<String, Object> props = new HashMap<>();
        props.put("label", "Test");
        props.put("required", true);
        props.put("formErrorLabel", "custom error label");
        component.put("props", props);

        List<Map<String, Object>> components = new ArrayList<>(Arrays.asList(component));
        Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put("components", components);
        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .key(formKey)
                        .configuration(configurationMap)
                        .build();

        when(formConfigurationService.getFormConfigurationByKeys(transactionDefinitionKey, formKey))
                .thenReturn(Optional.ofNullable(formConfiguration));

        Schema schema =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .key("SchemaKey")
                        .name("TestSchema")
                        .description("Description")
                        .property(formStepKey, String.class)
                        .build();
        DynamicEntity data = new DynamicEntity(schema);

        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .formConfigurationSelectionRules(new ArrayList<>())
                        .build();
        transactionDefinition.addFormConfiguration(formConfiguration);

        Transaction transaction =
                Transaction.builder()
                        .data(data)
                        .transactionDefinition(transactionDefinition)
                        .build();

        when(transactionDefinitionService.getTransactionDefinitionById(any()))
                .thenReturn(Optional.of(transactionDefinition));

        return transaction;
    }

    @Test
    void testValidateForm() {
        try (var staticCurrentUserUtility = Mockito.mockStatic(CurrentUserUtility.class)) {
            staticCurrentUserUtility
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(
                            Optional.of(
                                    UserToken.builder()
                                            .userType("userType")
                                            .roles(Collections.emptyList())
                                            .build()));

            String formStepKey = "formStepKey";
            String transactionDefinitionKey = "transactionDefinitionKey";
            String formKey = "formKey";

            Transaction transaction =
                    getTransactionForValidateFormTest(
                            formStepKey, transactionDefinitionKey, formKey);

            final List<NuvalenceFormioComponent> realResponseSaver = new ArrayList<>();

            doAnswer(
                            (invocation) -> {
                                NuvalenceFormioComponent result =
                                        (NuvalenceFormioComponent) invocation.callRealMethod();
                                realResponseSaver.add(result);
                                return result;
                            })
                    .when(formConfigMapper)
                    .formConfigToValidationConfigForStep(any(), any(), eq(false));

            doCallRealMethod()
                    .when(formioValidator)
                    .validateDataAgainstFormConfig(any(), any(), any());

            NuvalenceFormioValidationException exception =
                    assertThrows(
                            NuvalenceFormioValidationException.class,
                            () -> {
                                service.validateFormStep(
                                        formStepKey, transactionDefinitionKey, transaction, "", "");
                            });

            assertEquals(
                    1, exception.getFormioValidationErrors().getFormioValidationErrors().size());
            NuvalenceFormioValidationExItem exceptionItem =
                    exception.getFormioValidationErrors().getFormioValidationErrors().get(0);
            assertEquals("required", exceptionItem.getErrorName());
            assertEquals("custom error label", exceptionItem.getErrorMessage());

            verify(formConfigMapper, times(1))
                    .formConfigToValidationConfigForStep(any(), eq(formStepKey), eq(false));

            verify(formioValidator, times(1))
                    .validateDataAgainstFormConfig(
                            eq(realResponseSaver.get(0)), eq(transaction.getData()), anyList());
        }
    }

    @Test
    void testValidateForm_InvalidFormStep() {
        try (var staticCurrentUserUtility = Mockito.mockStatic(CurrentUserUtility.class)) {
            staticCurrentUserUtility
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(
                            Optional.of(
                                    UserToken.builder()
                                            .userType("userType")
                                            .roles(Collections.emptyList())
                                            .build()));

            String formStepKey = "formStepKey";
            String transactionDefinitionKey = "transactionDefinitionKey";
            String formKey = "formKey";

            Transaction transaction =
                    getTransactionForValidateFormTest(
                            formStepKey, transactionDefinitionKey, formKey);

            when(formConfigMapper.formConfigToValidationConfigForStep(any(), any(), eq(false)))
                    .thenCallRealMethod();

            ProvidedDataException exception =
                    assertThrows(
                            ProvidedDataException.class,
                            () -> {
                                service.validateFormStep(
                                        "notExistingKey",
                                        transactionDefinitionKey,
                                        transaction,
                                        "",
                                        "");
                            });

            assertEquals(
                    "The provided formStepKey does not exist in the form configuration",
                    exception.getMessage());

            verify(formConfigMapper, times(1))
                    .formConfigToValidationConfigForStep(any(), eq("notExistingKey"), eq(false));
        }
    }

    @Test
    void testValidateForm_NullKey() {

        Transaction transaction = new Transaction();

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateFormStep(
                                    null, "transactionDefinitionKey", transaction, "", "");
                        });

        assertEquals("Form step key cannot be null", exception.getMessage());
    }

    @Test
    void testUpdateDocument() {
        UUID transactionId = UUID.randomUUID();

        CustomerProvidedDocument customerProvidedDocumentPending =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .reviewStatus(ReviewStatus.PENDING)
                        .dataPath("documentPending")
                        .build();
        CustomerProvidedDocument customerProvidedDocumentRejected =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .reviewStatus(ReviewStatus.REJECTED)
                        .dataPath("documentRejected")
                        .build();
        CustomerProvidedDocument customerProvidedDocumentNew =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .reviewStatus(ReviewStatus.NEW)
                        .dataPath("documentNew")
                        .build();

        Schema schema =
                Schema.builder()
                        .properties(
                                List.of(
                                        new DynaProperty("documentPending", Document.class),
                                        new DynaProperty("documentRejected", Document.class),
                                        new DynaProperty("documentNew", Document.class)))
                        .build();

        DynamicEntity dynamicEntityUpdate = new DynamicEntity(schema);
        dynamicEntityUpdate.set(
                "documentPending",
                new Document(customerProvidedDocumentPending.getId(), "filename"));

        Transaction partialUpdate =
                Transaction.builder()
                        .id(transactionId)
                        .customerProvidedDocuments(
                                new ArrayList<>(Arrays.asList(customerProvidedDocumentPending)))
                        .data(dynamicEntityUpdate)
                        .build();

        when(customerProvidedDocumentRepository.findById(customerProvidedDocumentPending.getId()))
                .thenReturn(Optional.of(customerProvidedDocumentPending));

        Transaction existingTransaction =
                Transaction.builder()
                        .id(transactionId)
                        .customerProvidedDocuments(
                                new ArrayList<>(
                                        Arrays.asList(
                                                customerProvidedDocumentNew,
                                                customerProvidedDocumentRejected)))
                        .data(new DynamicEntity(schema))
                        .build();

        service.updateDocuments(partialUpdate, existingTransaction, new HashMap<>());

        assertEquals(ReviewStatus.NEW, customerProvidedDocumentPending.getReviewStatus());
        assertEquals(3, existingTransaction.getCustomerProvidedDocuments().size());
    }

    @Test
    void testUpdateDocumentWithPathChange() {
        UUID transactionId = UUID.randomUUID();

        CustomerProvidedDocument customerProvidedDocumentPending =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .reviewStatus(ReviewStatus.PENDING)
                        .dataPath("documentPending")
                        .build();

        Schema schema =
                Schema.builder()
                        .properties(
                                List.of(new DynaProperty("documentPendingNewPath", Document.class)))
                        .build();

        DynamicEntity dynamicEntityUpdate = new DynamicEntity(schema);
        dynamicEntityUpdate.set(
                "documentPendingNewPath",
                new Document(customerProvidedDocumentPending.getId(), "filename"));

        Transaction partialUpdate =
                Transaction.builder()
                        .id(transactionId)
                        .customerProvidedDocuments(
                                new ArrayList<>(Arrays.asList(customerProvidedDocumentPending)))
                        .data(dynamicEntityUpdate)
                        .build();

        when(customerProvidedDocumentRepository.findById(customerProvidedDocumentPending.getId()))
                .thenReturn(Optional.of(customerProvidedDocumentPending));

        Transaction existingTransaction =
                Transaction.builder()
                        .id(transactionId)
                        .customerProvidedDocuments(new ArrayList<>())
                        .data(new DynamicEntity(schema))
                        .build();

        service.updateDocuments(partialUpdate, existingTransaction, new HashMap<>());

        assertEquals("documentPendingNewPath", customerProvidedDocumentPending.getDataPath());
    }

    @Test
    void hasAdminDataChangesAssignedTo() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .assignedTo(UUID.randomUUID().toString())
                        .build();

        boolean result =
                service.hasAdminDataChanges(transaction, UUID.randomUUID().toString(), "LOW");

        assertTrue(result);
    }

    @Test
    void hasAdminDataChangesPriority() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .priority(TransactionPriority.MEDIUM)
                        .build();

        boolean result =
                service.hasAdminDataChanges(transaction, UUID.randomUUID().toString(), "LOW");

        assertTrue(result);
    }

    @Test
    void hasNoAdminDataChanges() {
        String assignedTo = UUID.randomUUID().toString();
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .assignedTo(assignedTo)
                        .priority(TransactionPriority.LOW)
                        .build();

        boolean result = service.hasAdminDataChanges(transaction, assignedTo, "LOW");

        assertFalse(result);
    }

    @Test
    void testDocumentDeletionForListsEqualValues() {
        Schema schema =
                Schema.builder()
                        .key("testSchemaKey")
                        .properties(
                                List.of(
                                        new DynaProperty(
                                                "listOfDocuments", List.class, Document.class)))
                        .build();

        Document originalDocumentOne = new Document(UUID.randomUUID(), "filename");
        Document originalDocumentTwo = new Document(UUID.randomUUID(), "filename");

        DynamicEntity data = new DynamicEntity(schema);
        data.set("listOfDocuments", Arrays.asList(originalDocumentOne, originalDocumentTwo));

        CustomerProvidedDocument customerProvidedDocumentOne =
                CustomerProvidedDocument.builder()
                        .id(originalDocumentOne.getDocumentId())
                        .dataPath("listOfDocuments")
                        .active(true)
                        .build();
        CustomerProvidedDocument customerProvidedDocumentTwo =
                CustomerProvidedDocument.builder()
                        .id(originalDocumentTwo.getDocumentId())
                        .dataPath("listOfDocuments")
                        .active(true)
                        .build();

        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .data(data)
                        .customerProvidedDocuments(
                                Arrays.asList(
                                        customerProvidedDocumentOne, customerProvidedDocumentTwo))
                        .build();

        List<CustomerProvidedDocument> documents =
                service.updateDocuments(transaction, transaction, Map.of());

        assertEquals(2, documents.size());
        assertTrue(documents.get(0).getActive());
        assertTrue(documents.get(1).getActive());
    }

    @Test
    void testDocumentDeletionForListsOneDifferentValue() {
        Schema schema =
                Schema.builder()
                        .key("testSchemaKey")
                        .properties(
                                List.of(
                                        new DynaProperty(
                                                "listOfDocuments", List.class, Document.class)))
                        .build();

        Document originalDocumentOne = new Document(UUID.randomUUID(), "filename");
        Document originalDocumentTwo = new Document(UUID.randomUUID(), "filename");

        DynamicEntity data = new DynamicEntity(schema);
        data.set("listOfDocuments", Arrays.asList(originalDocumentOne, originalDocumentTwo));

        CustomerProvidedDocument customerProvidedDocumentOne =
                CustomerProvidedDocument.builder()
                        .id(originalDocumentOne.getDocumentId())
                        .dataPath("listOfDocuments")
                        .active(true)
                        .build();
        CustomerProvidedDocument customerProvidedDocumentTwo =
                CustomerProvidedDocument.builder()
                        .id(originalDocumentTwo.getDocumentId())
                        .dataPath("listOfDocuments")
                        .active(true)
                        .build();

        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .data(data)
                        .customerProvidedDocuments(
                                new ArrayList<>(
                                        Arrays.asList(
                                                customerProvidedDocumentOne,
                                                customerProvidedDocumentTwo)))
                        .build();

        Document newDocument = new Document(UUID.randomUUID(), "filename");
        DynamicEntity newData = new DynamicEntity(schema);
        newData.set("listOfDocuments", Arrays.asList(originalDocumentOne, newDocument));
        Transaction newTransaction =
                Transaction.builder().id(UUID.randomUUID()).data(newData).build();

        List<CustomerProvidedDocument> documents =
                service.updateDocuments(newTransaction, transaction, Map.of());

        assertEquals(3, documents.size());
        assertTrue(documents.get(0).getActive());
        assertEquals(documents.get(0).getId(), originalDocumentOne.getDocumentId());
        assertFalse(documents.get(1).getActive());
        assertEquals(documents.get(1).getId(), originalDocumentTwo.getDocumentId());
        assertTrue(documents.get(2).getActive());
        assertEquals(documents.get(2).getId(), newDocument.getDocumentId());
    }

    @Test
    void testUpdateCompletedTransactions() {
        when(repository.markTransactionsAsCompleted())
                .thenReturn(5); // Set the expected return value
        int updatedRows = service.updateCompletedTransactions();
        assertEquals(5, updatedRows);
        verify(repository, times(1)).markTransactionsAsCompleted();
    }
}
