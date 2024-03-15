package io.nuvalence.workmanager.service.domain.securemessaging;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for Entity type.
 */
@Converter(autoApply = true)
public class EntityTypeConverter implements AttributeConverter<EntityType, String> {

    @Override
    public String convertToDatabaseColumn(EntityType entityType) {
        if (entityType == null) {
            return null;
        }
        return entityType.name();
    }

    @Override
    public EntityType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return EntityType.valueOf(value);
    }
}
