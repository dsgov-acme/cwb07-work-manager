package io.nuvalence.workmanager.service.audit.transaction;

import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * AuditHandler that records an audit event if the priority attribute of a Transaction has changed.
 */
@Slf4j
@RequiredArgsConstructor
public class PriorityChangedAuditHandler implements AuditHandler<Transaction> {
    private String transactionExternalId;
    private String before;
    private String after;

    private UUID transactionId;

    private final AuditEventService transactionAuditEventService;

    @Override
    public void handlePreUpdateState(Transaction subject) {
        transactionId = subject.getId();
        transactionExternalId = subject.getExternalId();
        before = subject.getPriority() != null ? subject.getPriority().name() : "";
    }

    @Override
    public void handlePostUpdateState(Transaction subject) {
        after = subject.getPriority() != null ? subject.getPriority().name() : "";
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        try {
            String eventSummary = "";
            if (!Objects.equals(before, after)) {
                eventSummary =
                        String.format("Transaction %s priority was changed", transactionExternalId);
            } else {
                return;
            }

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(originatorId)
                            .userId(originatorId)
                            .summary(eventSummary)
                            .businessObjectId(transactionId)
                            .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                            .data(
                                    before,
                                    after,
                                    AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue())
                            .build();

            transactionAuditEventService.sendAuditEvent(auditEvent);

        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for priority"
                            + " change in transaction "
                            + transactionId;
            log.error(errorMessage, e);
        }
    }
}
