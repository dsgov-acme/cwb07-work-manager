package io.nuvalence.workmanager.service.domain.dynamicschema.jpa;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Persistence model for attribute configuration.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
            value = DocumentProcessingConfigurationJson.class,
            name = "DocumentProcessor"),
    @JsonSubTypes.Type(
            value = DocumentClassifierConfigurationJson.class,
            name = "DocumentClassifier")
})
public interface AttributeConfigurationJson {}
