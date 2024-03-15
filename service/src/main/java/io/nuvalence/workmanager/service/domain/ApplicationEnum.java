package io.nuvalence.workmanager.service.domain;

/**
 * Represents an enumeration that can be served to clients as application data or configuration.
 */
public interface ApplicationEnum {

    String getValue();

    String getLabel();

    default boolean isHiddenFromApi() {
        return false;
    }

    default boolean isHiddenForPublicUsers() {
        return false;
    }
}
