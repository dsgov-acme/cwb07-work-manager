package io.nuvalence.workmanager.service.domain.transaction;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for Transaction Priority enumerator.
 */
@Converter(autoApply = true)
public class TransactionPriorityConverter
        implements AttributeConverter<TransactionPriority, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TransactionPriority priority) {
        if (priority == null) {
            return null;
        }
        return priority.getRank();
    }

    @Override
    public TransactionPriority convertToEntityAttribute(Integer value) {
        if (value == null) {
            return null;
        }
        return TransactionPriority.fromRank(value);
    }
}
