package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.audit.transaction.TransactionNoteChangedAuditHandler;
import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionNoteChangedAuditHandlerTest {

    @Mock private AuditEventService transactionAuditEventService;

    @InjectMocks private TransactionNoteChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_statusChanged() throws JsonProcessingException {
        TransactionNote note = new TransactionNote();
        note.setId(UUID.randomUUID());
        note.setTitle("title");
        note.setBody("Body");
        note.setTransactionId(UUID.randomUUID());
        UUID documentId = UUID.randomUUID();
        note.setDocuments(List.of(documentId));
        UUID typeId = UUID.randomUUID();
        note.setType(new NoteType(typeId, "myType"));

        String originatorId = "originatorId";

        auditHandler.handlePreUpdateState(note);
        note.setTitle("new title");
        note.setBody("new body");
        UUID newDocumentId = UUID.randomUUID();
        note.setDocuments(List.of(newDocumentId));
        UUID newTypeId = UUID.randomUUID();
        note.setType(new NoteType(newTypeId, "newType"));
        auditHandler.handlePostUpdateState(note);
        auditHandler.publishAuditEvent(originatorId);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        Assertions.assertNotNull(capturedAuditEvent);
        Assertions.assertEquals(originatorId, capturedAuditEvent.getOriginatorId());
        Assertions.assertEquals(note.getTransactionId(), capturedAuditEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.TRANSACTION, capturedAuditEvent.getBusinessObjectType());

        StateChangeEventData eventData = (StateChangeEventData) capturedAuditEvent.getData();
        Assertions.assertNotNull(eventData);
        Assertions.assertEquals(
                String.format(
                        "{\"noteType.name\":\"myType\",\"noteType.id\":\"%s\",\"title\":\"title\",\"body\":\"Body\",\"documents[0]\":\"%s\"}",
                        typeId, documentId),
                eventData.getOldState());
        Assertions.assertEquals(
                String.format(
                        "{\"noteType.name\":\"newType\",\"noteType.id\":\"%s\",\"title\":\"new"
                                + " title\",\"body\":\"new body\",\"documents[0]\":\"%s\"}",
                        newTypeId, newDocumentId),
                eventData.getNewState());
        Assertions.assertEquals(
                AuditActivityType.NOTE_UPDATED.getValue(), eventData.getActivityType());
    }

    @Test
    void testPublishAuditEvent_statusDidNotChange() throws JsonProcessingException {
        TransactionNote note = new TransactionNote();
        note.setId(UUID.randomUUID());
        note.setTitle("title");
        note.setBody("Body");
        note.setTransactionId(UUID.randomUUID());
        note.setType(new NoteType(UUID.randomUUID(), "myType"));

        auditHandler.handlePreUpdateState(note);
        auditHandler.handlePostUpdateState(note);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService, never()).sendAuditEvent(any());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testPublishAuditEvent_throws_Exception(CapturedOutput output)
            throws JsonProcessingException {

        TransactionNote note = new TransactionNote();
        note.setId(UUID.randomUUID());
        auditHandler.handlePreUpdateState(note);
        note.setBody("new body");
        auditHandler.handlePostUpdateState(note);

        String originatorId = "originatorId";
        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        doThrow(RuntimeException.class).when(transactionAuditEventService).sendAuditEvent(any());

        auditHandler.publishAuditEvent(originatorId);
        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " transaction note change in transaction "
                                        + note.getId()));
    }
}
