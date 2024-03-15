package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.audit.transaction.PriorityChangedAuditHandler;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.service.AuditEventService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PriorityChangedAuditHandlerTest {

    @Mock private AuditEventService transactionAuditEventService;

    @InjectMocks private PriorityChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_priorityChanged_success() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .priority(TransactionPriority.LOW)
                        .build();
        String originatorId = "originatorId";
        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(TransactionPriority.MEDIUM);
        auditHandler.handlePostUpdateState(transaction);

        auditHandler.publishAuditEvent(originatorId);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(transaction.getId(), capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.TRANSACTION, capturedEvent.getBusinessObjectType());
        Assertions.assertEquals(
                "Transaction externalId priority was changed", capturedEvent.getSummary());
        StateChangeEventData eventData = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals(TransactionPriority.LOW.name(), eventData.getOldState());
        Assertions.assertEquals(TransactionPriority.MEDIUM.name(), eventData.getNewState());
        Assertions.assertEquals(
                AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue(),
                eventData.getActivityType());
    }

    @Test
    void testPublishAuditEvent_priorityChanged_beforenull() {
        Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).externalId("externalId").build();
        String originatorId = "originatorId";
        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(TransactionPriority.MEDIUM);
        auditHandler.handlePostUpdateState(transaction);

        auditHandler.publishAuditEvent(originatorId);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(transaction.getId(), capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.TRANSACTION, capturedEvent.getBusinessObjectType());
        Assertions.assertEquals(
                "Transaction externalId priority was changed", capturedEvent.getSummary());
        StateChangeEventData eventData = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals("", eventData.getOldState());
        Assertions.assertEquals(TransactionPriority.MEDIUM.name(), eventData.getNewState());
        Assertions.assertEquals(
                AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue(),
                eventData.getActivityType());
    }

    @Test
    void testPublishAuditEvent_priorityChanged_afternull() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .priority(TransactionPriority.LOW)
                        .build();
        String originatorId = "originatorId";
        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(null);
        auditHandler.handlePostUpdateState(transaction);

        auditHandler.publishAuditEvent(originatorId);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(transaction.getId(), capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.TRANSACTION, capturedEvent.getBusinessObjectType());
        Assertions.assertEquals(
                "Transaction externalId priority was changed", capturedEvent.getSummary());
        StateChangeEventData eventData = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals(TransactionPriority.LOW.name(), eventData.getOldState());
        Assertions.assertEquals("", eventData.getNewState());
        Assertions.assertEquals(
                AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue(),
                eventData.getActivityType());
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testPublishAuditEvent_general_Exception(CapturedOutput output) {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .priority(TransactionPriority.LOW)
                        .build();
        String originatorId = "originatorId";
        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(TransactionPriority.MEDIUM);
        auditHandler.handlePostUpdateState(transaction);

        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .sendAuditEvent(any(AuditEventRequestObjectDto.class));

        auditHandler.publishAuditEvent(originatorId);

        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " priority change in transaction "
                                        + transaction.getId()));
    }
}
