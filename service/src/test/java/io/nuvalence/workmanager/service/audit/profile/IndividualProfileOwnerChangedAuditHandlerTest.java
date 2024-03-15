package io.nuvalence.workmanager.service.audit.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.domain.profile.Individual;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class IndividualProfileOwnerChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private IndividualProfileOwnerChangedAuditHandler auditHandler;

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        UUID ownerUserIdOld = UUID.randomUUID();
        Individual individual =
                Individual.builder().id(UUID.randomUUID()).ownerUserId(ownerUserIdOld).build();

        auditHandler.handlePreUpdateState(individual);

        UUID ownerUserIdNew = UUID.randomUUID();
        individual.setOwnerUserId(ownerUserIdNew);
        auditHandler.handlePostUpdateState(individual);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put("ownerUserId", ownerUserIdOld.toString());

        Map<String, String> after = new HashMap<>();
        after.put("ownerUserId", ownerUserIdNew.toString());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(individual.getId(), capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.INDIVIDUAL, capturedEvent.getBusinessObjectType());
        StateChangeEventData eventDataResult = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals(ownerUserIdOld.toString(), eventDataResult.getOldState());
        Assertions.assertEquals(ownerUserIdNew.toString(), eventDataResult.getNewState());
        Assertions.assertEquals(
                String.format(
                        "Owner changed to [%s] for individual profile %s. Previously it was owned"
                                + " by [%s]",
                        ownerUserIdNew, individual.getId(), ownerUserIdOld),
                capturedEvent.getSummary());
    }

    @Test
    void test_publishAuditEvent_WithNoChanges() {
        Individual individual = Individual.builder().ownerUserId(UUID.randomUUID()).build();

        auditHandler.handlePreUpdateState(individual);
        auditHandler.handlePostUpdateState(individual);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }
}
