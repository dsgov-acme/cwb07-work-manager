package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import org.junit.jupiter.api.Test;

class DocumentClassifierConfigurationTest {
    @Test
    void testCanApplyTo_WhenTypeIsDocument() {
        DocumentClassifierConfiguration configuration = new DocumentClassifierConfiguration();
        Class<?> documentClass = Document.class;

        boolean canApply = configuration.canApplyTo(documentClass);

        assertTrue(canApply);
    }

    @Test
    void testCanApplyTo_WhenTypeIsNotDocument() {
        DocumentClassifierConfiguration configuration = new DocumentClassifierConfiguration();
        Class<?> otherClass = String.class;

        boolean canApply = configuration.canApplyTo(otherClass);

        assertFalse(canApply);
    }
}
