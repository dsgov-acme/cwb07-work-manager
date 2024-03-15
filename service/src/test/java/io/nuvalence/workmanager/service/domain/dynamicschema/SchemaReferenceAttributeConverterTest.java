package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.service.SchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.UUID;

class SchemaReferenceAttributeConverterTest {

    @Mock private SchemaService schemaService;

    @Mock private ApplicationContext applicationContext;

    private SchemaReferenceAttributeConverter converter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        converter = new SchemaReferenceAttributeConverter();
        converter.setApplicationContext(applicationContext);
    }

    @Test
    void testConvertToDatabaseColumn() {
        UUID id = UUID.randomUUID();
        Schema schema = Schema.builder().id(id).name("schema").build();

        UUID dbColumn = converter.convertToDatabaseColumn(schema);

        assertEquals(id, dbColumn);
    }

    @Test
    void testConvertToEntityAttribute_WhenSchemaExists() {
        UUID id = UUID.randomUUID();
        Schema schema = Schema.builder().id(id).name("schema").build();

        when(schemaService.getSchemaById(id)).thenReturn(Optional.of(schema));
        when(applicationContext.getBean(SchemaService.class)).thenReturn(schemaService);

        Schema entityAttribute = converter.convertToEntityAttribute(id);

        assertNotNull(entityAttribute);
        assertEquals(schema, entityAttribute);
    }

    @Test
    void testConvertToEntityAttribute_WhenSchemaDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(schemaService.getSchemaById(id)).thenReturn(Optional.empty());
        when(applicationContext.getBean(SchemaService.class)).thenReturn(schemaService);

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> converter.convertToEntityAttribute(id));
        assertEquals("Cannot load schema with id " + id, exception.getMessage());
    }
}
