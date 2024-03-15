package io.nuvalence.workmanager.service.domain.transaction;

import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.record.RecordFormConfigurationSelectionRule;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

class RecordDefinitionTest {

    @Test
    void equalsHashcodeContract() {

        FormConfiguration formConfiguration1 =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .key("defaultFormConfigurationKey1")
                        .name("defaultFormConfigurationName1")
                        .schemaKey("defaultFormConfigurationSchemaKey1")
                        .configurationSchema("formio")
                        .configuration(new HashMap<>())
                        .build();
        FormConfiguration formConfiguration2 =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .key("defaultFormConfigurationKey2")
                        .name("defaultFormConfigurationName2")
                        .schemaKey("defaultFormConfigurationSchemaKey2")
                        .configurationSchema("formio")
                        .configuration(new HashMap<>())
                        .build();

        RecordFormConfigurationSelectionRule rule1 =
                RecordFormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .viewer("viewer1")
                        .context("context1")
                        .formConfigurationKey("defaultFormConfigurationKey1")
                        .build();
        RecordFormConfigurationSelectionRule rule2 =
                RecordFormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .viewer("viewer2")
                        .context("context2")
                        .formConfigurationKey("defaultFormConfigurationKey2")
                        .build();

        EqualsVerifier.forClass(RecordDefinition.class)
                .withPrefabValues(FormConfiguration.class, formConfiguration1, formConfiguration2)
                .withPrefabValues(RecordFormConfigurationSelectionRule.class, rule1, rule2)
                .usingGetClass()
                .verify();
    }
}
