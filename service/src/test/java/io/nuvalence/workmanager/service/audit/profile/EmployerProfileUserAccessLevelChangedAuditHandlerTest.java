package io.nuvalence.workmanager.service.audit.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
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
class EmployerProfileUserAccessLevelChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private EmployerProfileUserAccessLevelChangedAuditHandler auditHandler;

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        UUID ownerUserId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        ProfileAccessLevel accessLevelOld = ProfileAccessLevel.READER;
        Employer employer =
                Employer.builder()
                        .id(UUID.randomUUID())
                        .createdBy(String.valueOf(ownerUserId))
                        .build();

        EmployerUserLink employerUserLink =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(employer)
                        .userId(UUID.fromString(userId))
                        .profileAccessLevel(accessLevelOld)
                        .build();

        auditHandler.handlePreUpdateState(employerUserLink);

        ProfileAccessLevel profileAccessLevelNew = ProfileAccessLevel.WRITER;
        employerUserLink.setProfileAccessLevel(profileAccessLevelNew);
        auditHandler.handlePostUpdateState(employerUserLink);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put("accessLevel", accessLevelOld.toString());

        Map<String, String> after = new HashMap<>();
        after.put("accessLevel", profileAccessLevelNew.toString());

        Map<String, Object> eventData =
                Map.of(
                        "ownerUserId", ownerUserId,
                        "userId", userId);
        String eventDataJson = SpringConfig.getMapper().writeValueAsString(eventData);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(employer.getId(), capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.EMPLOYER, capturedEvent.getBusinessObjectType());
        Assertions.assertEquals(
                String.format(
                        "Profile user access level changed to [%s] for employer profile user %s"
                                + " owned by %s. Previously it was [%s]",
                        profileAccessLevelNew, userId, ownerUserId, accessLevelOld),
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
        EmployerUserLink employerUserLink =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(
                                Employer.builder()
                                        .id(UUID.randomUUID())
                                        .createdBy(String.valueOf(UUID.randomUUID()))
                                        .build())
                        .userId(UUID.randomUUID())
                        .profileAccessLevel(ProfileAccessLevel.ADMIN)
                        .build();
        auditHandler.handlePreUpdateState(employerUserLink);
        auditHandler.handlePostUpdateState(employerUserLink);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }
}
