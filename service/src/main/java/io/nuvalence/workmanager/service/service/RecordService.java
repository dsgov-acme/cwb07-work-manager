package io.nuvalence.workmanager.service.service;

import io.micrometer.common.util.StringUtils;
import io.nuvalence.workmanager.service.domain.TransactionRecordLinkType;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionRecordLink;
import io.nuvalence.workmanager.service.generated.models.RecordUpdateRequest;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.models.RecordFilters;
import io.nuvalence.workmanager.service.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Service for managing records.
 */
@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RecordService {
    private final RecordRepository repository;
    private final RecordFactory factory;
    private final TransactionService transactionService;
    private final EntityMapper entityMapper;

    /**
     * Create a new Record for a given record definition.
     *
     * @param definition Type of transaction to create
     * @return The newly created transaction
     * @throws MissingSchemaException if the transaction definition references a schema that does not exist
     */
    public Record createRecord(RecordDefinition definition, Transaction transaction)
            throws MissingSchemaException {

        final Record newRecord = repository.save(factory.createRecord(definition, transaction));

        transaction
                .getRecordLinks()
                .add(
                        TransactionRecordLink.builder()
                                .recordId(newRecord.getId())
                                .transaction(transaction)
                                .recordLinkType(TransactionRecordLinkType.CREATED)
                                .build());

        transactionService.updateTransaction(transaction);

        return newRecord;
    }

    public Optional<Record> getRecordById(final UUID id) {
        return repository.findById(id);
    }

    public Page<Record> getRecordsByFilters(RecordFilters filter) {

        return repository.findAll(filter.getRecordSpecification(), filter.getPageRequest());
    }

    /**
     * Update a record.
     *
     * @param updateRequest the update request
     * @param existingRecordId the existing record id
     * @param isAdmin flag to determine if the user is an admin
     * @return the record
     */
    public Record updateRecord(
            RecordUpdateRequest updateRequest, UUID existingRecordId, Boolean isAdmin) {

        final Record existingRecord =
                getRecordById(existingRecordId)
                        .orElseThrow(() -> new NotFoundException("Record not found"));

        if (Boolean.FALSE.equals(isAdmin)
                && hasAdminDataChanges(
                        existingRecord,
                        updateRequest.getStatus(),
                        updateRequest.getExpires() != null
                                ? updateRequest.getExpires().toString()
                                : null)) {
            throw new ForbiddenException("User is not authorized to update admin data");
        }

        if (!StringUtils.isBlank(updateRequest.getStatus())) {
            existingRecord.setStatus(updateRequest.getStatus());
        }

        if (!StringUtils.isBlank(String.valueOf(updateRequest.getExpires()))) {
            existingRecord.setExpires(updateRequest.getExpires());
        }

        if (updateRequest.getData() != null) {
            final Map<String, Object> mergedMap =
                    transactionService.unifyAttributeMaps(
                            updateRequest.getData(),
                            entityMapper.convertAttributesToGenericMap(existingRecord.getData()));

            final Schema schema = existingRecord.getData().getSchema();
            try {
                existingRecord.setData(entityMapper.convertGenericMapToEntity(schema, mergedMap));
            } catch (MissingSchemaException e) {
                log.error(
                        String.format(
                                "Record [%s] references missing schema.", existingRecord.getId()));
            }
        }

        return repository.save(existingRecord);
    }

    /**
     * Check if the record has admin data changes.
     *
     * @param originalRecord the original record
     * @param status the status
     * @param expires the expires
     * @return true if the record has admin data changes
     */
    private boolean hasAdminDataChanges(Record originalRecord, String status, String expires) {

        if (status != null && !status.equals(originalRecord.getStatus())) {
            return true;
        }

        return expires != null && !expires.equals(originalRecord.getExpires().toString());
    }

    public int expireRecords() {
        return repository.updateStatusForExpiredRecords();
    }
}
