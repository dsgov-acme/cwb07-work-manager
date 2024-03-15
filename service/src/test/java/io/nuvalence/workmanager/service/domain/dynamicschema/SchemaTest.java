package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class SchemaTest {
    @Test
    void equalsHashcodeContract() {
        EqualsVerifier.forClass(Schema.class)
                .withPrefabValues(
                        DynaClass.class,
                        new BasicDynaClass(
                                "redschema",
                                BasicDynaBean.class,
                                List.of(
                                                new DynaProperty("foo", String.class),
                                                new DynaProperty("bar", List.class, String.class),
                                                new DynaProperty("baz", Integer.class))
                                        .toArray(new DynaProperty[0])),
                        new BasicDynaClass(
                                "blueschema",
                                BasicDynaBean.class,
                                List.of(
                                                new DynaProperty("foo", String.class),
                                                new DynaProperty("baz", String.class))
                                        .toArray(new DynaProperty[0])))
                .verify();
    }

    @Test
    void testGetMyClassMapWhenAttributeExists() {
        String attribute = "testAttribute";
        String testProcessorId = "testProcessorId";
        DocumentProcessingConfiguration documentProcessingConfiguration =
                new DocumentProcessingConfiguration();
        documentProcessingConfiguration.setProcessorId(testProcessorId);
        Map<String, List<AttributeConfiguration>> attributeConfigurations = new HashMap<>();
        attributeConfigurations.put(
                attribute, Collections.singletonList(documentProcessingConfiguration));
        Schema schema =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .key("")
                        .name("")
                        .description("")
                        .properties(new ArrayList<>())
                        .attributeConfigurations(attributeConfigurations)
                        .build();

        List<DocumentProcessingConfiguration> result =
                schema.getAttributeConfigurations(attribute, DocumentProcessingConfiguration.class);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(result.get(0).getProcessorId(), testProcessorId);
    }
}
