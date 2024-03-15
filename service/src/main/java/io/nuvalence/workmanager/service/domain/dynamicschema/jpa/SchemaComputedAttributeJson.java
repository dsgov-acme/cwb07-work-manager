package io.nuvalence.workmanager.service.domain.dynamicschema.jpa;

import lombok.Data;

/**
 * Serializable persistence model for computed attribute definitions.
 */
@Data
public class SchemaComputedAttributeJson {
    private String name;
    private String type;
    private String expression;
}
