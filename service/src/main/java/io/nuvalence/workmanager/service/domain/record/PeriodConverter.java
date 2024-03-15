package io.nuvalence.workmanager.service.domain.record;

import java.time.Period;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts java.time.Period to String for persistence.
 */
@Converter(autoApply = true)
public class PeriodConverter implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period attribute) {
        // the only challenge for java.time.Period is that weeks will be converted to days format
        // joda-time is an alternative with other implications
        return attribute.toString();
    }

    @Override
    public Period convertToEntityAttribute(String dbData) {
        return Period.parse(dbData);
    }
}
