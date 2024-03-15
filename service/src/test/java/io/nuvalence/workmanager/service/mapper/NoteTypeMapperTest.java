package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.generated.models.NoteTypeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class NoteTypeMapperTest {

    private NoteType entity;
    private NoteTypeModel model;

    private NoteTypeMapper mapper;

    @BeforeEach
    void setup() {
        UUID id = UUID.randomUUID();
        entity = new NoteType(id, "test-type");
        model = new NoteTypeModel();
        model.setId(id);
        model.setName("test-type");
        mapper = NoteTypeMapper.INSTANCE;
    }

    @Test
    void testToModel() {
        NoteTypeModel result = mapper.toModel(entity);

        assertEquals(model.getId(), result.getId());
        assertEquals(model.getName(), result.getName());
    }

    @Test
    void testToEntity() {
        NoteType result = mapper.toEntity(model);

        assertEquals(entity.getId(), result.getId());
        assertEquals(entity.getName(), result.getName());
    }
}
