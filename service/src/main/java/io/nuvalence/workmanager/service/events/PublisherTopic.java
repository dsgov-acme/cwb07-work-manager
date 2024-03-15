package io.nuvalence.workmanager.service.events;

import lombok.Getter;

/**
 * Enumerates the topics that can be published to.
 */
@Getter
public enum PublisherTopic {
    DOCUMENT_PROCESSING_INITIATION,
    APPLICATION_ROLE_REPORTING,
    NOTIFICATION_REQUESTS,
    AUDIT_EVENTS_RECORDING
}
