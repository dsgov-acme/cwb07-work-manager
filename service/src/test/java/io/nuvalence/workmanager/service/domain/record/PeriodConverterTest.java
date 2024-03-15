package io.nuvalence.workmanager.service.domain.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.time.Period;

class PeriodConverterTest {

    private PeriodConverter converter = new PeriodConverter();

    @Test
    void testWithPreserveUnits() {

        String periodString = "P2Y5M16D";
        Period period = converter.convertToEntityAttribute(periodString);
        String periodString2 = converter.convertToDatabaseColumn(period);

        assertEquals(periodString, periodString2);
    }

    @Test
    void testWithWeeks() {

        String periodString = "P2Y5M3W16D";
        Period period = converter.convertToEntityAttribute(periodString);
        String periodString2 = converter.convertToDatabaseColumn(period);

        assertEquals("P2Y5M37D", periodString2);
    }
}
