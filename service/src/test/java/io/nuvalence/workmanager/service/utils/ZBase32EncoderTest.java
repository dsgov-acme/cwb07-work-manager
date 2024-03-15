package io.nuvalence.workmanager.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:abbreviationaswordinname")
class ZBase32EncoderTest {

    @Test
    void encodeTest() {
        assertEquals("w", ZBase32Encoder.encode(10));
        assertEquals("6", ZBase32Encoder.encode(15));
        assertEquals("be", ZBase32Encoder.encode(20));
        assertEquals("jc", ZBase32Encoder.encode(150));
        assertEquals("jqhdj6", ZBase32Encoder.encode(158795423));
    }
}
