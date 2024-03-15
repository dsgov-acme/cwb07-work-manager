package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Period;
import java.time.format.DateTimeParseException;

/**
 * Record Definition mapper.
 */
@Mapper(componentModel = "spring")
public interface RecordDefinitionMapper {

    RecordDefinitionMapper INSTANCE = Mappers.getMapper(RecordDefinitionMapper.class);

    RecordDefinitionResponseModel recordDefinitionToResponseModel(
            RecordDefinition recordDefinition);

    RecordDefinition updateModelToRecordDefinition(RecordDefinitionUpdateModel updateModel);

    RecordDefinition createModelToRecordDefinition(RecordDefinitionCreateModel createModel);

    /**
     * Maps a string to a Period.
     *
     * @param value the string to map
     *
     * @return the Period
     */
    default Period map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Period.parse(value);
        } catch (DateTimeParseException e) {
            throw new ProvidedDataException(
                    "Please provide durations in ISO-8601 format for periods (units between years"
                            + " and days)");
        }
    }
}
