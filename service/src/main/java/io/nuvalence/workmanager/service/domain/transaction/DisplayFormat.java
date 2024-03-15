package io.nuvalence.workmanager.service.domain.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Display format for a field.
 */
public enum DisplayFormat {
    DATE(1, "Date"),
    DATETIME(2, "Date Time"),
    PHONE(3, "Phone"),
    USERDATA(4, "User Data"),
    PRIORITY(4, "Priority");

    private final int value;

    private final String label;

    DisplayFormat(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    /**
     * Constructs a new DisplayFormat with the given int value.
     *
     * @param value the string value to be converted to an enum value
     *
     * @return an element from the enum
     *
     * @throws IllegalArgumentException if value is not a valid enum value.
     */
    @JsonCreator
    public static DisplayFormat fromValue(int value) {
        for (DisplayFormat b : DisplayFormat.values()) {
            if (b.value == value) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
