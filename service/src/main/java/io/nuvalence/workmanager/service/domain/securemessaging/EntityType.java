package io.nuvalence.workmanager.service.domain.securemessaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.service.domain.ApplicationEnum;
import lombok.Getter;

public enum EntityType implements ApplicationEnum {
    TRANSACTION("TRANSACTION", "Transaction"),
    EMPLOYER("EMPLOYER", "Employer");

    private final String value;
    @Getter private final String label;

    EntityType(String value, String label) {
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

    /**
     * Constructs a new BusinessType with the given string value.
     * @param value the string value to be converted to an enum value
     * @return an element from the enum
     *
     * @throws IllegalArgumentException if value is not a valid enum value.
     */
    @JsonCreator
    public static EntityType fromValue(String value) {
        for (EntityType p : EntityType.values()) {
            if (p.value.equals(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
