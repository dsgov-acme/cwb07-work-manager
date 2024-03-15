package io.nuvalence.workmanager.service.models.auditevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.ActivityEventData;
import io.nuvalence.events.event.dto.AuditEventDataBase;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.config.SpringConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a request object for audit event data, detailing the specifics of an audit event.
 * This class includes information such as the originator of the event, the user involved,
 * a summary of the event, and details about the business object associated with the event.
 */
@Getter
@Setter
@Builder
public class AuditEventRequestObjectDto {
    private AuditEventDataBase data; // Object containing specifics of the audit event.
    private String originatorId; // ID of the originator of the event.
    private String userId; // ID of the user involved in the event.
    private String summary; // Brief description of the event.
    private UUID businessObjectId; // ID of the business object involved in the event.
    private AuditEventBusinessObject
            businessObjectType; // Type of the business object involved in the event.

    public static class AuditEventRequestObjectDtoBuilder {
        public AuditEventRequestObjectDtoBuilder data(
                Map<String, String> oldStateMap,
                Map<String, String> newStateMap,
                String data,
                String activityType)
                throws JsonProcessingException {
            String oldState = SpringConfig.getMapper().writeValueAsString(oldStateMap);
            String newState = SpringConfig.getMapper().writeValueAsString(newStateMap);

            this.data = createStateChangeEventData(oldState, newState, activityType, data);
            return this;
        }

        public AuditEventRequestObjectDtoBuilder data(
                String oldState, String newState, String activityType) {
            this.data = createStateChangeEventData(oldState, newState, activityType, null);
            return this;
        }

        public AuditEventRequestObjectDtoBuilder data(
                String jsonData, AuditActivityType activityType) {
            this.data = createActivityEventData(jsonData, activityType);
            return this;
        }

        private StateChangeEventData createStateChangeEventData(
                String oldState, String newState, String activityType, String data) {
            StateChangeEventData stateChangeEventData = new StateChangeEventData();
            stateChangeEventData.setOldState(oldState);
            stateChangeEventData.setNewState(newState);
            stateChangeEventData.setActivityType(activityType);
            if (data != null) {
                stateChangeEventData.setData(data);
            }

            return stateChangeEventData;
        }

        private ActivityEventData createActivityEventData(
                String jsonData, AuditActivityType activityType) {
            ActivityEventData activityEventData = new ActivityEventData();
            activityEventData.setData(jsonData);
            activityEventData.setActivityType(activityType.getValue());

            return activityEventData;
        }
    }
}
