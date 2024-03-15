package io.nuvalence.workmanager.service.audit.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.domain.profile.Address;
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
class IndividualProfileDataChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private IndividualProfileDataChangedAuditHandler auditHandler;

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        Individual individual = createIndividual();

        auditHandler.handlePreUpdateState(individual);
        individual.setSsn("New SSN");
        auditHandler.handlePostUpdateState(individual);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put("ssn", "ssn");

        Map<String, String> after = new HashMap<>();
        after.put("ssn", "New SSN");

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(individual.getId(), capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.INDIVIDUAL, capturedEvent.getBusinessObjectType());
        Assertions.assertEquals(
                String.format("Data for individual profile %s changed.", individual.getId()),
                capturedEvent.getSummary());
        StateChangeEventData eventDataResult = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals(
                before.toString(),
                eventDataResult.getOldState().replace("\"", "").replace(":", "="));
        Assertions.assertEquals(
                after.toString(),
                eventDataResult.getNewState().replace("\"", "").replace(":", "="));
    }

    @Test
    void test_publishAuditEvent_WithNoChanges() {
        Individual individual = createIndividual();

        auditHandler.handlePreUpdateState(individual);
        auditHandler.handlePostUpdateState(individual);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }

    private Individual createIndividual() {
        Address address =
                Address.builder()
                        .id(UUID.randomUUID())
                        .address1("addressLine1")
                        .address2("addressLine2")
                        .city("city")
                        .state("state")
                        .postalCode("zipCode")
                        .build();

        return Individual.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .ssn("ssn")
                .mailingAddress(address)
                .primaryAddress(address)
                .build();
    }
}
