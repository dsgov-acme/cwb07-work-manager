package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.generated.models.NoteTypeModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Maps transaction to entity and model.
 */
@Mapper(componentModel = "spring")
public interface NoteTypeMapper {
    NoteTypeMapper INSTANCE = Mappers.getMapper(NoteTypeMapper.class);

    NoteTypeModel toModel(NoteType entity);

    NoteType toEntity(NoteTypeModel model);
}
