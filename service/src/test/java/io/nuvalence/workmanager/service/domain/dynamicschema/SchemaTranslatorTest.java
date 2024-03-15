package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.generated.models.AttributeDefinitionModel;
import io.nuvalence.workmanager.service.generated.models.SchemaModel;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class SchemaTranslatorTest {

    @Mock private DynamicSchemaMapper dynamicSchemaMapper;

    @Mock private ApplicationContext applicationContext;

    private SchemaTranslator schemaTranslator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        schemaTranslator = new SchemaTranslator();
        schemaTranslator.setApplicationContext(applicationContext);
    }

    @Test
    void testTranslate_WhenResourceIsSchema() {
        SchemaModel expectedModel = new SchemaModel();
        List<AttributeDefinitionModel> attributes = new ArrayList<>();
        AttributeDefinitionModel attribute = new AttributeDefinitionModel();
        attributes.add(attribute);
        expectedModel.setId(UUID.randomUUID());
        expectedModel.setKey("testKey");
        expectedModel.setDescription("testDescription");
        expectedModel.setName("testName");
        expectedModel.setAttributes(attributes);
        Schema schema = Schema.builder().name("schema").build();
        when(applicationContext.getBean(DynamicSchemaMapper.class)).thenReturn(dynamicSchemaMapper);
        when(dynamicSchemaMapper.schemaToSchemaModel(schema)).thenReturn(expectedModel);

        Object translatedResource = schemaTranslator.translate(schema);

        assertEquals(expectedModel, translatedResource);
    }

    @Test
    void testTranslate_WhenResourceIsNotSchema() {
        Object resource = new Object();

        Object translatedResource = schemaTranslator.translate(resource);

        assertEquals(resource, translatedResource);
    }
}
