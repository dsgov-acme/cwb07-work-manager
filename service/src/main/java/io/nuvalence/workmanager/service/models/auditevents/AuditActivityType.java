package io.nuvalence.workmanager.service.models.auditevents;

import lombok.Getter;

/**
 * Types of activities for auditable actions.
 */
public enum AuditActivityType {
    NOTE_ADDED("note_added"),
    NOTE_UPDATED("note_updated"),
    NOTE_DELETED("note_deleted"),
    TRANSACTION_SUBMITTED("transaction_submitted"),
    TRANSACTION_CREATED("transaction_created"),
    TRANSACTION_ASSIGNED_TO_CHANGED("transaction_assigned_to_changed"),
    DOCUMENT_ACCEPTED("document_accepted"),
    DOCUMENT_REJECTED("document_rejected"),
    DOCUMENT_UNACCEPTED("document_unaccepted"),
    DOCUMENT_UNREJECTED("document_unrejected"),
    TRANSACTION_DATA_UPDATED("transaction_data_changed"),
    TRANSACTION_STATUS_CHANGED("transaction_status_changed"),
    TRANSACTION_PRIORITY_CHANGED("transaction_priority_changed"),

    EMPLOYER_PROFILE_CREATED("employer_profile_created"),

    INDIVIDUAL_PROFILE_CREATED("individual_profile_created"),

    EMPLOYER_PROFILE_DATA_CHANGED("employer_profile_data_changed"),

    INDIVIDUAL_PROFILE_DATA_CHANGED("individual_profile_data_changed"),

    INDIVIDUAL_PROFILE_OWNER_CHANGED("individual_profile_owner_changed"),

    INDIVIDUAL_PROFILE_USER_ADDED("individual_profile_user_added"),
    INDIVIDUAL_PROFILE_USER_REMOVED("individual_profile_user_removed"),

    INDIVIDUAL_PROFILE_USER_ACCESS_LEVEL_CHANGED("individual_profile_user_access_level_changed"),
    EMPLOYER_PROFILE_USER_ADDED("employer_profile_user_added"),
    EMPLOYER_PROFILE_USER_REMOVED("employer_profile_user_removed"),

    EMPLOYER_PROFILE_USER_ACCESS_LEVEL_CHANGED("employer_profile_user_access_level_changed"),

    PROFILE_INVITATION_SENT("profile_invitation_sent"),

    PROFILE_INVITATION_CLAIMED("profile_invitation_claimed"),

    PROFILE_INVITATION_DELETED("profile_invitation_deleted");

    @Getter private String value;

    AuditActivityType(String value) {
        this.value = value;
    }

    /**
     * Gets an enum value from string.
     *
     * @param value string value of enum.
     * @return an enum object.
     *
     * @throws IllegalArgumentException if value is not a valid enum value.
     */
    public static AuditActivityType fromValue(String value) {
        for (AuditActivityType activityType : AuditActivityType.values()) {
            if (activityType.getValue().equals(value)) {
                return activityType;
            }
        }
        throw new IllegalArgumentException("Unknown enum value : " + value);
    }
}
