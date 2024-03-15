package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for Rejection Reason enumerator.
 */
@Converter(autoApply = true)
public class RejectionReasonConverter implements AttributeConverter<RejectionReasonType, String> {

    @Override
    public String convertToDatabaseColumn(RejectionReasonType rejectionReason) {
        if (rejectionReason == null) {
            return null;
        }
        return rejectionReason.name();
    }

    @Override
    public RejectionReasonType convertToEntityAttribute(String rejectionReasonString) {
        if (rejectionReasonString == null) {
            return null;
        }
        return RejectionReasonType.valueOf(rejectionReasonString);
    }
}
