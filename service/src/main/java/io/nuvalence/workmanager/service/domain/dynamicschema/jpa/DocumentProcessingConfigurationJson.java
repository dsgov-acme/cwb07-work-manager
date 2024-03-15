package io.nuvalence.workmanager.service.domain.dynamicschema.jpa;

import lombok.Data;

/**
 * Persistence model for DocumentProcessingConfiguration.
 */
@Data
public class DocumentProcessingConfigurationJson implements AttributeConfigurationJson {

    private String processorId;
}
