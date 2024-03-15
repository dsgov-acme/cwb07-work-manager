package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the status of a given document.
 */
public enum DocumentStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected");

    @JsonValue private final String text;

    DocumentStatus(String text) {
        this.text = text;
    }

    /**
     * Validation for Document Status enum.
     *
     * @param text incoming string from unmarshalled json.
     * @return a valid enum.
     */
    public static DocumentStatus fromText(String text) {
        for (DocumentStatus status : DocumentStatus.values()) {
            if (status.toString().equalsIgnoreCase(text)) {
                return status;
            }
        }
        return PENDING;
    }

    @Override
    public String toString() {
        return text;
    }
}
