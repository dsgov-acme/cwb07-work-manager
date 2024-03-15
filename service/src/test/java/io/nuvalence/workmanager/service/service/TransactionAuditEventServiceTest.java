package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.AuditEvent;
import io.nuvalence.events.event.dto.ActivityEventData;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.logging.util.CorrelationIdContext;
import io.nuvalence.workmanager.service.events.PublisherTopic;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionAuditEventServiceTest {

    @Mock private EventGateway eventGateway;

    @Mock private PublisherProperties publisherProperties;

    @Mock private RequestContextTimestamp requestContextTimestamp;

    private AuditEventService transactionAuditEventService;
    private String oldCorrelationId;

    @BeforeEach
    void setup() {
        transactionAuditEventService =
                Mockito.spy(
                        new AuditEventService(
                                eventGateway, publisherProperties, requestContextTimestamp));
        ReflectionTestUtils.setField(
                transactionAuditEventService,
                "fullyQualifiedTopicName",
                PublisherTopic.AUDIT_EVENTS_RECORDING.name());

        // Store the old correlation ID
        oldCorrelationId = CorrelationIdContext.getCorrelationId();
    }

    @AfterEach
    public void tearDown() {
        // Restore the old correlation ID after the test
        CorrelationIdContext.setCorrelationId(oldCorrelationId);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    void testPostStateChangeEvent() {
        String originatorId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String summary = "summary";
        UUID businessObjectId = UUID.randomUUID();
        AuditEventBusinessObject auditEventBusinessObject = AuditEventBusinessObject.TRANSACTION;
        String oldState = "oldState";
        String newState = "newState";

        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        OffsetDateTime eventTime = OffsetDateTime.now();
        when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(eventTime);

        String correlationId = UUID.randomUUID().toString();
        CorrelationIdContext.setCorrelationId(correlationId);

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(originatorId)
                        .userId(userId)
                        .summary(summary)
                        .businessObjectId(businessObjectId)
                        .businessObjectType(auditEventBusinessObject)
                        .data(oldState, newState, "someActivityType")
                        .build();

        UUID result = transactionAuditEventService.sendAuditEvent(testEvent);

        verify(eventGateway).publishEvent(auditEventCaptor.capture(), anyString());
        AuditEvent capturedAuditEvent = auditEventCaptor.getValue();

        UUID capturedTraceId = capturedAuditEvent.getRequestContext().getTraceId();
        assertNotNull(capturedTraceId);
        assertEquals(correlationId, capturedTraceId.toString());
        assertEquals(eventTime, capturedAuditEvent.getMetadata().getTimestamp());
        assertEquals(AuditEvent.class.getSimpleName(), capturedAuditEvent.getMetadata().getType());
        assertEquals(StateChangeEventData.class, capturedAuditEvent.getEventData().getClass());
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    void testPostActivityAuditEvent() {
        String originatorId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String summary = "summary";
        UUID businessObjectId = UUID.randomUUID();
        AuditEventBusinessObject auditEventBusinessObject = AuditEventBusinessObject.TRANSACTION;
        String jsonData = "{}";
        AuditActivityType activityType = AuditActivityType.NOTE_ADDED;

        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        OffsetDateTime eventTime = OffsetDateTime.now();
        when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(eventTime);
        String correlationId = UUID.randomUUID().toString();
        CorrelationIdContext.setCorrelationId(correlationId);

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(originatorId)
                        .userId(userId)
                        .summary(summary)
                        .businessObjectId(businessObjectId)
                        .businessObjectType(auditEventBusinessObject)
                        .data(jsonData, activityType)
                        .build();

        UUID result = transactionAuditEventService.sendAuditEvent(testEvent);

        verify(eventGateway).publishEvent(auditEventCaptor.capture(), anyString());
        AuditEvent capturedAuditEvent = auditEventCaptor.getValue();
        UUID capturedTraceId = capturedAuditEvent.getRequestContext().getTraceId();
        assertNotNull(capturedTraceId);
        assertEquals(correlationId, capturedTraceId.toString());
        assertEquals(eventTime, capturedAuditEvent.getMetadata().getTimestamp());
        assertEquals(AuditEvent.class.getSimpleName(), capturedAuditEvent.getMetadata().getType());
        assertEquals(ActivityEventData.class, capturedAuditEvent.getEventData().getClass());
        assertNotNull(result);
    }

    @Test
    void testPostStateChangeEvent2() throws JsonProcessingException {
        // Arrange
        Map<String, String> oldStateMap = new HashMap<>();
        oldStateMap.put("key1", "value1");
        oldStateMap.put("key2", "value2");
        Map<String, String> newStateMap = new HashMap<>();
        newStateMap.put("key3", "value3");
        newStateMap.put("key4", "value4");

        final ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        UUID expectedEventId = UUID.randomUUID();

        String originatorId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String summary = "summary";
        UUID businessObjectId = UUID.randomUUID();
        AuditEventBusinessObject auditEventBusinessObject = AuditEventBusinessObject.TRANSACTION;

        String correlationId = UUID.randomUUID().toString();
        CorrelationIdContext.setCorrelationId(correlationId);

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(originatorId)
                        .userId(userId)
                        .summary(summary)
                        .businessObjectId(businessObjectId)
                        .businessObjectType(auditEventBusinessObject)
                        .data(oldStateMap, newStateMap, "test", "someActivityType")
                        .build();

        UUID result = transactionAuditEventService.sendAuditEvent(testEvent);

        verify(eventGateway).publishEvent(auditEventCaptor.capture(), anyString());
        AuditEvent capturedAuditEvent = auditEventCaptor.getValue();

        UUID capturedTraceId = capturedAuditEvent.getRequestContext().getTraceId();
        assertNotNull(capturedTraceId);
        assertEquals(correlationId, capturedTraceId.toString());

        StateChangeEventData data = (StateChangeEventData) capturedAuditEvent.getEventData();

        assertEquals("test", data.getData());

        // Assert
        assertNotNull(result);
        verify(eventGateway).publishEvent(any(), anyString());
    }
}
