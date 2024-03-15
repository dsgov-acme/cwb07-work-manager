package io.nuvalence.workmanager.service.domain;

/**
 * Type of link between a transaction and a record.
 */
public enum TransactionRecordLinkType implements ApplicationEnum {
    CREATED("Created"),
    UPDATED("Updated");

    private final String label;

    TransactionRecordLinkType(String label) {
        this.label = label;
    }

    @Override
    public String getValue() {
        return this.name();
    }

    @Override
    public String getLabel() {
        return this.label;
    }
}
