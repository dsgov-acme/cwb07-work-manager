package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for Review Status enumerator.
 */
@Converter(autoApply = true)
public class ReviewStatusConverter implements AttributeConverter<ReviewStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReviewStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public ReviewStatus convertToEntityAttribute(String status) {
        if (status == null) {
            return null;
        }
        return ReviewStatus.valueOf(status);
    }
}
