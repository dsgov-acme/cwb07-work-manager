package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.InitiateDocumentProcessingEvent;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class DocumentManagementServiceTest {

    @Mock private EventGateway eventGateway;

    @Mock private PublisherProperties publisherProperties;

    private DocumentManagementService documentManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentManagementService =
                new DocumentManagementService(eventGateway, publisherProperties);
    }

    @Test
    void initiateDocumentProcessing_Success() {
        UUID documentId = UUID.randomUUID();
        List<String> processorsNames = Collections.singletonList("Processor1");

        when(publisherProperties.getFullyQualifiedTopicName(any()))
                .thenReturn(Optional.of("topicName"));

        documentManagementService.initiateDocumentProcessing(documentId, processorsNames);

        verify(eventGateway)
                .publishEvent(any(InitiateDocumentProcessingEvent.class), eq("topicName"));
    }

    @Test
    void initiateDocumentProcessing_TopicNotConfigured_ThrowsUnexpectedException() {
        UUID documentId = UUID.randomUUID();
        List<String> processorsNames = Collections.singletonList("Processor1");

        when(publisherProperties.getFullyQualifiedTopicName(any())).thenReturn(Optional.empty());

        UnexpectedException exception =
                assertThrows(
                        UnexpectedException.class,
                        () ->
                                documentManagementService.initiateDocumentProcessing(
                                        documentId, processorsNames));

        String expectedMessage =
                "Document processing initiation topic not configured, topic name:"
                        + " DOCUMENT_PROCESSING_INITIATION";
        String actualMessage = exception.getMessage();

        assertThrows(
                UnexpectedException.class,
                () -> {
                    throw new UnexpectedException(expectedMessage);
                });

        assertThrows(
                UnexpectedException.class,
                () -> {
                    throw new UnexpectedException(actualMessage);
                });

        assertThrows(
                UnexpectedException.class,
                () -> {
                    throw new UnexpectedException(expectedMessage + "extra stuff");
                });
    }
}
