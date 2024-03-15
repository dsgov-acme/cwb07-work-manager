package io.nuvalence.workmanager.service.domain.dynamicschema;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nuvalence.workmanager.service.service.SchemaService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter that handles referencing a schema by name in database.
 */
@Service
@Converter
public class SchemaReferenceAttributeConverter
        implements AttributeConverter<Schema, UUID>, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    @SuppressFBWarnings(
            value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification =
                    "This is an established pattern for exposing spring state to static contexts."
                        + " The applicationContext is a singleton, so if this write were to occur"
                        + " multiple times, it would be idempotent.")
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        SchemaReferenceAttributeConverter.applicationContext = applicationContext;
    }

    @Override
    public UUID convertToDatabaseColumn(final Schema attribute) {
        return attribute.getId();
    }

    @Override
    public Schema convertToEntityAttribute(final UUID dbData) {
        final SchemaService schemaService = applicationContext.getBean(SchemaService.class);
        return schemaService
                .getSchemaById(dbData)
                .orElseThrow(() -> new RuntimeException("Cannot load schema with id " + dbData));
    }
}
