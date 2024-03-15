package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.audit.transaction.AssignedToChangedAuditHandler;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
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
class AssignedToChangedAuditHandlerTest {

    @Mock private AuditEventService transactionAuditEventService;
    @InjectMocks private AssignedToChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_UserAssigned() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo("user2");
        auditHandler.handlePostUpdateState(transaction);
        String originatorId = "originatorId";
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
                "User [user2] was assigned transaction externalId. Previously it had been"
                        + " assigned to [user1]",
                capturedEvent.getSummary());
        StateChangeEventData eventData = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals("user1", eventData.getOldState());
        Assertions.assertEquals("user2", eventData.getNewState());
        Assertions.assertEquals(
                AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue(),
                eventData.getActivityType());
    }

    @Test
    void testPublishAuditEvent_UserAssigned_beforenull() {
        Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).externalId("externalId").build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo("user2");
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
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
                "User [user2] was assigned transaction externalId", capturedEvent.getSummary());
        StateChangeEventData eventData = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertNull(eventData.getOldState());
        Assertions.assertEquals("user2", eventData.getNewState());
        Assertions.assertEquals(
                AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue(),
                eventData.getActivityType());
    }

    @Test
    void testPublishAuditEvent_UserAssigned_afternull() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo(null);
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
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
                "User [user1] was unassigned from transaction externalId",
                capturedEvent.getSummary());
        StateChangeEventData eventData = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals("user1", eventData.getOldState());
        Assertions.assertNull(eventData.getNewState());
        Assertions.assertEquals(
                AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue(),
                eventData.getActivityType());
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testPublishAuditEvent_Exception_path(CapturedOutput output) {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo("user2");
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        doThrow(RuntimeException.class).when(transactionAuditEventService).sendAuditEvent(any());

        auditHandler.publishAuditEvent(originatorId);

        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " assigned to change in transaction "
                                        + transaction.getId()));
    }
}
