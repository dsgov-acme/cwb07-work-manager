package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.service.domain.ApplicationEnum;

/**
 * Reason for a customer provided document to be rejected.
 */
public enum RejectionReasonType implements ApplicationEnum {
    POOR_QUALITY("POOR_QUALITY", "Poor Quality"),
    INCORRECT_TYPE("INCORRECT_TYPE", "Incorrect Type"),
    DOES_NOT_SATISFY_REQUIREMENTS("DOES_NOT_SATISFY_REQUIREMENTS", "Does Not Satisfy Requirements"),
    SUSPECTED_FRAUD("SUSPECTED_FRAUD", "Suspected Fraud");

    private final String value;
    private final String label;

    RejectionReasonType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public String getLabel() {
        return label;
    }

    /**
   * Constructs a new RejectionReason with the given string value.
   *
   * @param value the string value to be converted to an enum value
   *
   * @return an element from the enum
   *
   * @throws IllegalArgumentException if value is not a valid enum value.
   */
    @JsonCreator
    public static RejectionReasonType fromValue(String value) {
        for (RejectionReasonType b : RejectionReasonType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
