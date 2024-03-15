package io.nuvalence.workmanager.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.ComputedDynaProperty;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.SupportedTypes;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.AttributeConfigurationJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaAttributeJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaComputedAttributeJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import io.nuvalence.workmanager.service.generated.models.AttributeConfigurationModel;
import io.nuvalence.workmanager.service.generated.models.AttributeDefinitionModel;
import io.nuvalence.workmanager.service.generated.models.ComputedAttributeDefinitionModel;
import io.nuvalence.workmanager.service.generated.models.SchemaAttributeModel;
import io.nuvalence.workmanager.service.generated.models.SchemaComputedAttributeModel;
import io.nuvalence.workmanager.service.generated.models.SchemaCreateModel;
import io.nuvalence.workmanager.service.generated.models.SchemaExportModel;
import io.nuvalence.workmanager.service.generated.models.SchemaModel;
import io.nuvalence.workmanager.service.generated.models.SchemaUpdateModel;
import lombok.Setter;
import org.apache.commons.beanutils.DynaProperty;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

/**
 * Maps dynamic schemas between 3 forms.
 *
 * <ul>
 *     <li>API Model ({@link io.nuvalence.workmanager.service.generated.models.SchemaModel})</li>
 *     <li>Logic Object ({@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema})</li>
 *     <li>Persistence Model ({@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow})</li>
 * </ul>
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@SuppressWarnings({"ClassFanOutComplexity", "ClassDataAbstractionCoupling"})
public abstract class DynamicSchemaMapper {

    @Autowired @Setter private ObjectMapper objectMapper;

    @Autowired @Setter private AttributeConfigurationMapper attributeConfigurationMapper;

    public SchemaModel schemaToSchemaModel(final Schema schema) {
        return schemaToSchemaModel(schema, null);
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema} to
     * {@link io.nuvalence.workmanager.service.generated.models.SchemaModel}.
     *
     * @param schema Logic model for schema
     * @param relatedSchemas map of related schemas
     *
     * @return API model for schema
     */
    public SchemaModel schemaToSchemaModel(
            final Schema schema, final Map<String, List<Schema>> relatedSchemas) {

        final SchemaModel model = new SchemaModel();
        model.id(schema.getId());
        model.key(schema.getKey());
        model.setName(schema.getName());
        model.description(schema.getDescription());
        model.createdBy(schema.getCreatedBy());
        model.lastUpdatedBy(schema.getLastUpdatedBy());
        model.createdTimestamp(schema.getCreatedTimestamp());
        model.lastUpdatedTimestamp(schema.getLastUpdatedTimestamp());

        for (DynaProperty property : schema.getDynaProperties()) {
            if (property instanceof ComputedDynaProperty) {
                addComputedAttributeToSchemaModel(model.getComputedAttributes(), property);
            } else if (mustAddSchemaChildren(relatedSchemas, property)) {
                addAttributeAndChildren(schema, relatedSchemas, model.getAttributes(), property);
            } else {
                addAttributeToSchemaModel(property, schema, model.getAttributes());
            }
        }

        return model;
    }

    private boolean mustAddSchemaChildren(
            Map<String, List<Schema>> relatedSchemas, DynaProperty property) {
        return (property.getType().isAssignableFrom(DynamicEntity.class)
                        || (property.getType().isAssignableFrom(List.class)
                                && property.getContentType().equals(DynamicEntity.class)))
                && relatedSchemas != null;
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema} to
     * {@link io.nuvalence.workmanager.service.generated.models.SchemaModel}.
     *
     * @param schema Logic model for schema
     * @return API model for schema
     */
    public SchemaUpdateModel schemaToSchemaUpdateModel(final Schema schema) {
        final SchemaUpdateModel schemaRequest = new SchemaUpdateModel();
        schemaRequest.setName(schema.getName());
        schemaRequest.description(schema.getDescription());
        schemaRequest.setComputedAttributes(new ArrayList<>());

        for (DynaProperty property : schema.getDynaProperties()) {
            if (property instanceof ComputedDynaProperty) {
                addComputedAttributeToSchemaModel(schemaRequest.getComputedAttributes(), property);
            } else {
                addAttributeToSchemaModel(property, schema, schemaRequest.getAttributes());
            }
        }

        return schemaRequest;
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema} to
     * {@link io.nuvalence.workmanager.service.generated.models.SchemaCreateModel}.
     *
     * @param schema Logic model for schema
     * @return API model for schema
     */
    public SchemaCreateModel schemaToSchemaCreateModel(final Schema schema) {
        final SchemaCreateModel schemaRequest = new SchemaCreateModel();
        schemaRequest.setKey(schema.getKey());
        schemaRequest.setName(schema.getName());
        schemaRequest.description(schema.getDescription());
        schemaRequest.setComputedAttributes(new ArrayList<>());

        for (DynaProperty property : schema.getDynaProperties()) {
            if (property instanceof ComputedDynaProperty) {
                addComputedAttributeToSchemaModel(schemaRequest.getComputedAttributes(), property);
            } else {
                addAttributeToSchemaModel(property, schema, schemaRequest.getAttributes());
            }
        }

        return schemaRequest;
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.generated.models.SchemaModel} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema}.
     *
     * @param model API model for schema
     * @return Logic model for schema
     */
    public Schema schemaModelToSchema(final SchemaModel model) {
        final Schema.SchemaBuilder builder = Schema.builder();
        builder.id(model.getId());
        builder.key(model.getKey());
        builder.name(model.getName());
        builder.description(model.getDescription());

        addAttributesToBuilder(model.getAttributes(), builder);
        addComputedAttributesToBuilder(model.getComputedAttributes(), builder);

        return builder.build();
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.generated.models.SchemaUpdateModel} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema}.
     *
     * @param model     API request model for schema
     * @param key       schema idefinition key
     * @param schemaId  schema idefinition key
     * @return Logic model for schema
     */
    public Schema schemaUpdateModelToSchema(SchemaUpdateModel model, String key, UUID schemaId) {
        final Schema.SchemaBuilder builder = Schema.builder();

        builder.id(schemaId);
        builder.key(key);
        builder.name(model.getName());
        builder.description(model.getDescription());

        addAttributesToBuilder(model.getAttributes(), builder);
        if (model.getComputedAttributes() != null) {
            addComputedAttributesToBuilder(model.getComputedAttributes(), builder);
        }

        return builder.build();
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.generated.models.SchemaCreateModel} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema}.
     *
     * @param model     API request model for schema creation
     * @return Logic model for schema
     */
    public Schema schemaCreateModelToSchema(SchemaCreateModel model) {
        final Schema.SchemaBuilder builder = Schema.builder();

        builder.key(model.getKey());
        builder.name(model.getName());
        builder.description(model.getDescription());

        addAttributesToBuilder(model.getAttributes(), builder);
        if (model.getComputedAttributes() != null) {
            addComputedAttributesToBuilder(model.getComputedAttributes(), builder);
        }

        return builder.build();
    }

    private void addComputedAttributeToSchemaModel(
            List<ComputedAttributeDefinitionModel> model, DynaProperty property) {
        final ComputedAttributeDefinitionModel attribute = new ComputedAttributeDefinitionModel();
        attribute.setName(property.getName());
        attribute.setType(typeToString(property.getType()));
        attribute.setExpression(((ComputedDynaProperty) property).getExpression());
        model.add(attribute);
    }

    private AttributeDefinitionModel createAttributeDefinitionModel(
            DynaProperty property, Schema schema) {
        final AttributeDefinitionModel attribute = new AttributeDefinitionModel();
        attribute.setName(property.getName());
        attribute.setType(typeToString(property.getType()));
        if (property.getContentType() != null) {
            attribute.setContentType(typeToString(property.getContentType()));
        }

        if (DynamicEntity.class.equals(property.getType())
                || DynamicEntity.class.equals(property.getContentType())) {
            attribute.setEntitySchema(schema.getRelatedSchemas().get(property.getName()));
        }

        attribute.setAttributeConfigurations(
                schema
                        .getAttributeConfigurations(
                                property.getName(), AttributeConfiguration.class)
                        .stream()
                        .map(this.attributeConfigurationMapper::attributeToAttributeModel)
                        .toList());

        return attribute;
    }

    private void addAttributeAndChildren(
            Schema parentSchema,
            Map<String, List<Schema>> relatedSchemas,
            List<AttributeDefinitionModel> model,
            DynaProperty property) {

        AttributeDefinitionModel attribute = createAttributeDefinitionModel(property, parentSchema);

        List<Schema> childrenSchemas = relatedSchemas.get(parentSchema.getKey());

        Schema foundSchema =
                childrenSchemas.stream()
                        .filter(
                                schema ->
                                        schema.getKey()
                                                .equals(
                                                        parentSchema
                                                                .getRelatedSchemas()
                                                                .get(property.getName())))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Schema not found: "
                                                        + parentSchema
                                                                .getRelatedSchemas()
                                                                .get(property.getName())));

        attribute.setSchema(schemaToSchemaModel(foundSchema, relatedSchemas));

        model.add(attribute);
    }

    private void addAttributeToSchemaModel(
            DynaProperty property, Schema schema, List<AttributeDefinitionModel> model) {
        AttributeDefinitionModel attribute = createAttributeDefinitionModel(property, schema);
        model.add(attribute);
    }

    private void addAttributesToBuilder(
            List<AttributeDefinitionModel> attributes, final Schema.SchemaBuilder builder) {
        final Map<String, String> relatedSchemas = new HashMap<>();

        for (AttributeDefinitionModel attribute : attributes) {
            if (attribute.getContentType() == null) {
                builder.property(attribute.getName(), stringToType(attribute.getType()));
            } else {
                builder.property(
                        attribute.getName(),
                        stringToType(attribute.getType()),
                        stringToType(attribute.getContentType()));
            }

            if (attribute.getEntitySchema() != null) {
                relatedSchemas.put(attribute.getName(), attribute.getEntitySchema());
            }

            for (AttributeConfigurationModel attributeConfigurationModel :
                    attribute.getAttributeConfigurations()) {
                builder.attributeConfiguration(
                        attribute.getName(),
                        this.attributeConfigurationMapper.attributeModelToAttribute(
                                attributeConfigurationModel));
            }
        }

        builder.relatedSchemas(relatedSchemas);
    }

    private void addComputedAttributesToBuilder(
            List<ComputedAttributeDefinitionModel> computedAttributes,
            Schema.SchemaBuilder builder) {
        for (ComputedAttributeDefinitionModel attribute : computedAttributes) {
            builder.computedProperty(
                    attribute.getName(),
                    stringToType(attribute.getType()),
                    attribute.getExpression());
        }
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema}.
     *
     * @param row Persistence model for schema
     * @return Logic model for schema
     * @throws JsonProcessingException if schema JSON cannot be parsed
     */
    public Schema schemaRowToSchema(final SchemaRow row) throws JsonProcessingException {
        return schemaJsonToSchemaBuilder(
                        objectMapper.readValue(row.getSchemaJson(), SchemaJson.class), row.getId())
                .createdBy(row.getCreatedBy())
                .createdTimestamp(row.getCreatedTimestamp())
                .lastUpdatedBy(row.getLastUpdatedBy())
                .lastUpdatedTimestamp(row.getLastUpdatedTimestamp())
                .build();
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow}.
     *
     * @param schema Logic model for schema
     * @return Persistence model for schema
     * @throws JsonProcessingException if schema cannot be serialized to JSON
     */
    public SchemaRow schemaToSchemaRow(final Schema schema) throws JsonProcessingException {
        SchemaRow.SchemaRowBuilder builder =
                SchemaRow.builder()
                        .id(schema.getId())
                        .name(schema.getName())
                        .key(schema.getKey())
                        .description(schema.getDescription())
                        .schemaJson(objectMapper.writeValueAsString(schemaToSchemaJson(schema)));

        return builder.build();
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson}.
     *
     * @param schema Logic model for schema
     * @return JSON model used in persistence model
     */
    public SchemaJson schemaToSchemaJson(final Schema schema) {
        final SchemaJson json = new SchemaJson();

        if (schema.getId() != null) {
            json.setId(schema.getId().toString());
        }
        json.setKey(schema.getKey());
        json.setName(schema.getName());
        json.setDescription(schema.getDescription());

        for (DynaProperty property : schema.getDynaProperties()) {
            if (property instanceof ComputedDynaProperty computedDynaProperty) {
                final SchemaComputedAttributeJson computedAttribute =
                        new SchemaComputedAttributeJson();
                computedAttribute.setName(property.getName());
                computedAttribute.setType(typeToString(property.getType()));
                computedAttribute.setExpression(computedDynaProperty.getExpression());
                json.getComputedAttributes().add(computedAttribute);
            } else {
                final SchemaAttributeJson attribute = new SchemaAttributeJson();
                attribute.setName(property.getName());
                attribute.setType(typeToString(property.getType()));
                if (property.getContentType() != null) {
                    attribute.setContentType(typeToString(property.getContentType()));
                }

                if (DynamicEntity.class.equals(property.getType())
                        || DynamicEntity.class.equals(property.getContentType())) {
                    attribute.setEntitySchema(schema.getRelatedSchemas().get(property.getName()));
                }

                attribute.setAttributeConfigurations(
                        schema
                                .getAttributeConfigurations(
                                        property.getName(), AttributeConfiguration.class)
                                .stream()
                                .map(this.attributeConfigurationMapper::attributeToAttributeJson)
                                .toList());

                json.getAttributes().add(attribute);
            }
        }

        return json;
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.Schema}.
     *
     * @param json JSON model from persistence model
     * @param id ID if schema
     * @return Logic model for schema
     */
    public Schema schemaJsonToSchema(final SchemaJson json, UUID id) {
        var builder = schemaJsonToSchemaBuilder(json, id);

        return builder.build();
    }

    private Schema.SchemaBuilder schemaJsonToSchemaBuilder(SchemaJson json, UUID id) {
        final Map<String, String> relatedSchemas = new HashMap<>();
        final Schema.SchemaBuilder builder = Schema.builder();

        builder.id(id);
        builder.key(json.getKey());
        builder.name(json.getName());
        builder.description(json.getDescription());

        for (SchemaAttributeJson attribute : json.getAttributes()) {
            if (attribute.getContentType() == null) {
                builder.property(attribute.getName(), stringToType(attribute.getType()));
            } else {
                builder.property(
                        attribute.getName(),
                        stringToType(attribute.getType()),
                        stringToType(attribute.getContentType()));
            }

            if (attribute.getEntitySchema() != null) {
                relatedSchemas.put(attribute.getName(), attribute.getEntitySchema());
            }

            for (AttributeConfigurationJson attributeConfigurationJson :
                    attribute.getAttributeConfigurations()) {
                builder.attributeConfiguration(
                        attribute.getName(),
                        this.attributeConfigurationMapper.attributeJsonToAttribute(
                                attributeConfigurationJson));
            }
        }
        builder.relatedSchemas(relatedSchemas);
        for (SchemaComputedAttributeJson computedAttribute : json.getComputedAttributes()) {
            builder.computedProperty(
                    computedAttribute.getName(),
                    stringToType(computedAttribute.getType()),
                    computedAttribute.getExpression());
        }
        return builder;
    }

    /**
     * Converts a SchemaRow to a SchemaExportModel.
     *
     * @param value the SchemaRow.
     * @return the SchemaExportModel.
     * @throws JsonProcessingException if an error occurs during JSON processing.
     */
    public SchemaExportModel schemaRowToSchemaExportModel(SchemaRow value)
            throws JsonProcessingException {
        SchemaExportModel model = new SchemaExportModel();
        SchemaJson schemaJson = objectMapper.readValue(value.getSchemaJson(), SchemaJson.class);
        model.setName(value.getName());
        model.setKey(value.getKey());
        model.setAttributes(
                schemaJson.getAttributes().stream()
                        .map(this::schemaAttributeJsonToSchemaAttributeModel)
                        .toList());
        model.computedAttributes(
                schemaJson.getComputedAttributes().stream()
                        .map(this::schemaComputedAttributeJsonToSchemaComputedAttributeModel)
                        .toList());
        return model;
    }

    private SchemaComputedAttributeModel schemaComputedAttributeJsonToSchemaComputedAttributeModel(
            SchemaComputedAttributeJson schemaComputedAttributeJson) {
        SchemaComputedAttributeModel schemaComputedAttributeModel =
                new SchemaComputedAttributeModel();
        schemaComputedAttributeModel.setName(schemaComputedAttributeJson.getName());
        schemaComputedAttributeModel.setType(schemaComputedAttributeJson.getType());
        schemaComputedAttributeModel.setExpression(schemaComputedAttributeJson.getExpression());
        return schemaComputedAttributeModel;
    }

    private SchemaAttributeModel schemaAttributeJsonToSchemaAttributeModel(
            SchemaAttributeJson value) {
        SchemaAttributeModel schemaAttributeModel = new SchemaAttributeModel();
        schemaAttributeModel.setName(value.getName());
        schemaAttributeModel.setType(value.getType());
        schemaAttributeModel.setContentType(value.getContentType());
        schemaAttributeModel.setEntitySchema(value.getEntitySchema());

        List<AttributeConfiguration> attributeConfigurations;
        List<AttributeConfigurationModel> attributeConfigurationModels;

        attributeConfigurations =
                value.getAttributeConfigurations().stream()
                        .map(attributeConfigurationMapper::attributeJsonToAttribute)
                        .toList();

        attributeConfigurationModels =
                attributeConfigurations.stream()
                        .map(attributeConfigurationMapper::attributeToAttributeModel)
                        .toList();

        schemaAttributeModel.setAttributeConfigurations(attributeConfigurationModels);

        return schemaAttributeModel;
    }

    /**
     * Converts a SchemaExportModel to a SchemaRow.
     *
     * @param model the SchemaExportModel.
     * @return the SchemaRow.
     * @throws JsonProcessingException if an error occurs during JSON processing.
     */
    public SchemaRow schemaExportModelToSchemaRow(SchemaExportModel model)
            throws JsonProcessingException {
        String test = objectMapper.writeValueAsString(model);
        SchemaRow schemarow = new SchemaRow();
        schemarow.setSchemaJson(test);
        schemarow.setName(model.getName());
        schemarow.setKey(model.getKey());
        return schemarow;
    }

    private String typeToString(final Class<?> type) {
        return type.getSimpleName();
    }

    private Class<?> stringToType(final String typeName) {
        return Objects.requireNonNull(SupportedTypes.getBySimpleName(typeName)).getTypeClass();
    }
}
