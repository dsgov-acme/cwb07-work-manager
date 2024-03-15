package io.nuvalence.workmanager.service.domain.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.service.domain.ApplicationEnum;

import java.util.List;

public enum ProfileAccessLevel implements ApplicationEnum {
    ADMIN("ADMIN", "Admin"),
    WRITER("WRITER", "Writer"),
    READER("READER", "Reader"),
    AGENCY_READONLY("AGENCY_READONLY", "Agency Readonly");

    private static final List<String> hiddenValuesForPublicUser = List.of("AGENCY_READONLY");

    private final String value;
    private final String label;

    ProfileAccessLevel(String value, String label) {
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

    @Override
    public boolean isHiddenForPublicUsers() {
        return hiddenValuesForPublicUser.contains(value);
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
    public static ProfileAccessLevel fromValue(String value) {
        for (ProfileAccessLevel p : ProfileAccessLevel.values()) {
            if (p.value.equals(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
