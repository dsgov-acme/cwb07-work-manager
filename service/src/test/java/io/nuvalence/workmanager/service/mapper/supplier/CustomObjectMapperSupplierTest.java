package io.nuvalence.workmanager.service.mapper.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

class CustomObjectMapperSupplierTest {

    @Test
    void testGet() {
        // Arrange
        CustomObjectMapperSupplier supplier = new CustomObjectMapperSupplier();

        // Act
        ObjectMapper objectMapper = supplier.get();

        // Assert
        assertNotNull(objectMapper, "ObjectMapper should not be null");
        assertFalse(
                objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
                "FAIL_ON_UNKNOWN_PROPERTIES should be disabled");
        JsonInclude.Value serializationInclusion =
                objectMapper.getSerializationConfig().getDefaultPropertyInclusion();
        assertEquals(
                JsonInclude.Include.NON_NULL,
                serializationInclusion.getValueInclusion(),
                "Serialization inclusion should be NON_NULL");
        DateFormat dateFormat = objectMapper.getDateFormat();
        assertTrue(
                dateFormat instanceof SimpleDateFormat,
                "DateFormat should be an instance of SimpleDateFormat");
        assertEquals(
                "yyyy-MM-dd HH:mm:ss",
                ((SimpleDateFormat) dateFormat).toPattern(),
                "Date format pattern should match the expected pattern");
    }
}
