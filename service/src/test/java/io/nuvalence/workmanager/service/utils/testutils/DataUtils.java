package io.nuvalence.workmanager.service.utils.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.utils.JsonFileLoader;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.util.UUID;

/**
 * Utilities for managing transaction data.
 */
public class DataUtils {
    private static final DynamicSchemaMapper schemaMapper =
            Mappers.getMapper(DynamicSchemaMapper.class);
    private static final ObjectMapper objectMapper = SpringConfig.getMapper();

    private static final JsonFileLoader jsonLoader = new JsonFileLoader();

    /**
     * Creates a test schema from sources.
     *
     * @return test schema.
     * @throws IOException exception if resource file is not found.
     */
    public static Schema createSchemaWithoutSavingToDb() throws IOException {
        String schemaString = jsonLoader.loadConfigString("/basicSchema.json");
        SchemaJson schemaJson = objectMapper.readValue(schemaString, SchemaJson.class);
        return schemaMapper.schemaJsonToSchema(schemaJson, UUID.randomUUID());
    }
}
