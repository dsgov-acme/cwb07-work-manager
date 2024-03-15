package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentProcessingConfigurationTest {

    private DocumentProcessingConfiguration documentProcessingConfiguration;

    @BeforeEach
    public void setUp() {
        this.documentProcessingConfiguration = new DocumentProcessingConfiguration();
    }

    @Test
    void shouldApply() {
        assertTrue(documentProcessingConfiguration.canApplyTo(Document.class));
    }

    @Test
    void shouldNotApply() {
        assertFalse(documentProcessingConfiguration.canApplyTo(String.class));
    }
}
