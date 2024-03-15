package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.service.domain.ApplicationEnum;

import java.util.List;

/**
 * Review status for a particular customer provided document.
 */
public enum ReviewStatus implements ApplicationEnum {
    NEW("NEW", "New"),
    ACCEPTED("ACCEPTED", "Accepted"),
    REJECTED("REJECTED", "Rejected"),
    PENDING("PENDING", "Pending");

    private static final List<String> hiddenValues = List.of("PENDING");

    private final String value;
    private final String label;

    ReviewStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean isHiddenFromApi() {
        return hiddenValues.contains(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
   * Constructs a new ReviewStatus with the given string value.
   *
   * @param value the string value to be converted to an enum value
   *
   * @return an element from the enum
   *
   * @throws IllegalArgumentException if value is not a valid enum value.
   */
    @JsonCreator
    public static ReviewStatus fromValue(String value) {
        for (ReviewStatus b : ReviewStatus.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
