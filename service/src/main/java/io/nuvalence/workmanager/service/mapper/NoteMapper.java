package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.Note;
import io.nuvalence.workmanager.service.generated.models.NoteCreationModelRequest;
import io.nuvalence.workmanager.service.generated.models.NoteModelResponse;
import io.nuvalence.workmanager.service.generated.models.NoteUpdateModelRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for notes.
 */
@Mapper(componentModel = "spring", uses = NoteTypeMapper.class)
public interface NoteMapper {

    /**
     * Map a note to a note model response.
     *
     * @param note note
     * @return note model response
     */
    NoteModelResponse noteToNoteModelResponse(Note note);

    /**
     * Map a note model request to a note.
     *
     * @param noteModelRequest note model request
     * @return note
     */
    @Mapping(target = "type", ignore = true)
    Note noteModelRequestToNote(NoteCreationModelRequest noteModelRequest);

    /**
     * Map a note update model request to a note.
     *
     * @param noteUpdateModelRequest note update model request
     * @return note
     */
    @Mapping(target = "type", ignore = true)
    Note noteUpdateModelRequestToNote(NoteUpdateModelRequest noteUpdateModelRequest);
}
