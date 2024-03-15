package io.nuvalence.workmanager.service.domain.transaction;

import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.service.domain.ApplicationEnum;

/**
 * User types.
 */
public enum UserType implements ApplicationEnum {
    AGENCY("agency", "Agency"),
    PUBLIC("public", "Public");

    private final String value;
    private final String label;

    UserType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    public String getLabel() {
        return this.label;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
