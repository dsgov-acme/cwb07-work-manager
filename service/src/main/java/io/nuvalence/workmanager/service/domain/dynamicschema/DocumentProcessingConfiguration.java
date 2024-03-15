package io.nuvalence.workmanager.service.domain.dynamicschema;

import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import lombok.Data;

/**
 * Additional configuration for document processing.
 */
@Data
public class DocumentProcessingConfiguration implements AttributeConfiguration {

    private String processorId;

    @Override
    public boolean canApplyTo(Class<?> type) {
        return Document.class.isAssignableFrom(type);
    }
}
