package io.nuvalence.workmanager.service.models.auditevents;

import lombok.Getter;

/**
 * Enum for business object type for audit events.
 */
public enum AuditEventBusinessObject {
    TRANSACTION("transaction"),
    EMPLOYER("employer"),
    INDIVIDUAL("individual");

    @Getter private String value;

    AuditEventBusinessObject(String value) {
        this.value = value;
    }
}
