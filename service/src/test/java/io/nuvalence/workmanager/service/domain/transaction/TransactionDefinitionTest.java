package io.nuvalence.workmanager.service.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class TransactionDefinitionTest {

    @Mock TransactionDefinition transactionDefinition;

    List<FormConfigurationSelectionRule> selectionRules = new ArrayList<>();

    private String context;
    private String formConfigurationKey;
    private String task;
    private String viewer;

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

        FormConfigurationSelectionRule rule1 =
                FormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .task("task1")
                        .viewer("viewer1")
                        .context("context1")
                        .formConfigurationKey("defaultFormConfigurationKey1")
                        .build();
        FormConfigurationSelectionRule rule2 =
                FormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .task("task2")
                        .viewer("viewer2")
                        .context("context2")
                        .formConfigurationKey("defaultFormConfigurationKey2")
                        .build();

        TransactionRecordLinker recordLinker1 =
                TransactionRecordLinker.builder()
                        .id(UUID.randomUUID())
                        .recordDefinitionKey("recordDefinitionKey1")
                        .fieldMappings(new HashMap<>())
                        .transactionDefinition(transactionDefinition)
                        .build();

        TransactionRecordLinker recordLinker2 =
                TransactionRecordLinker.builder()
                        .id(UUID.randomUUID())
                        .recordDefinitionKey("recordDefinitionKey2")
                        .fieldMappings(new HashMap<>())
                        .transactionDefinition(transactionDefinition)
                        .build();

        EqualsVerifier.forClass(TransactionDefinition.class)
                .withPrefabValues(FormConfiguration.class, formConfiguration1, formConfiguration2)
                .withPrefabValues(FormConfigurationSelectionRule.class, rule1, rule2)
                .withPrefabValues(TransactionRecordLinker.class, recordLinker1, recordLinker2)
                .usingGetClass()
                .verify();
    }

    @Test
    void testGetFormConfigurationKey_MatchingRule() {
        mockData("formConfigKey");
        Optional<String> result =
                transactionDefinition.getFormConfigurationKey(task, viewer, context);

        assertTrue(result.isPresent());
        assertEquals(formConfigurationKey, result.get());
    }

    @Test
    void testGetFormConfigurationKey_NoMatchingRule() {
        String defaultFormConfigurationKey = "defaultFormConfigKey";
        mockData(defaultFormConfigurationKey);

        Optional<String> result =
                transactionDefinition.getFormConfigurationKey(task, viewer, context);

        assertTrue(result.isPresent());
        assertEquals(defaultFormConfigurationKey, result.get());
    }

    private void mockData(String formConfigKey) {
        task = "task";
        viewer = "viewer";
        context = "context";
        formConfigurationKey = formConfigKey;
        FormConfigurationSelectionRule rule =
                FormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .task(task)
                        .viewer(viewer)
                        .context(context)
                        .formConfigurationKey(formConfigurationKey)
                        .build();
        selectionRules.add(
                FormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .task("task1")
                        .viewer("viewer1")
                        .context("context1")
                        .formConfigurationKey("formConfigKey1")
                        .build());
        selectionRules.add(rule);
        selectionRules.add(
                FormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .task("task2")
                        .viewer("viewer2")
                        .context("context2")
                        .formConfigurationKey("formConfigKey2")
                        .build());

        transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("key")
                        .name("name")
                        .category("category")
                        .processDefinitionKey("processDefinitionKey")
                        .formConfigurationSelectionRules(selectionRules)
                        .build();
    }
}
