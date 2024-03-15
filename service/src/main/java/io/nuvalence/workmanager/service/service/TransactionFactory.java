package io.nuvalence.workmanager.service.service;

import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.repository.TransactionRepository;
import io.nuvalence.workmanager.service.utils.ZBase32Encoder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Factory that encapsulates transaction initialization logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class TransactionFactory {
    private final ProcessEngine processEngine;
    private final SchemaService schemaService;
    private final TransactionRepository repository;

    @Setter(AccessLevel.PACKAGE)
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Create a new transaction for a given transaction definition.
     *
     * @param definition Type of transaction to create
     * @return The newly created transaction
     * @throws MissingSchemaException if the transaction definition references a schema that does not exist
     */
    public Transaction createTransaction(final TransactionDefinition definition)
            throws MissingSchemaException {
        String createdByUserId = null;
        String subjectUserId = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ((authentication instanceof UserToken userToken)) {
            final String userId = userToken.getApplicationUserId();

            createdByUserId = userId;
            subjectUserId = userId;

            // TODO: When we start allowing users to create transactions on behalf of another
            // user and determine how
            //  to actually link the subject, we will need to update this with the real
            // subjectUserId. For now,
            //  it is just populated by the user that is creating the transaction. This will
            // also need to be
            //  fixed in TransactionApiDelegateImpl.java.

        }

        final Schema schema =
                schemaService
                        .getSchemaByKey(definition.getSchemaKey())
                        .orElseThrow(() -> new MissingSchemaException(definition.getSchemaKey()));
        final OffsetDateTime now = OffsetDateTime.now(clock);

        return Transaction.builder()
                .transactionDefinitionId(definition.getId())
                .transactionDefinitionKey(definition.getKey())
                .priority(
                        TransactionPriority
                                .MEDIUM) // default to medium TODO: check with FE dropdown to
                // ingest that
                .createdBy(createdByUserId)
                // processInstanceId and status will be set in TransactionStatusUpdateDelegate, an
                // empty string is set for now to avoid hibernate errors due to non-nullable field
                .processInstanceId("")
                .status("")
                .subjectUserId(subjectUserId)
                .createdTimestamp(now)
                .lastUpdatedTimestamp(now)
                .data(new DynamicEntity(schema))
                .externalId(generateExternalId())
                .build();
    }

    private String generateExternalId() {
        Long sequenceValue = repository.getNextTransactionSequenceValue();
        return ZBase32Encoder.encode(sequenceValue);
    }
}
