package io.nuvalence.workmanager.service.domain;

/**
 * Represents an enumeration that can be served to clients as application data or configuration.
 */
public interface ApplicationEnumRanked extends ApplicationEnum {

    int getRank();
}
