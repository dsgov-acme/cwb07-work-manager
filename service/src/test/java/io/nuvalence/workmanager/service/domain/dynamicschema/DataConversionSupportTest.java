package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(OutputCaptureExtension.class)
class DataConversionSupportTest {

    @Test
    void convertStringToLocalDate() {
        assertEquals(
                LocalDate.of(2022, 4, 12),
                DataConversionSupport.convert("2022-04-12", LocalDate.class));
        assertNull(DataConversionSupport.convert("", LocalDate.class));
    }

    @Test
    void convertStringToLocalDate_when_isodatetime1() {
        assertEquals(
                LocalDate.of(2023, 5, 2),
                DataConversionSupport.convert("2023-05-02T06:00:00.000Z", LocalDate.class));
        assertNull(DataConversionSupport.convert("", LocalDate.class));
    }

    @Test
    void convertStringToLocalDate_when_isodatetime2() {
        assertEquals(
                LocalDate.of(2023, 5, 2),
                DataConversionSupport.convert("2023-05-02T06:00:00", LocalDate.class));
        assertNull(DataConversionSupport.convert("", LocalDate.class));
    }

    @Test
    void convertStringToLocalDate_when_isodatetime3() {
        assertEquals(
                LocalDate.of(2023, 5, 2),
                DataConversionSupport.convert("2023-05-02T06:00:00+02:00", LocalDate.class));
        assertNull(DataConversionSupport.convert("", LocalDate.class));
    }

    @Test
    void convertStringToLocalTime() {
        assertEquals(
                LocalTime.of(12, 30, 0),
                DataConversionSupport.convert("12:30:00", LocalTime.class));
        assertNull(DataConversionSupport.convert("", LocalTime.class));
    }

    @Test
    void convertStringToInteger() {
        assertEquals(5, DataConversionSupport.convert("5", Integer.class));
        assertNull(DataConversionSupport.convert("", Integer.class));
    }

    @Test
    void convertStringToBigDecimal() {
        assertEquals(BigDecimal.valueOf(5), DataConversionSupport.convert("5", BigDecimal.class));
        assertNull(DataConversionSupport.convert("", BigDecimal.class));
    }

    @Test
    void convertIntegerToBigDecimal() {
        assertEquals(new BigDecimal("5"), DataConversionSupport.convert(5, BigDecimal.class));
    }

    @Test
    void convertDoubleToBigDecimal() {
        assertEquals(new BigDecimal("5.99"), DataConversionSupport.convert(5.99, BigDecimal.class));
    }

    @Test
    void convertStringToBoolean() {
        assertTrue(DataConversionSupport.convert("yes", Boolean.class));
        assertTrue(DataConversionSupport.convert("Yes", Boolean.class));
        assertTrue(DataConversionSupport.convert("YES", Boolean.class));
        assertTrue(DataConversionSupport.convert("true", Boolean.class));
        assertTrue(DataConversionSupport.convert("True", Boolean.class));
        assertTrue(DataConversionSupport.convert("TRUE", Boolean.class));
        assertFalse(DataConversionSupport.convert("no", Boolean.class));
        assertFalse(DataConversionSupport.convert("No", Boolean.class));
        assertFalse(DataConversionSupport.convert("NO", Boolean.class));
        assertFalse(DataConversionSupport.convert("false", Boolean.class));
        assertFalse(DataConversionSupport.convert("False", Boolean.class));
        assertFalse(DataConversionSupport.convert("FALSE", Boolean.class));
        assertThrows(
                IllegalArgumentException.class,
                () -> DataConversionSupport.convert("Foo", Boolean.class));
        assertThrows(
                IllegalArgumentException.class,
                () -> DataConversionSupport.convert("", Boolean.class));
    }

    @Test
    void convertObjectMapToDocument() {
        Map<Object, Object> documentMap = new HashMap<Object, Object>();
        documentMap.put("documentId", "94c2ca16-dad1-11ec-b1ac-2aaa794f39fc");
        documentMap.put("filename", "document.png");

        Document expected =
                Document.builder()
                        .documentId(UUID.fromString("94c2ca16-dad1-11ec-b1ac-2aaa794f39fc"))
                        .filename("document.png")
                        .build();

        Document received = DataConversionSupport.convert(documentMap, Document.class);

        assertEquals(received.getDocumentId(), expected.getDocumentId());
    }

    @Test
    void convertObjectMapToDocument_noDocumentId() {
        Map<Object, Object> documentMap = new HashMap<>();
        documentMap.put("filename", "document.png");

        Document received = DataConversionSupport.convert(documentMap, Document.class);

        assertNull(received);
    }

    @Test
    void throwsUnsupportedOperationExceptionIfNoConverterFound() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> DataConversionSupport.convert("", DataConversionSupportTest.class));
    }

    @Test
    void convertWithNullValue() {
        assertNull(DataConversionSupport.convert(null, Document.class));
    }

    @Test
    void testConvertStringToLocalDate_WhenValueIsInvalid(CapturedOutput output) {
        String value = "InvalidValue";
        LocalDate result = DataConversionSupport.convert(value, LocalDate.class);

        assertNull(result);
        assertTrue(
                output.getOut()
                        .contains(
                                "Could not convert value \"InvalidValue\" to LocalDate value."
                                        + " Returning null."));
    }
}
