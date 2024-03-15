package io.nuvalence.workmanager.service.documentmanagementapi.models;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Request class to call the document management endpoint to initiate a document processing.
 */
@Getter
@Builder
@ToString
@Jacksonized
public class DocumentProcessingRequest {
    private String processorId;
}
