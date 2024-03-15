package io.nuvalence.workmanager.service.audit.profile;

import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class IndividualProfileOwnerChangedAuditHandler implements AuditHandler<Individual> {

    private UUID before;
    private UUID after;

    private UUID profileId;

    private final AuditEventService individualAuditEventService;

    @Override
    public void handlePreUpdateState(Individual subject) {
        profileId = subject.getId();
        before = subject.getOwnerUserId();
    }

    @Override
    public void handlePostUpdateState(Individual subject) {
        after = subject.getOwnerUserId();
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        try {
            String eventSummary = "";
            if (before != null && !before.equals(after)) {
                eventSummary =
                        String.format(
                                "Owner changed to [%s] for individual profile %s. Previously it was"
                                        + " owned by [%s]",
                                after, profileId, before);
            } else {
                return;
            }

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(originatorId)
                            .userId(originatorId)
                            .summary(eventSummary)
                            .businessObjectId(profileId)
                            .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                            .data(
                                    before.toString(),
                                    after.toString(),
                                    AuditActivityType.INDIVIDUAL_PROFILE_OWNER_CHANGED.getValue())
                            .build();

            individualAuditEventService.sendAuditEvent(auditEvent);
        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for "
                            + " individual owner changed in profile "
                            + profileId;
            log.error(errorMessage, e);
        }
    }
}
