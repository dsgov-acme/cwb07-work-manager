package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentProcessingConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.AttributeConfigurationJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.DocumentProcessingConfigurationJson;
import io.nuvalence.workmanager.service.generated.models.AttributeConfigurationModel;
import io.nuvalence.workmanager.service.generated.models.DocumentProcessorConfigurationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AttributeConfigurationMapperTest {

    private static DocumentProcessingConfigurationJson json;

    private static DocumentProcessingConfiguration attributeConfiguration;

    private static DocumentProcessorConfigurationModel attributeConfigurationModel;

    private final String processorId = "testProcessorId";
    private final AttributeConfigurationMapper mapper =
            Mappers.getMapper(AttributeConfigurationMapper.class);

    @BeforeEach
    void setUp() {
        json = new DocumentProcessingConfigurationJson();
        json.setProcessorId(processorId);

        attributeConfiguration = new DocumentProcessingConfiguration();
        attributeConfiguration.setProcessorId(processorId);

        attributeConfigurationModel = new DocumentProcessorConfigurationModel();
        attributeConfigurationModel.setProcessorId(processorId);
    }

    @Test
    void attributeJsonToAttributeTest() {
        AttributeConfiguration result = mapper.attributeJsonToAttribute(json);

        assertTrue(result instanceof DocumentProcessingConfiguration);
        assertEquals(processorId, ((DocumentProcessingConfiguration) result).getProcessorId());
    }

    @Test
    void attributeToAttributeJsonTest() {
        AttributeConfigurationJson result = mapper.attributeToAttributeJson(attributeConfiguration);

        assertTrue(result instanceof DocumentProcessingConfigurationJson);
        assertEquals(processorId, ((DocumentProcessingConfigurationJson) result).getProcessorId());
    }

    @Test
    void attributeToAttributeModelTest() {

        AttributeConfigurationModel result =
                mapper.attributeToAttributeModel(attributeConfiguration);

        assertTrue(result instanceof DocumentProcessorConfigurationModel);
        assertEquals(processorId, ((DocumentProcessorConfigurationModel) result).getProcessorId());
    }

    @Test
    void attributeModelToAttributeTest() {

        AttributeConfiguration result =
                mapper.attributeModelToAttribute(attributeConfigurationModel);

        assertTrue(result instanceof DocumentProcessingConfiguration);
        assertEquals(processorId, ((DocumentProcessingConfiguration) result).getProcessorId());
    }
}
