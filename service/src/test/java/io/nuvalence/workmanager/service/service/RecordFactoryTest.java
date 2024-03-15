package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class RecordFactoryTest {

    @Mock private SchemaService schemaService;

    @Mock private RecordRepository repository;

    @Mock private TransactionService transactionService;

    @Mock private EntityMapper entityMapper;

    private RecordService recordService;

    private RecordFactory factory;
    private Clock clock;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        factory = new RecordFactory(repository, schemaService);
        factory.setClock(clock);
        recordService = new RecordService(repository, factory, transactionService, entityMapper);
    }

    @Test
    void fabricateRecord() throws MissingSchemaException {
        // Arrange
        final RecordDefinition definition = createRecordDefinition();
        final Schema schema = mockSchema();
        final Transaction transaction =
                createTransaction(definition.getId(), definition.getKey(), schema);
        final Record record = createRecord(definition, transaction);

        // Mock the Authentication object to return null
        SecurityContextHolder.setContext(new SecurityContextImpl());

        // Act and Assert
        assertEquals(record, factory.createRecord(definition, transaction));
    }

    @Test
    void testCreateRecord() throws MissingSchemaException {
        // Arrange
        final RecordDefinition definition = createRecordDefinition();
        final Schema schema = mockSchema();
        final Transaction transaction =
                createTransaction(definition.getId(), definition.getKey(), schema);
        final Record record = createRecord(definition, transaction);

        Mockito.when(repository.save(any(Record.class))).thenReturn(record);

        // Act and Assert
        Record createdRecord = recordService.createRecord(definition, transaction);

        assertNotNull(createdRecord);
        assertEquals(definition.getId(), createdRecord.getRecordDefinition().getId());
        assertEquals(definition.getKey(), createdRecord.getRecordDefinitionKey());
        assertTrue(createdRecord.getStatus().isEmpty());

        verify(repository).save(any(Record.class));
    }

    @Test
    void createRecordThrowsMissingSchemaExceptionIfSchemaIsMissing() {
        // Arrange
        final RecordDefinition definition = createRecordDefinition();
        final Schema schema = mockSchema();
        final Transaction transaction =
                createTransaction(definition.getId(), definition.getKey(), schema);
        Mockito.lenient()
                .when(schemaService.getSchemaByKey(definition.getSchemaKey()))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                MissingSchemaException.class, () -> factory.createRecord(definition, transaction));
    }

    private RecordDefinition createRecordDefinition() {
        return RecordDefinition.builder()
                .id(UUID.randomUUID())
                .name("Test Record Definition")
                .key("key")
                .schemaKey("schemaKey")
                .expirationDuration(Period.ofDays(20))
                .build();
    }

    private Record createRecord(RecordDefinition recordDefinition, Transaction transaction) {
        return Record.builder()
                .recordDefinitionKey("key")
                .externalId("y")
                .recordDefinition(recordDefinition)
                .status("")
                .expires(OffsetDateTime.now(clock).plus(recordDefinition.getExpirationDuration()))
                .createdTimestamp(OffsetDateTime.now(clock))
                .createdFrom(transaction)
                .lastUpdatedFrom(transaction)
                .lastUpdatedTimestamp(OffsetDateTime.now(clock))
                .data(new DynamicEntity(Schema.builder().build()))
                .build();
    }

    private Schema mockSchema() {
        final Schema schema = Schema.builder().name("schema").build();
        Mockito.when(schemaService.getSchemaByKey(any())).thenReturn(Optional.of(schema));
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
