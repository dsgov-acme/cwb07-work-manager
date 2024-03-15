package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.audit.transaction.StatusChangedAuditHandler;
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
class StatusChangedAuditHandlerTest {

    @Mock private AuditEventService transactionAuditEventService;

    @InjectMocks private StatusChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_statusChanged() {
        // Arrange
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .status("Draft")
                        .build();

        String originatorId = "originatorId";
        auditHandler.handlePreUpdateState(transaction);
        transaction.setStatus("Review");
        auditHandler.handlePostUpdateState(transaction);

        // Act
        auditHandler.publishAuditEvent(originatorId);

        // Capture and assert
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
                AuditActivityType.TRANSACTION_STATUS_CHANGED.getValue(),
                capturedEvent.getData().getActivityType());
        Assertions.assertEquals(
                "Draft", ((StateChangeEventData) capturedEvent.getData()).getOldState());
        Assertions.assertEquals(
                "Review", ((StateChangeEventData) capturedEvent.getData()).getNewState());
    }

    @Test
    void testPublishAuditEvent_statusDidNotChange() {
        // Arrange
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .status("Draft")
                        .build();

        String originatorId = "originatorId";
        auditHandler.handlePreUpdateState(transaction);
        auditHandler.handlePostUpdateState(transaction);

        // Act
        auditHandler.publishAuditEvent(originatorId);

        // Assert that no event is sent
        verify(transactionAuditEventService, never())
                .sendAuditEvent(any(AuditEventRequestObjectDto.class));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testPublishAuditEventException(CapturedOutput output) {
        // Arrange
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .status("Draft")
                        .build();

        String originatorId = "originatorId";
        auditHandler.handlePreUpdateState(transaction);
        transaction.setStatus("Review");
        auditHandler.handlePostUpdateState(transaction);

        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .sendAuditEvent(any(AuditEventRequestObjectDto.class));

        // Act
        auditHandler.publishAuditEvent(originatorId);

        // Assert
        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " status change in transaction "
                                        + transaction.getId()));
    }
}
