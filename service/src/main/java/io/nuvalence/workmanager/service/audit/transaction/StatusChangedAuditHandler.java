package io.nuvalence.workmanager.service.audit.transaction;

import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * AuditHandler that records an audit event if the status of a Transaction has changed.
 */
@Slf4j
@RequiredArgsConstructor
public class StatusChangedAuditHandler implements AuditHandler<Transaction> {
    private String transactionExternalId;
    private String before;
    private String after;

    private UUID transactionId;

    private final AuditEventService transactionAuditEventService;

    @Override
    public void handlePreUpdateState(Transaction subject) {
        transactionId = subject.getId();
        transactionExternalId = subject.getExternalId();
        before = subject.getStatus();
    }

    @Override
    public void handlePostUpdateState(Transaction subject) {
        after = subject.getStatus();
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        try {
            String eventSummary;
            if (before != null && !before.equals(after)) {
                eventSummary =
                        String.format(
                                "Transaction %s changed its status to [%s]. Previously it"
                                        + " was [%s]",
                                transactionExternalId, after, before);
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
                                    AuditActivityType.TRANSACTION_STATUS_CHANGED.getValue())
                            .build();

            transactionAuditEventService.sendAuditEvent(auditEvent);
        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for status"
                            + " change in transaction "
                            + transactionId;
            log.error(errorMessage, e);
        }
    }
}
