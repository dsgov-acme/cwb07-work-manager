package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentProcessingConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import io.nuvalence.workmanager.service.generated.models.AttributeDefinitionModel;
import io.nuvalence.workmanager.service.generated.models.ComputedAttributeDefinitionModel;
import io.nuvalence.workmanager.service.generated.models.DocumentProcessorConfigurationModel;
import io.nuvalence.workmanager.service.generated.models.SchemaAttributeModel;
import io.nuvalence.workmanager.service.generated.models.SchemaComputedAttributeModel;
import io.nuvalence.workmanager.service.generated.models.SchemaCreateModel;
import io.nuvalence.workmanager.service.generated.models.SchemaExportModel;
import io.nuvalence.workmanager.service.generated.models.SchemaModel;
import io.nuvalence.workmanager.service.generated.models.SchemaUpdateModel;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class DynamicSchemaMapperTest {
    private Schema schema;
    private SchemaModel schemaModel;
    private SchemaUpdateModel modificationRequest;
    private SchemaCreateModel creationRequest;
    private SchemaExportModel schemaExportModel;
    private SchemaRow schemaRow;

    private SchemaRow schemaRow2;
    private DynamicSchemaMapper mapper;
    private final UUID commonSchemaId = UUID.fromString("de8cbb88-a1f0-4b3f-8d59-778751805c73");

    @BeforeEach
    void setUp() throws JsonProcessingException {
        final ObjectMapper objectMapper = SpringConfig.getMapper();

        schema = getCommonSchemaBuilder(commonSchemaId).build();

        schemaModel =
                new SchemaModel()
                        .id(commonSchemaId)
                        .key("TestSchemaKey")
                        .name("root/TestSchema")
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("foo")
                                        .type("String")
                                        .addAttributeConfigurationsItem(
                                                new DocumentProcessorConfigurationModel()
                                                        .processorId("testProcessorId")))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("bars")
                                        .type("List")
                                        .contentType("String"))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("child")
                                        .type("DynamicEntity")
                                        .entitySchema("common/Child"))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("children")
                                        .type("List")
                                        .contentType("DynamicEntity")
                                        .entitySchema("common/Child"))
                        .addComputedAttributesItem(
                                new ComputedAttributeDefinitionModel()
                                        .name("foobars")
                                        .type("String")
                                        .expression("#concat(\", \", foo, bars)"));
        modificationRequest =
                new SchemaUpdateModel()
                        .name("root/TestSchema")
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("foo")
                                        .type("String")
                                        .addAttributeConfigurationsItem(
                                                new DocumentProcessorConfigurationModel()
                                                        .processorId("testProcessorId")))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("bars")
                                        .type("List")
                                        .contentType("String"))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("child")
                                        .type("DynamicEntity")
                                        .entitySchema("common/Child"))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("children")
                                        .type("List")
                                        .contentType("DynamicEntity")
                                        .entitySchema("common/Child"))
                        .addComputedAttributesItem(
                                new ComputedAttributeDefinitionModel()
                                        .name("foobars")
                                        .type("String")
                                        .expression("#concat(\", \", foo, bars)"));

        creationRequest =
                new SchemaCreateModel()
                        .key("TestSchemaKey")
                        .name("root/TestSchema")
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("foo")
                                        .type("String")
                                        .addAttributeConfigurationsItem(
                                                new DocumentProcessorConfigurationModel()
                                                        .processorId("testProcessorId")))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("bars")
                                        .type("List")
                                        .contentType("String"))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("child")
                                        .type("DynamicEntity")
                                        .entitySchema("common/Child"))
                        .addAttributesItem(
                                new AttributeDefinitionModel()
                                        .name("children")
                                        .type("List")
                                        .contentType("DynamicEntity")
                                        .entitySchema("common/Child"))
                        .addComputedAttributesItem(
                                new ComputedAttributeDefinitionModel()
                                        .name("foobars")
                                        .type("String")
                                        .expression("#concat(\", \", foo, bars)"));

        SchemaAttributeModel schemaAttributeModel =
                new SchemaAttributeModel()
                        .name("foo")
                        .type("String")
                        .contentType("testContentType")
                        .entitySchema("testEntitySchema")
                        .addAttributeConfigurationsItem(
                                new DocumentProcessorConfigurationModel()
                                        .processorId("testProcessorId"));
        SchemaAttributeModel schemaAttributeModel2 =
                new SchemaAttributeModel()
                        .name("foo")
                        .type("String")
                        .contentType("testContentType2")
                        .entitySchema("testEntitySchema2")
                        .addAttributeConfigurationsItem(
                                new DocumentProcessorConfigurationModel()
                                        .processorId("testProcessorId2"));

        List<SchemaAttributeModel> schemaAttributeModels = new ArrayList<>();

        schemaAttributeModels.add(schemaAttributeModel);
        schemaAttributeModels.add(schemaAttributeModel2);

        SchemaComputedAttributeModel schemaComputedAttributeModel =
                new SchemaComputedAttributeModel()
                        .name("foobars")
                        .type("String")
                        .expression("#concat(\", \", foo, bars)");

        schemaExportModel =
                new SchemaExportModel()
                        .id(commonSchemaId.toString())
                        .name("root/TestSchema")
                        .key("TestSchemaKey")
                        .attributes(schemaAttributeModels)
                        .addComputedAttributesItem(schemaComputedAttributeModel);

        schemaRow =
                SchemaRow.builder()
                        .id(commonSchemaId)
                        .key("TestSchemaKey")
                        .name("root/TestSchema")
                        .schemaJson(objectMapper.writeValueAsString(schemaModel))
                        .createdBy("user1")
                        .lastUpdatedBy("user2")
                        .createdTimestamp(
                                Instant.ofEpochMilli(1629427100000L).atOffset(ZoneOffset.UTC))
                        .lastUpdatedTimestamp(
                                Instant.ofEpochMilli(1629427200001L).atOffset(ZoneOffset.UTC))
                        .build();

        schemaRow2 =
                SchemaRow.builder()
                        .id(UUID.randomUUID())
                        .key("TestSchemaKey")
                        .name("root/TestSchema")
                        .schemaJson(objectMapper.writeValueAsString(schemaExportModel))
                        .build();

        mapper = Mappers.getMapper(DynamicSchemaMapper.class);
        mapper.setObjectMapper(SpringConfig.getMapper());
        mapper.setAttributeConfigurationMapper(
                Mappers.getMapper(AttributeConfigurationMapper.class));
    }

    private Schema.SchemaBuilder getCommonSchemaBuilder(UUID schemaId) {
        final Schema childSchema = Schema.builder().name("common/Child").build();

        DocumentProcessingConfiguration documentProcessingConfiguration =
                new DocumentProcessingConfiguration();
        documentProcessingConfiguration.setProcessorId("testProcessorId");

        return Schema.builder()
                .id(schemaId)
                .key("TestSchemaKey")
                .name("root/TestSchema")
                .property("foo", String.class)
                .property("bars", List.class, String.class)
                .property("child", childSchema)
                .property("children", List.class, childSchema)
                .attributeConfiguration("foo", documentProcessingConfiguration)
                .computedProperty("foobars", String.class, "#concat(\", \", foo, bars)");
    }

    @Test
    void schemaToSchemaModel() {
        schemaModel
                .createdBy("user1")
                .lastUpdatedBy("user2")
                .createdTimestamp(Instant.ofEpochMilli(1629427100000L).atOffset(ZoneOffset.UTC))
                .lastUpdatedTimestamp(
                        Instant.ofEpochMilli(1629427200001L).atOffset(ZoneOffset.UTC));

        schema =
                getCommonSchemaBuilder(commonSchemaId)
                        .createdBy("user1")
                        .lastUpdatedBy("user2")
                        .createdTimestamp(
                                Instant.ofEpochMilli(1629427100000L).atOffset(ZoneOffset.UTC))
                        .lastUpdatedTimestamp(
                                Instant.ofEpochMilli(1629427200001L).atOffset(ZoneOffset.UTC))
                        .build();

        assertEquals(schemaModel, mapper.schemaToSchemaModel(schema));
    }

    @Test
    void schemaToSchemaModelTestWithChildren() {
        Schema rootSchema =
                Schema.builder()
                        .key("root")
                        .properties(
                                List.of(
                                        new DynaProperty("child", DynamicEntity.class),
                                        new DynaProperty(
                                                "child2", List.class, DynamicEntity.class)))
                        .relatedSchemas(Map.of("child", "Child", "child2", "Child"))
                        .build();

        Schema childSchema =
                Schema.builder()
                        .key("Child")
                        .properties(List.of(new DynaProperty("string", String.class)))
                        .relatedSchemas(Map.of())
                        .build();

        Map<String, List<Schema>> schemas = Map.of("root", List.of(childSchema));

        SchemaModel schemaModel = mapper.schemaToSchemaModel(rootSchema, schemas);

        assertEquals(2, schemaModel.getAttributes().size());
        assertEquals("child", schemaModel.getAttributes().get(0).getName());
        assertNotNull(schemaModel.getAttributes().get(0).getSchema());
        assertEquals("child2", schemaModel.getAttributes().get(1).getName());
        assertNotNull(schemaModel.getAttributes().get(1).getSchema());
    }

    @Test
    void schemaModelToSchema() {
        assertEquals(schema, mapper.schemaModelToSchema(schemaModel));
    }

    @Test
    void schemaRowToSchema() throws JsonProcessingException {

        schema =
                getCommonSchemaBuilder(commonSchemaId)
                        .createdBy("user1")
                        .lastUpdatedBy("user2")
                        .createdTimestamp(
                                Instant.ofEpochMilli(1629427100000L).atOffset(ZoneOffset.UTC))
                        .lastUpdatedTimestamp(
                                Instant.ofEpochMilli(1629427200001L).atOffset(ZoneOffset.UTC))
                        .build();

        assertEquals(schema, mapper.schemaRowToSchema(schemaRow));
    }

    @Test
    void schemaToSchemaRow() throws JsonProcessingException {
        final SchemaRow actual = mapper.schemaToSchemaRow(schema);
        assertEquals(schemaRow.getName(), actual.getName());
        var jsonMapper = new ObjectMapper();
        JsonNode schemaJson = jsonMapper.readTree(schemaRow.getSchemaJson());
        JsonNode actualJson = jsonMapper.readTree(actual.getSchemaJson());

        assertEquals(schemaJson, actualJson);
    }

    @Test
    void schemaRowToSchemaExportModel() throws JsonProcessingException {
        final SchemaExportModel actual = mapper.schemaRowToSchemaExportModel(schemaRow2);
        assertEquals(schemaExportModel.getName(), actual.getName());
        assertEquals(schemaExportModel.getKey(), actual.getKey());
        assertEquals(schemaExportModel.getAttributes(), actual.getAttributes());
        assertEquals(schemaExportModel.getComputedAttributes(), actual.getComputedAttributes());
    }

    @Test
    void schemaExportModelToSchemaRow() throws JsonProcessingException {
        final SchemaRow actual = mapper.schemaExportModelToSchemaRow(schemaExportModel);
        assertEquals(schemaRow2.getName(), actual.getName());
        assertEquals(schemaRow2.getSchemaJson(), actual.getSchemaJson());
    }

    @Test
    void schemaToSchemaUpdateModel() {
        assertEquals(modificationRequest, mapper.schemaToSchemaUpdateModel(schema));
    }

    @Test
    void schemaToSchemaCreateModel() {
        assertEquals(creationRequest, mapper.schemaToSchemaCreateModel(schema));
    }

    @Test
    void schemaUpdateModelToSchema() {
        assertEquals(
                schema,
                mapper.schemaUpdateModelToSchema(
                        modificationRequest, schema.getKey(), schema.getId()));
    }
}
