package io.nuvalence.workmanager.service.audit.profile;

import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class IndividualProfileUserAccessLevelChangedAuditHandler
        implements AuditHandler<IndividualUserLink> {

    private static final String ACCESS_LEVEL = "accessLevel";

    private final Map<String, String> accessLevelBefore = new HashMap<>();
    private final Map<String, String> accessLevelAfter = new HashMap<>();

    private UUID ownerUserId;
    private String userId;

    private final AuditEventService individualAuditEventService;

    @Override
    public void handlePreUpdateState(IndividualUserLink subject) {
        ownerUserId = subject.getProfile().getOwnerUserId();
        userId = subject.getUserId().toString();
        accessLevelBefore.put(ACCESS_LEVEL, subject.getAccessLevel().getValue());
    }

    @Override
    public void handlePostUpdateState(IndividualUserLink subject) {
        accessLevelAfter.put(ACCESS_LEVEL, subject.getAccessLevel().getValue());
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        try {
            String eventSummary;

            if (!accessLevelBefore.get(ACCESS_LEVEL).equals(accessLevelAfter.get(ACCESS_LEVEL))) {
                eventSummary =
                        String.format(
                                "Profile user access level changed to [%s] for individual profile"
                                        + " user %s owned by %s. Previously it was [%s]",
                                accessLevelAfter.get(ACCESS_LEVEL),
                                userId,
                                ownerUserId,
                                accessLevelBefore.get(ACCESS_LEVEL));
            } else {
                return;
            }

            Map<String, Object> individualEventData =
                    Map.of(
                            "ownerUserId", ownerUserId,
                            "userId", userId);

            String individualEventDataJson =
                    SpringConfig.getMapper().writeValueAsString(individualEventData);

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(originatorId)
                            .userId(originatorId)
                            .summary(eventSummary)
                            .businessObjectId(UUID.fromString(userId))
                            .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                            .data(
                                    accessLevelBefore,
                                    accessLevelAfter,
                                    individualEventDataJson,
                                    AuditActivityType.INDIVIDUAL_PROFILE_USER_ACCESS_LEVEL_CHANGED
                                            .getValue())
                            .build();

            individualAuditEventService.sendAuditEvent(auditEvent);
        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for "
                            + " individual profile user access level  changed for user "
                            + userId;
            log.error(errorMessage, e);
        }
    }
}
