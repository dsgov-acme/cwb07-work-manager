package io.nuvalence.workmanager.service.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class AllowedLinkTest {

    @Test
    void testEquals() {
        UUID id = UUID.randomUUID();
        String transactionDefinitionKey = "transactionDef";
        TransactionLinkType transactionLinkType = new TransactionLinkType();

        AllowedLink link1 = new AllowedLink();
        link1.setId(id);
        link1.setTransactionDefinitionKey(transactionDefinitionKey);
        link1.setTransactionLinkType(transactionLinkType);

        AllowedLink link2 = new AllowedLink();
        link2.setId(id);
        link2.setTransactionDefinitionKey(transactionDefinitionKey);
        link2.setTransactionLinkType(transactionLinkType);

        assertEquals(link2, link1);
        assertEquals(link1, link2);
    }

    @Test
    void testNotEquals() {
        AllowedLink link1 = new AllowedLink();
        link1.setId(UUID.randomUUID());
        link1.setTransactionDefinitionKey("transactionDef");
        link1.setTransactionLinkType(new TransactionLinkType());

        AllowedLink link2 = new AllowedLink();
        link2.setId(UUID.randomUUID());
        link2.setTransactionDefinitionKey("transactionDef");
        link2.setTransactionLinkType(new TransactionLinkType());

        assertNotEquals(link2, link1);
        assertNotEquals(link1, link2);
    }

    @Test
    void testHashCode() {
        UUID id = UUID.randomUUID();
        String transactionDefinitionKey = "transactionDef";
        TransactionLinkType transactionLinkType = new TransactionLinkType();

        AllowedLink link1 = new AllowedLink();
        link1.setId(id);
        link1.setTransactionDefinitionKey(transactionDefinitionKey);
        link1.setTransactionLinkType(transactionLinkType);

        AllowedLink link2 = new AllowedLink();
        link2.setId(id);
        link2.setTransactionDefinitionKey(transactionDefinitionKey);
        link2.setTransactionLinkType(transactionLinkType);

        assertEquals(link1.hashCode(), link2.hashCode());
    }
}
