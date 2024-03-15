package io.nuvalence.workmanager.service.service;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.AuditEvent;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.events.EventFactory;
import io.nuvalence.workmanager.service.events.PublisherTopic;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;

/**
 * Service for managing transaction audit events.
 */
@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final EventGateway eventGateway;
    private final PublisherProperties publisherProperties;
    private final RequestContextTimestamp requestContextTimestamp;

    private String fullyQualifiedTopicName;

    @PostConstruct
    private void getFullyQualifiedTopicName() {
        if (this.fullyQualifiedTopicName == null) {
            Optional<String> fullyQualifiedTopicNameOptional =
                    publisherProperties.getFullyQualifiedTopicName(
                            PublisherTopic.AUDIT_EVENTS_RECORDING.name());

            if (!fullyQualifiedTopicNameOptional.isPresent()) {
                throw new UnexpectedException(
                        "Audit events topic not configured, topic name: "
                                + PublisherTopic.DOCUMENT_PROCESSING_INITIATION.name());
            }
            this.fullyQualifiedTopicName = fullyQualifiedTopicNameOptional.get();
        }
    }

    /**
     * Post state change events to audit service.
     * @param auditEvent object containing specifics of the audit evet.
     * @return Result audit event id.
     */
    public UUID sendAuditEvent(AuditEventRequestObjectDto auditEvent) {
        AuditEvent event =
                EventFactory.createAuditEvent(
                        auditEvent.getData(),
                        auditEvent.getOriginatorId(),
                        auditEvent.getUserId(),
                        auditEvent.getSummary(),
                        auditEvent.getBusinessObjectId(),
                        auditEvent.getBusinessObjectType());
        event.getMetadata().setTimestamp(requestContextTimestamp.getCurrentTimestamp());

        eventGateway.publishEvent(event, this.fullyQualifiedTopicName);

        return event.getMetadata().getId();
    }
}
