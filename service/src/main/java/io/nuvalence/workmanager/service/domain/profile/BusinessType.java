package io.nuvalence.workmanager.service.domain.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.service.domain.ApplicationEnum;

public enum BusinessType implements ApplicationEnum {
    CORPORATION("CORPORATION", "Corporation"),
    SOLE_PROPRIETORSHIP("SOLE_PROPRIETORSHIP", "Sole Proprietorship"),
    PARTNERSHIP("PARTNERSHIP", "Partnership"),
    LLC("LLC", "LLC"),
    LLP("LLP", "LLP");

    private final String value;
    private final String label;

    BusinessType(String value, String label) {
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
     * Constructs a new BusinessType with the given string value.
     * @param value the string value to be converted to an enum value
     * @return an element from the enum
     *
     * @throws IllegalArgumentException if value is not a valid enum value.
     */
    @JsonCreator
    public static BusinessType fromValue(String value) {
        for (BusinessType b : BusinessType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
