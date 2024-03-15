package io.nuvalence.workmanager.service.service;

import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.repository.RecordRepository;
import io.nuvalence.workmanager.service.utils.ZBase32Encoder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Factory that encapsulates record initialization logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecordFactory {
    private final RecordRepository repository;
    private final SchemaService schemaService;

    @Setter(AccessLevel.PACKAGE)
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Create a new record for a given record definition.
     *
     * @param definition Type of record to create
     * @return The newly created record
     * @throws MissingSchemaException if the record definition references a schema that does not exist
     */
    public Record createRecord(RecordDefinition definition, Transaction transaction)
            throws MissingSchemaException {
        String createdByUserId = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ((authentication instanceof UserToken token)) {
            createdByUserId = token.getApplicationUserId();
        }

        final Schema schema =
                schemaService
                        .getSchemaByKey(definition.getSchemaKey())
                        .orElseThrow(() -> new MissingSchemaException(definition.getSchemaKey()));

        final OffsetDateTime now = OffsetDateTime.now(clock);

        return Record.builder()
                .externalId(generateExternalId())
                .recordDefinitionKey(definition.getKey())
                .status("")
                .expires(
                        definition.getExpirationDuration() == null
                                ? null
                                : now.plus(definition.getExpirationDuration()))
                .createdBy(createdByUserId)
                .recordDefinition(definition)
                .createdFrom(transaction)
                .createdTimestamp(now)
                .lastUpdatedBy(createdByUserId)
                .lastUpdatedTimestamp(now)
                .lastUpdatedFrom(transaction)
                .data(new DynamicEntity(schema))
                .build();
    }

    private String generateExternalId() {
        Long sequenceValue = repository.getNextTransactionSequenceValue();
        return ZBase32Encoder.encode(sequenceValue);
    }
}
