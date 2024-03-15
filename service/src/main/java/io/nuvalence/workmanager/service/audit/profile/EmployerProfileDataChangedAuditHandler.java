package io.nuvalence.workmanager.service.audit.profile;

import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.audit.util.AuditMapManagementUtility;
import io.nuvalence.workmanager.service.domain.profile.Employer;
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
public class EmployerProfileDataChangedAuditHandler implements AuditHandler<Employer> {

    private final Map<String, String> before = new HashMap<>();
    private final Map<String, String> after = new HashMap<>();

    private UUID profileId;

    private final AuditEventService employerAuditEventService;

    @Override
    public void handlePreUpdateState(Employer subject) {
        profileId = subject.getId();
        before.putAll(convertToMap(subject));
    }

    @Override
    public void handlePostUpdateState(Employer subject) {
        after.putAll(convertToMap(subject));
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        AuditMapManagementUtility.removeCommonItems(before, after);

        try {
            String eventSummary;
            if (!before.isEmpty() || !after.isEmpty()) {
                eventSummary = String.format("Data for employer profile %s changed.", profileId);
            } else {
                return;
            }

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(originatorId)
                            .userId(originatorId)
                            .summary(eventSummary)
                            .businessObjectId(profileId)
                            .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                            .data(
                                    before,
                                    after,
                                    null,
                                    AuditActivityType.EMPLOYER_PROFILE_DATA_CHANGED.getValue())
                            .build();

            employerAuditEventService.sendAuditEvent(auditEvent);
        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for employer"
                            + " profile data change for profile "
                            + profileId;
            log.error(errorMessage, e);
        }
    }

    private Map<String, String> convertToMap(Employer employer) {
        Map<String, String> map = new HashMap<>();

        if (employer == null) {
            return map;
        }

        map.put("id", employer.getId() != null ? employer.getId().toString() : null);
        map.put("fein", employer.getFein());
        map.put("legalName", employer.getLegalName());
        for (int i = 0; i < employer.getOtherNames().size(); i++) {
            map.put("otherNames[" + i + "]", employer.getOtherNames().get(i));
        }
        map.put("type", employer.getType());
        map.put("industry", employer.getIndustry());
        map.put("summaryOfBusiness", employer.getSummaryOfBusiness());
        map.put("businessPhone", employer.getBusinessPhone());
        map.putAll(
                AuditMapManagementUtility.convertAddressToMap(
                        "mailingAddress.", employer.getMailingAddress()));

        for (int i = 0; i < employer.getLocations().size(); i++) {
            map.putAll(
                    AuditMapManagementUtility.convertAddressToMap(
                            "locations[" + i + "].", employer.getLocations().get(i)));
        }

        return map;
    }
}
