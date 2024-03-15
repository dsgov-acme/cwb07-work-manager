package io.nuvalence.workmanager.service.domain.dynamicschema;

import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import lombok.Data;

/**
 * Additional configuration for document classification.
 */
@Data
public class DocumentClassifierConfiguration implements AttributeConfiguration {

    private String classifierName;

    @Override
    public boolean canApplyTo(Class<?> type) {
        return Document.class.isAssignableFrom(type);
    }
}
