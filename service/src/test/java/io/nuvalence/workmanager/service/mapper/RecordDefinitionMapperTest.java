package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionUpdateModel;
import org.junit.jupiter.api.Test;

import java.time.Period;

class RecordDefinitionMapperTest {

    private RecordDefinitionMapper mapper = RecordDefinitionMapper.INSTANCE;

    @Test
    void testCreateModelToRecordDefinition() {

        RecordDefinitionCreateModel createModel = new RecordDefinitionCreateModel();

        createModel.setKey("rdKey");
        createModel.setName("rdName");
        createModel.setDescription("rdDescription");
        createModel.setSchemaKey("rdSchemaKey");

        createModel.setExpirationDuration("P1Y2M3D");

        RecordDefinition recordDefinition = mapper.createModelToRecordDefinition(createModel);

        assertEquals("rdKey", recordDefinition.getKey());
        assertEquals("rdName", recordDefinition.getName());
        assertEquals("rdDescription", recordDefinition.getDescription());
        assertEquals("rdSchemaKey", recordDefinition.getSchemaKey());
        assertEquals(Period.parse("P1Y2M3D"), recordDefinition.getExpirationDuration());
    }

    @Test
    void testRecordDefinitionToResponseModel() {

        RecordDefinition recordDefinition = new RecordDefinition();

        recordDefinition.setKey("rdKey");
        recordDefinition.setName("rdName");
        recordDefinition.setDescription("rdDescription");
        recordDefinition.setSchemaKey("rdSchemaKey");

        recordDefinition.setExpirationDuration(Period.parse("P1Y2M3D"));

        RecordDefinitionResponseModel responseModel =
                mapper.recordDefinitionToResponseModel(recordDefinition);

        assertEquals("rdKey", responseModel.getKey());
        assertEquals("rdName", responseModel.getName());
        assertEquals("rdDescription", responseModel.getDescription());
        assertEquals("rdSchemaKey", responseModel.getSchemaKey());
        assertEquals("P1Y2M3D", responseModel.getExpirationDuration());
    }

    @Test
    void testUpdateModelToRecordDefinition_FailedDuration() {

        RecordDefinitionUpdateModel updateModel = new RecordDefinitionUpdateModel();

        updateModel.setName("rdName");
        updateModel.setDescription("rdDescription");
        updateModel.setSchemaKey("rdSchemaKey");

        updateModel.setExpirationDuration("P1YT48H");

        ProvidedDataException exception =
                assertThrows(
                        ProvidedDataException.class,
                        () -> {
                            mapper.updateModelToRecordDefinition(updateModel);
                        });

        assertEquals(
                "Please provide durations in ISO-8601 format for periods (units between years and"
                        + " days)",
                exception.getMessage());
    }

    @Test
    void testCreateModelToRecordDefinition_NullDuration() {

        RecordDefinitionCreateModel createModel = new RecordDefinitionCreateModel();

        createModel.setKey("rdKey");
        createModel.setName("rdName");
        createModel.setSchemaKey("rdSchemaKey");

        RecordDefinition recordDefinition = mapper.createModelToRecordDefinition(createModel);

        assertEquals("rdKey", recordDefinition.getKey());
        assertEquals("rdName", recordDefinition.getName());
        assertEquals("rdSchemaKey", recordDefinition.getSchemaKey());
        assertEquals(null, recordDefinition.getExpirationDuration());
    }
}
