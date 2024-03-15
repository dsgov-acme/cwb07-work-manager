package io.nuvalence.workmanager.service.domain.transaction;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Unit tests for {@link TransactionLink}.
 */
class TransactionLinkTest {

    @Test
    void testEqualsSameReference() {
        TransactionLink transactionLink = new TransactionLink();
        assertEquals(
                transactionLink, transactionLink, "A transaction link should be equal to itself.");
    }

    @Test
    void testEqualsDifferentId() {
        TransactionLink transactionLink1 = new TransactionLink();
        transactionLink1.setId(UUID.randomUUID());

        TransactionLink transactionLink2 = new TransactionLink();
        transactionLink2.setId(UUID.randomUUID());

        assertNotEquals(
                transactionLink1,
                transactionLink2,
                "Two transaction links with different IDs should not be equal.");
    }

    @Test
    void testEqualsSameId() {
        UUID id = UUID.randomUUID();

        TransactionLink transactionLink1 = new TransactionLink();
        transactionLink1.setId(id);

        TransactionLink transactionLink2 = new TransactionLink();
        transactionLink2.setId(id);

        assertEquals(
                transactionLink1,
                transactionLink2,
                "Two transaction links with the same ID should be equal.");
    }
}
