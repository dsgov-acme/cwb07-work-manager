package io.nuvalence.workmanager.service.domain.profile;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProfileTypeConverter implements AttributeConverter<ProfileType, String> {
    @Override
    public String convertToDatabaseColumn(ProfileType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ProfileType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ProfileType.valueOf(dbData);
    }
}
