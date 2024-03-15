package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.domain.TransactionRecordLinkType;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.generated.models.RecordUpdateRequest;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.models.RecordFilters;
import io.nuvalence.workmanager.service.repository.RecordRepository;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

class RecordServiceTest {

    @Mock private RecordRepository recordRepository;

    @Mock private RecordFactory recordFactory;

    @Mock private EntityMapper entityMapper;

    @Mock private ApplicationContext applicationContext;
    @Mock private TransactionService transactionService;

    @InjectMocks private RecordService recordService;

    private Clock clock = Clock.systemDefaultZone();

    private EntityMapper mapper;

    @BeforeEach
    void setUp() {
        openMocks(this);
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

        ObjectMapper objectMapper = new ObjectMapper();
        mapper = Mappers.getMapper(EntityMapper.class);
        mapper.setApplicationContext(applicationContext);
        mapper.setObjectMapper(objectMapper);
    }

    @Test
    void testCreateRecord() throws MissingSchemaException {
        // Mock data
        RecordDefinition recordDefinition = new RecordDefinition();
        Transaction transaction = new Transaction();
        Record record = new Record();

        // Mock behavior
        when(recordFactory.createRecord(any(RecordDefinition.class), any(Transaction.class)))
                .thenReturn(record);
        when(recordRepository.save(any(Record.class))).thenReturn(record);

        assertTrue(transaction.getRecordLinks().isEmpty());

        // Perform the test
        Record result = recordService.createRecord(recordDefinition, transaction);

        // Verify the interactions and assertions
        verify(recordFactory, times(1)).createRecord(recordDefinition, transaction);
        verify(recordRepository, times(1)).save(record);
        verify(transactionService, times(1)).updateTransaction(transaction);

        assertEquals(record, result);
        assertEquals(1, transaction.getRecordLinks().size());
        assertEquals(record.getId(), transaction.getRecordLinks().get(0).getRecordId());
        assertEquals(transaction, transaction.getRecordLinks().get(0).getTransaction());
        assertEquals(
                TransactionRecordLinkType.CREATED,
                transaction.getRecordLinks().get(0).getRecordLinkType());
    }

    @Test
    void testGetRecordById() {
        // Mock data
        UUID id = UUID.randomUUID();
        Record record = new Record();

        // Mock behavior
        when(recordRepository.findById(id)).thenReturn(Optional.of(record));

        // Perform the test
        Optional<Record> result = recordService.getRecordById(id);

        // Verify the interactions and assertions
        verify(recordRepository, times(1)).findById(id);
        assertEquals(Optional.of(record), result);
    }

    @Test
    void testGetRecordsByFilters() {
        // Mock data
        final RecordFilters filters =
                new RecordFilters(null, null, "externalId", "createdTimestamp", "ASC", 0, 2);
        Page<Record> recordPage = mock(Page.class);

        // Mock behavior
        when(recordRepository.findAll(any(), (Pageable) any())).thenReturn(recordPage);

        // Perform the test
        Page<Record> result = recordService.getRecordsByFilters(filters);

        // Verify the interactions and assertions
        verify(recordRepository, times(1)).findAll(any(), (Pageable) any());
        assertEquals(recordPage, result);
    }

    @Test
    void testUpdateRecord_NotFound() {
        RecordUpdateRequest updateRequest = new RecordUpdateRequest();
        UUID existingRecordId = UUID.randomUUID();
        when(recordRepository.findById(existingRecordId)).thenReturn(Optional.empty());

        Exception exception =
                assertThrows(
                        NotFoundException.class,
                        () -> {
                            recordService.updateRecord(updateRequest, existingRecordId, false);
                        });

        String expectedMessage = "Record not found";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testUpdateRecord_Forbidden() {
        RecordUpdateRequest updateRequest = new RecordUpdateRequest();
        updateRequest.setStatus("NEW_STATUS");

        UUID existingRecordId = UUID.randomUUID();

        Record existingRecord = Record.builder().build();
        when(recordRepository.findById(existingRecordId)).thenReturn(Optional.of(existingRecord));

        Exception exception =
                assertThrows(
                        ForbiddenException.class,
                        () -> {
                            recordService.updateRecord(updateRequest, existingRecordId, false);
                        });

        String expectedMessage = "User is not authorized to update admin data";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testUpdateRecord_Success() {
        RecordUpdateRequest updateRequest = new RecordUpdateRequest();
        String status = "NEW_STATUS";
        updateRequest.setStatus(status);
        OffsetDateTime expires = OffsetDateTime.now();
        updateRequest.setExpires(expires);

        HashMap<String, Object> data = new HashMap<>();
        data.put("key", "value");
        updateRequest.setData(data);

        UUID existingRecordId = UUID.randomUUID();

        DynamicEntity dynamicEntity =
                new DynamicEntity(
                        Schema.builder()
                                .properties(List.of(new DynaProperty("key", String.class)))
                                .build());
        Record existingRecord = Record.builder().data(dynamicEntity).build();
        when(recordRepository.findById(existingRecordId)).thenReturn(Optional.of(existingRecord));

        when(transactionService.unifyAttributeMaps(any(), any())).thenReturn(data);

        Record expected =
                Record.builder().status(status).expires(expires).data(dynamicEntity).build();
        when(recordRepository.save(any(Record.class))).thenReturn(expected);

        Record result = recordService.updateRecord(updateRequest, existingRecordId, true);

        assertEquals(expected, result);

        verify(recordRepository, times(1)).findById(existingRecordId);
        verify(transactionService, times(1)).unifyAttributeMaps(any(), any());
        verify(recordRepository, times(1)).save(any(Record.class));
    }

    @Test
    void testExpireRecords() {
        int expectedExpiredRecordsCount = 5;
        when(recordRepository.updateStatusForExpiredRecords())
                .thenReturn(expectedExpiredRecordsCount);

        recordService.expireRecords();
        verify(recordRepository, times(1)).updateStatusForExpiredRecords();
    }
}
