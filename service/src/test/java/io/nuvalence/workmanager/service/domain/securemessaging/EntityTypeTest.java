package io.nuvalence.workmanager.service.domain.securemessaging;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EntityTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetValue() {
        assertEquals("TRANSACTION", EntityType.TRANSACTION.getValue());
        assertEquals("EMPLOYER", EntityType.EMPLOYER.getValue());
    }

    @Test
    void testToString() {
        assertEquals("TRANSACTION", EntityType.TRANSACTION.toString());
        assertEquals("EMPLOYER", EntityType.EMPLOYER.toString());
    }

    @Test
    void testFromValue_ValidValue() {
        assertEquals(EntityType.TRANSACTION, EntityType.fromValue("TRANSACTION"));
        assertEquals(EntityType.EMPLOYER, EntityType.fromValue("EMPLOYER"));
    }

    @Test
    void testFromValue_InvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> EntityType.fromValue("INVALID_VALUE"));
    }

    @Test
    void testJsonValue() throws Exception {
        String jsonValue = objectMapper.writeValueAsString(EntityType.TRANSACTION);
        assertEquals("\"TRANSACTION\"", jsonValue);
    }

    @Test
    void testJsonCreator() throws Exception {
        EntityType parsedEntity = objectMapper.readValue("\"EMPLOYER\"", EntityType.class);
        assertEquals(EntityType.EMPLOYER, parsedEntity);
    }

    @Test
    void testEnumEquality() {
        assertSame(EntityType.TRANSACTION, EntityType.fromValue("TRANSACTION"));
        assertNotSame(EntityType.TRANSACTION, EntityType.EMPLOYER);
    }
}
