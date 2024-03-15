package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.repository.CustomerProvidedDocumentRepository;
import io.nuvalence.workmanager.service.repository.TransactionRepository;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.formconfig.formio.NuvalenceFormioValidator;
import org.camunda.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionFactoryTest {
    @Mock private ProcessEngine processEngine;

    @Mock private SchemaService schemaService;

    @Mock private TransactionRepository repository;

    @Mock private CustomerProvidedDocumentRepository customerProvidedDocumentRepository;

    @Mock private TransactionTaskService transactionTaskService;

    @Mock private WorkflowTasksService workflowTasksService;

    @Mock private AuditEventService transactionAuditEventService;

    @Mock private RequestContextTimestamp requestContextTimestamp;

    @Mock private FormConfigurationService formConfigurationService;

    @Mock private TransactionDefinitionService transactionDefinitionService;

    @Mock private FormConfigurationMapper formConfigMapper;

    @Mock private NuvalenceFormioValidator formioValidator;

    @Mock private IndividualService individualService;
    @Mock private EmployerService employerService;

    private TransactionService transactionService;
    private TransactionFactory factory;
    private Clock clock;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        factory = new TransactionFactory(processEngine, schemaService, repository);
        factory.setClock(clock);
        transactionService =
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
                        employerService);
    }

    @Test
    void fabricateTransaction() throws MissingSchemaException {
        // Arrange
        final TransactionDefinition definition = createTransactionDefinition();
        final Schema schema = mockSchema();
        final Transaction transaction =
                createTransaction(definition.getId(), definition.getKey(), schema);

        // Mock the Authentication object to return null
        SecurityContextHolder.setContext(new SecurityContextImpl());

        // Act and Assert
        assertEquals(transaction, factory.createTransaction(definition));
    }

    @Test
    void testCreateTransaction() throws MissingSchemaException {
        // Arrange
        final TransactionDefinition definition = createTransactionDefinition();
        final Schema schema = mockSchema();
        final Transaction transaction =
                createTransaction(definition.getId(), definition.getKey(), schema);

        Mockito.when(repository.save(any(Transaction.class))).thenReturn(transaction);

        Mockito.doNothing()
                .when(transactionTaskService)
                .startTask(transaction, definition.getProcessDefinitionKey());

        // Act and Assert
        Transaction createdTransaction = transactionService.createTransaction(definition, "jwt");

        assertNotNull(createdTransaction);
        assertEquals(definition.getId(), createdTransaction.getTransactionDefinitionId());
        assertEquals(definition.getKey(), createdTransaction.getTransactionDefinitionKey());
        assertTrue(createdTransaction.getStatus().isEmpty());
        assertTrue(createdTransaction.getProcessInstanceId().isEmpty());

        verify(repository).save(any(Transaction.class));
        verify(transactionTaskService).startTask(transaction, definition.getProcessDefinitionKey());
    }

    @Test
    void createTransactionWithToken() throws MissingSchemaException {
        // Arrange
        final TransactionDefinition definition = createTransactionDefinition();
        final Schema schema = mockSchema();

        final Optional<User> mockUser = createUser();

        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(securityContext.getAuthentication())
                .thenReturn(
                        UserToken.builder()
                                .providerUserId("EXT000123")
                                .applicationUserId(mockUser.get().getId().toString())
                                .roles(Collections.emptyList())
                                .build());

        final Transaction transaction =
                Transaction.builder()
                        .transactionDefinitionId(definition.getId())
                        .transactionDefinitionKey(definition.getKey())
                        .processInstanceId("")
                        .status("")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(mockUser.get().getId().toString())
                        .subjectUserId(mockUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now(clock))
                        .lastUpdatedTimestamp(OffsetDateTime.now(clock))
                        .data(new DynamicEntity(schema))
                        .externalId("y")
                        .build();

        // Act and Assert
        assertEquals(transaction, factory.createTransaction(definition));
    }

    @Test
    void createTransactionThrowsMissingSchemaExceptionIfSchemaIsMissing() {
        // Arrange
        final TransactionDefinition definition = createTransactionDefinition();
        final Optional<User> mockUser = createUser();
        Mockito.lenient()
                .when(schemaService.getSchemaByKey(definition.getSchemaKey()))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(MissingSchemaException.class, () -> factory.createTransaction(definition));
    }

    private Optional<User> createUser() {
        return Optional.ofNullable(
                User.builder().email("someEmail@something.com").id(UUID.randomUUID()).build());
    }

    private TransactionDefinition createTransactionDefinition() {
        return TransactionDefinition.builder()
                .id(UUID.randomUUID())
                .key("key")
                .name("Transaction")
                .defaultStatus("status")
                .schemaKey("schema")
                .category("")
                .processDefinitionKey("process-id")
                .build();
    }

    private Schema mockSchema() {
        final Schema schema = Schema.builder().name("schema").build();
        Mockito.when(schemaService.getSchemaByKey(schema.getName()))
                .thenReturn(Optional.of(schema));
        return schema;
    }

    private Transaction createTransaction(
            UUID transactionDefinitionId, String transactionDefinitionKey, Schema schema) {
        return Transaction.builder()
                .transactionDefinitionId(transactionDefinitionId)
                .transactionDefinitionKey(transactionDefinitionKey)
                .processInstanceId("")
                .status("")
                .priority(TransactionPriority.MEDIUM)
                .createdTimestamp(OffsetDateTime.now(clock))
                .lastUpdatedTimestamp(OffsetDateTime.now(clock))
                .data(new DynamicEntity(schema))
                .externalId("y")
                .build();
    }
}
