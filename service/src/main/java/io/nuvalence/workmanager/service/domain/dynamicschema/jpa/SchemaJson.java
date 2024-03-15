package io.nuvalence.workmanager.service.domain.dynamicschema.jpa;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * Serializable persistence model for schemas.
 */
@Data
public class SchemaJson {
    private String id;
    private String key;
    private String name;
    private String description;
    private List<SchemaAttributeJson> attributes = new LinkedList<>();
    private List<SchemaComputedAttributeJson> computedAttributes = new LinkedList<>();
}
