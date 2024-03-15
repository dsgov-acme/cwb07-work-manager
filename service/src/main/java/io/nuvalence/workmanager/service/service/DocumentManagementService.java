package io.nuvalence.workmanager.service.service;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.InitiateDocumentProcessingEvent;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.events.EventFactory;
import io.nuvalence.workmanager.service.events.PublisherTopic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the comunication with document management service.
 */
@Component
@RequiredArgsConstructor
public class DocumentManagementService {
    private final EventGateway eventGateway;
    private final PublisherProperties publisherProperties;

    /**
     * Initiates document processing.
     *
     * @param documentId       document id
     * @param processorsNames  list of processors names
     */
    public void initiateDocumentProcessing(UUID documentId, List<String> processorsNames) {

        InitiateDocumentProcessingEvent event =
                EventFactory.createInitiateDocumentProcessingEvent(documentId, processorsNames);

        Optional<String> fullyQualifiedTopicNameOptional =
                publisherProperties.getFullyQualifiedTopicName(
                        PublisherTopic.DOCUMENT_PROCESSING_INITIATION.name());

        if (!fullyQualifiedTopicNameOptional.isPresent()) {
            throw new UnexpectedException(
                    "Document processing initiation topic not configured, topic name: "
                            + PublisherTopic.DOCUMENT_PROCESSING_INITIATION.name());
        }

        eventGateway.publishEvent(event, fullyQualifiedTopicNameOptional.get());
    }
}
