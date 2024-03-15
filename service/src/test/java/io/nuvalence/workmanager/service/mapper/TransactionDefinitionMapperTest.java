package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.FormConfigurationSelectionRule;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.generated.models.FormConfigSelectionRuleExportModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationSelectionRuleModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionUpdateModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class TransactionDefinitionMapperTest {
    private TransactionDefinition transactionDefinition;
    private TransactionDefinitionResponseModel model;
    private TransactionDefinitionUpdateModel updateModel;
    private TransactionDefinitionExportModel exportModel;
    private TransactionDefinitionMapper mapper;

    @BeforeEach
    void setup() {
        final UUID id = UUID.randomUUID();

        FormConfigurationSelectionRule selectionRule =
                FormConfigurationSelectionRule.builder()
                        .id(UUID.randomUUID())
                        .task("taskKey")
                        .viewer("viewerKey")
                        .context("contextKey")
                        .formConfigurationKey("formKey")
                        .build();

        transactionDefinition =
                TransactionDefinition.builder()
                        .id(id)
                        .key("test")
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("testschema")
                        .subjectType(ProfileType.INDIVIDUAL)
                        .allowedRelatedPartyTypes(Set.of(ProfileType.INDIVIDUAL))
                        .transactionDefinitionSetKey("setKey")
                        .isPublicVisible(true)
                        .formConfigurationSelectionRules(List.of(selectionRule))
                        .build();
        exportModel =
                new TransactionDefinitionExportModel()
                        .key("test")
                        .name("test transaction")
                        .schemaKey("testschema")
                        .processDefinitionKey("process-definition-key")
                        .subjectType("INDIVIDUAL")
                        .allowedRelatedPartyTypes(List.of("INDIVIDUAL"))
                        .isPublicVisible(true)
                        .transactionDefinitionSetKey("setKey")
                        .formConfigurationSelectionRules(
                                List.of(
                                        new FormConfigSelectionRuleExportModel()
                                                .task("taskKey")
                                                .viewer("viewerKey")
                                                .context("contextKey")
                                                .formConfigurationKey("formKey")));
        model =
                new TransactionDefinitionResponseModel()
                        .id(id)
                        .key("test")
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .subjectType("INDIVIDUAL")
                        .allowedRelatedPartyTypes(List.of("INDIVIDUAL"))
                        .schemaKey("testschema")
                        .transactionDefinitionSetKey("setKey")
                        .formConfigurationSelectionRules(
                                List.of(
                                        new FormConfigurationSelectionRuleModel()
                                                .task("taskKey")
                                                .viewer("viewerKey")
                                                .context("contextKey")
                                                .formConfigurationKey("formKey")))
                        .isPublicVisible(true);

        updateModel =
                new TransactionDefinitionUpdateModel()
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .subjectType("INDIVIDUAL")
                        .allowedRelatedPartyTypes(List.of("INDIVIDUAL"))
                        .schemaKey("testschema");
        mapper = TransactionDefinitionMapper.INSTANCE;
    }

    @Test
    void transactionDefinitionToResponseModel() {
        assertEquals(model, mapper.transactionDefinitionToResponseModel(transactionDefinition));
        assertNull(mapper.transactionDefinitionToResponseModel(null));
    }

    @Test
    void updateModelToTransactionDefinition() {
        assertTransactionDefinitionsEqual(
                transactionDefinition,
                mapper.updateModelToTransactionDefinition(updateModel),
                true);
        assertNull(mapper.updateModelToTransactionDefinition(null));
    }

    @Test
    void transactionDefinitionToTransactionDefinitionExportModel() {
        assertEquals(
                exportModel,
                mapper.transactionDefinitionToTransactionDefinitionExportModel(
                        transactionDefinition));
        assertNull(mapper.transactionDefinitionToTransactionDefinitionExportModel(null));
    }

    @Test
    void stringsToProfileTypes() {
        List<String> strings = List.of("INDIVIDUAL", "EMPLOYER");
        Set<ProfileType> expected = Set.of(ProfileType.INDIVIDUAL, ProfileType.EMPLOYER);

        Set<ProfileType> result = mapper.stringsToProfileTypes(strings);

        assertEquals(expected, result);
    }

    @Test
    void stringsToProfileTypes_NullInput() {
        List<String> strings = null;
        Set<ProfileType> expected = new HashSet<>();

        Set<ProfileType> result = mapper.stringsToProfileTypes(strings);

        assertEquals(expected, result);
    }

    @Test
    void stringToProfileType() {
        assertEquals(ProfileType.INDIVIDUAL, mapper.stringToProfileType("INDIVIDUAL"));
        assertEquals(ProfileType.EMPLOYER, mapper.stringToProfileType("EMPLOYER"));
        assertEquals(ProfileType.INDIVIDUAL, mapper.stringToProfileType(null));
    }

    @Test
    void testFormConfigurationSelectionRuleToExportModel_nullInput() {
        List<FormConfigurationSelectionRule> input = null;
        List<FormConfigSelectionRuleExportModel> result =
                mapper.formConfigurationSelectionRuleToExportModel(input);

        Assertions.assertEquals(Collections.emptyList(), result);
    }

    private void assertTransactionDefinitionsEqual(
            final TransactionDefinition a, final TransactionDefinition b, boolean ignoreIdFields) {
        if (!ignoreIdFields) {
            assertEquals(a.getId(), b.getId());
        }
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getProcessDefinitionKey(), b.getProcessDefinitionKey());
        assertEquals(a.getSchemaKey(), b.getSchemaKey());
    }
}
