package io.nuvalence.workmanager.service.domain.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.service.domain.ApplicationEnumRanked;

/**
 * Transaction Priority for a particular transction.
 */
public enum TransactionPriority implements ApplicationEnumRanked {
    LOW(10, "LOW", "Low"),
    MEDIUM(20, "MEDIUM", "Medium"),
    HIGH(30, "HIGH", "High"),
    URGENT(40, "URGENT", "Urgent");

    private final int rank;
    private final String value;
    private final String label;

    TransactionPriority(int rank, String value, String label) {
        this.rank = rank;
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public int getRank() {
        return rank;
    }

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
   * Constructs a new TransactionPriority with the given int value.
     *
   * @param rank the integer rank to be converted to an enum value
   *
   * @return an element from the enum
   *
   * @throws IllegalArgumentException if the given value does not match one of the enum values
   */
    @JsonCreator
    public static TransactionPriority fromRank(int rank) {
        for (TransactionPriority priority : TransactionPriority.values()) {
            if (priority.rank == rank) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unexpected rank '" + rank + "'");
    }

    /**
     * Constructs a new TransactionPriority with the given string value.
     *
     * @param value the string value to be converted to an enum value
     *
     * @return an element from the enum
     *
     * @throws IllegalArgumentException if the given value does not match one of the enum values
     */
    @JsonCreator
    public static TransactionPriority fromStringValue(String value) {
        for (TransactionPriority priority : TransactionPriority.values()) {
            if (priority.value.equalsIgnoreCase(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
