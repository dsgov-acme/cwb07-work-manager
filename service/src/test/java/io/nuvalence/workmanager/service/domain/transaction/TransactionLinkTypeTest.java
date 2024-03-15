package io.nuvalence.workmanager.service.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class TransactionLinkTypeTest {

    @Test
    void testEqualsAndHashCode() {
        TransactionLinkType linkType1 = new TransactionLinkType();
        linkType1.setId(UUID.randomUUID());
        linkType1.setName("Name");
        linkType1.setFromDescription("FromDescription");
        linkType1.setToDescription("ToDescription");

        TransactionLinkType linkType2 = new TransactionLinkType();
        linkType2.setId(linkType1.getId());
        linkType2.setName(linkType1.getName());
        linkType2.setFromDescription(linkType1.getFromDescription());
        linkType2.setToDescription(linkType1.getToDescription());

        assertEquals(
                linkType1,
                linkType2,
                "Two TransactionLinkType objects with the same properties should be equal");
        assertEquals(
                linkType1.hashCode(),
                linkType2.hashCode(),
                "Two equal TransactionLinkType objects should have the same hashCode");

        linkType2.setId(UUID.randomUUID());
        assertNotEquals(
                linkType1,
                linkType2,
                "Two TransactionLinkType objects with different ids should not be equal");
        assertNotEquals(
                linkType1.hashCode(),
                linkType2.hashCode(),
                "Two unequal TransactionLinkType objects should have different hashCodes");
    }
}
