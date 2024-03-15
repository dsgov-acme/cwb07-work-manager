package io.nuvalence.workmanager.service.audit.transaction;

import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.audit.util.AuditMapManagementUtility;
import io.nuvalence.workmanager.service.domain.dynamicschema.ComputedDynaProperty;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AuditHandler that records an audit event if the dynamic data of a Transaction has changed.
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicDataChangedAuditHandler implements AuditHandler<Transaction> {

    private String transactionExternalId;
    private final Map<String, String> before = new HashMap<>();
    private final Map<String, String> after = new HashMap<>();

    private UUID transactionId;

    private final AuditEventService transactionAuditEventService;
    private final EntityMapper mapper;

    @Override
    public void handlePreUpdateState(Transaction subject) {
        transactionId = subject.getId();
        transactionExternalId = subject.getExternalId();
        before.putAll(mapper.flattenDynaDataMap(subject.getData()));
        removeComputedFields("", subject.getData(), before);
    }

    @Override
    public void handlePostUpdateState(Transaction subject) {
        after.putAll(mapper.flattenDynaDataMap(subject.getData()));
        removeComputedFields("", subject.getData(), after);
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        AuditMapManagementUtility.removeCommonItems(before, after);
        try {
            String eventSummary;
            if (!before.isEmpty() || !after.isEmpty()) {
                eventSummary =
                        String.format(
                                "Transaction %s changed its dynamic data", transactionExternalId);
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
                                    null,
                                    AuditActivityType.TRANSACTION_DATA_UPDATED.getValue())
                            .build();

            transactionAuditEventService.sendAuditEvent(auditEvent);
        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for dynamic data"
                            + " change in transaction "
                            + transactionId;
            log.error(errorMessage, e);
        }
    }

    private void removeComputedFields(
            String prefix, DynamicEntity entity, Map<String, String> flattenedMap) {
        for (DynaProperty dynaProperty : entity.getSchema().getDynaProperties()) {
            String key = dynaProperty.getName();
            Object value = entity.get(dynaProperty.getName());
            if (value instanceof DynamicEntity dynamicEntity) {
                String nestedPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                removeComputedFields(nestedPrefix, dynamicEntity, flattenedMap);
            } else if (dynaProperty instanceof ComputedDynaProperty) {
                flattenedMap.remove(prefix + "." + key);
            }
        }
    }
}
