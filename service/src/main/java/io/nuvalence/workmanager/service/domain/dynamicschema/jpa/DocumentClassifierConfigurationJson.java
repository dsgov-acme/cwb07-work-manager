package io.nuvalence.workmanager.service.domain.dynamicschema.jpa;

import lombok.Data;

/**
 * Persistence model for DocumentClassifierConfiguration.
 */
@Data
public class DocumentClassifierConfigurationJson implements AttributeConfigurationJson {
    private String classifierName;
}
