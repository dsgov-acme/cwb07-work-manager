package io.nuvalence.workmanager.service.domain.transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransactionPriorityTest {

    @Test
    void testGetValue() {
        Assertions.assertEquals("LOW", TransactionPriority.LOW.getValue());
        Assertions.assertEquals("MEDIUM", TransactionPriority.MEDIUM.getValue());
        Assertions.assertEquals("HIGH", TransactionPriority.HIGH.getValue());
        Assertions.assertEquals("URGENT", TransactionPriority.URGENT.getValue());
    }

    @Test
    void testGetRank() {
        Assertions.assertEquals(10, TransactionPriority.LOW.getRank());
        Assertions.assertEquals(20, TransactionPriority.MEDIUM.getRank());
        Assertions.assertEquals(30, TransactionPriority.HIGH.getRank());
        Assertions.assertEquals(40, TransactionPriority.URGENT.getRank());
    }

    @Test
    void testGetLabel() {
        Assertions.assertEquals("Low", TransactionPriority.LOW.getLabel());
        Assertions.assertEquals("Medium", TransactionPriority.MEDIUM.getLabel());
        Assertions.assertEquals("High", TransactionPriority.HIGH.getLabel());
        Assertions.assertEquals("Urgent", TransactionPriority.URGENT.getLabel());
    }

    @Test
    void testFromRank() {
        Assertions.assertEquals(TransactionPriority.LOW, TransactionPriority.fromRank(10));
        Assertions.assertEquals(TransactionPriority.MEDIUM, TransactionPriority.fromRank(20));
        Assertions.assertEquals(TransactionPriority.HIGH, TransactionPriority.fromRank(30));
        Assertions.assertEquals(TransactionPriority.URGENT, TransactionPriority.fromRank(40));

        // Test an invalid value
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> TransactionPriority.fromRank(5));
    }

    @Test
    void testToString() {
        Assertions.assertEquals("LOW", TransactionPriority.LOW.toString());
        Assertions.assertEquals("MEDIUM", TransactionPriority.MEDIUM.toString());
        Assertions.assertEquals("HIGH", TransactionPriority.HIGH.toString());
        Assertions.assertEquals("URGENT", TransactionPriority.URGENT.toString());
    }
}
