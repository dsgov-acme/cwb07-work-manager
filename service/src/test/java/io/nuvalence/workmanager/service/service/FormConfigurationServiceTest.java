package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.repository.FormConfigurationRepository;
import io.nuvalence.workmanager.service.utils.UserUtility;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.AdditionalAnswers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FormConfigurationServiceTest {

    static String userType = "userType";
    private FormConfigurationRepository repository;
    private TransactionTaskService transactionTaskService;
    private FormConfigurationService service;
    private SchemaService schemaService;

    private static MockedStatic<UserUtility> initUserUtilityMock() {
        MockedStatic<UserUtility> staticUserUtility = Mockito.mockStatic(UserUtility.class);
        staticUserUtility.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);

        return staticUserUtility;
    }

    @BeforeEach
    void setUp() {
        repository = mock(FormConfigurationRepository.class);
        transactionTaskService = mock(TransactionTaskService.class);
        schemaService = mock(SchemaService.class);
        service = new FormConfigurationService(repository, transactionTaskService, schemaService);
    }

    @Test
    void directRepositoryWrappers() {
        // Arrange
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("transactionDefinitionKey")
                        .build();
        String formConfigurationKey = "formConfigurationKey";

        FormConfiguration formConfigWithId =
                getFormBaseObject(transactionDefinition.getKey(), formConfigurationKey);

        // test 1
        when(repository.searchByKeys(transactionDefinition.getKey(), formConfigurationKey))
                .thenReturn(List.of(formConfigWithId));

        var result =
                service.getFormConfigurationByKeys(
                                transactionDefinition.getKey(), formConfigurationKey)
                        .get();

        // Assert
        assert result != null;
        assertEquals("testerUser", result.getCreatedBy());

        // ---

        // test 2
        when(repository.findByTransactionDefinitionKey(transactionDefinition.getKey()))
                .thenReturn(List.of(formConfigWithId));
        result =
                service.getFormConfigurationsByTransactionDefinitionKey(
                                transactionDefinition.getKey())
                        .get(0);

        // Assert
        assert result != null;
        assertEquals("testerUser", result.getCreatedBy());
    }

    @Test
    void getActiveFormConfiguration() {

        String context = "context";
        String defaultFormDef = "defaultFormDef";
        String transactionDefKey = "transactionDefKey";
        String createdBy = "testerUser";
        String task1 = "task1";
        String task2 = "task2";

        Transaction transaction =
                configTest(
                        transactionDefKey,
                        defaultFormDef,
                        context,
                        createdBy,
                        List.of(task1, task2),
                        true,
                        false);

        try (var staticMock = initUserUtilityMock()) {

            // Act
            var result = service.getActiveFormConfiguration(transaction, context);

            // Assert
            assert result != null;
            assertEquals(2, result.size());
            assertEquals("testerUser", result.get(task1).getCreatedBy());
        }
    }

    @Test
    void getActiveFormConfigurationForCompletedProcess() {

        String context = "context";
        String defaultFormDef = "defaultFormDef";
        String transactionDefKey = "transactionDefKey";
        String createdBy = "testerUser";

        Transaction transaction =
                configTest(
                        transactionDefKey,
                        defaultFormDef,
                        context,
                        createdBy,
                        List.of(),
                        true,
                        false);

        try (var staticMock = initUserUtilityMock()) {

            // Act
            var result = service.getActiveFormConfiguration(transaction, context);

            // Assert
            assert result != null;
            assertEquals(1, result.size());
            assertEquals("testerUser", result.get("fallback").getCreatedBy());
        }
    }

    @Test
    void getFirstTasksForms() {

        String createdBy = "firstTaskSeeker";
        String task1 = "firstTask1";
        String task2 = "firstTask2";

        Transaction transaction =
                configTest(
                        "trxnDefKey",
                        "defaultFormDefKey",
                        null,
                        createdBy,
                        List.of(task1, task2),
                        false,
                        true);

        Map<String, FormConfiguration> result;
        try (var staticMock = initUserUtilityMock()) {
            result =
                    service.getFirstTasksFormConfigurations(
                            transaction.getTransactionDefinition(), null);
        }

        assertEquals(2, result.size());
        assertEquals(createdBy, result.get(task1).getCreatedBy());
        assertEquals(createdBy, result.get(task2).getCreatedBy());
    }

    @Test
    void getFormConfigurationByKeyAndRecordDefinitionKey() {
        String recordDefinitionKey = "recordDefinitionKey";
        String formKey = "formKey";

        FormConfiguration formConfiguration =
                getFormBaseObject("transactionDefinitionKey", formKey);

        when(repository.findByKeyAndRecordDefinitionKey(recordDefinitionKey, formKey))
                .thenReturn(List.of(formConfiguration));

        Optional<FormConfiguration> formConfigurationResult =
                service.getFormConfigurationByKeyAndRecordDefinitionKey(
                        formKey, recordDefinitionKey);

        assertTrue(formConfigurationResult.isPresent());
        assertEquals(formConfiguration, formConfigurationResult.get());
    }

    @Test
    void getFormConfigurationsByRecordDefinitionKey() {
        String recordDefinitionKey = "recordDefinitionKey";

        FormConfiguration formConfiguration =
                getFormBaseObject("transactionDefinitionKey", "formKey");
        when(repository.findByRecordDefinitionKey(recordDefinitionKey))
                .thenReturn(List.of(formConfiguration));

        List<FormConfiguration> result =
                service.getFormConfigurationsByRecordDefinitionKey(recordDefinitionKey);

        assertEquals(1, result.size());
        assertEquals(formConfiguration, result.get(0));
    }

    private Transaction configTest(
            String transactionDefKey,
            String defaultFormDef,
            String context,
            String createdBy,
            List<String> tasks,
            boolean generateActiveTasks,
            boolean generateFirstTasks) {
        FormConfiguration formConfig = FormConfiguration.builder().createdBy(createdBy).build();

        when(repository.searchByKeys(transactionDefKey, defaultFormDef))
                .thenReturn(List.of(formConfig));

        UUID transactionDefId = UUID.randomUUID();
        Transaction transaction =
                Transaction.builder().transactionDefinitionId(transactionDefId).build();

        if (generateActiveTasks) {
            when(transactionTaskService.getActiveTasksForCurrentUser(transaction))
                    .thenReturn(
                            tasks.stream()
                                    .map(task -> WorkflowTask.builder().key(task).build())
                                    .collect(Collectors.toList()));
        }

        TransactionDefinition transactionDefinition = mock(TransactionDefinition.class);
        when(transactionDefinition.getFormConfigurationKey(any(), eq(userType), eq(context)))
                .thenReturn(Optional.of(defaultFormDef));
        when(transactionDefinition.getKey()).thenReturn(transactionDefKey);

        if (generateFirstTasks) {
            when(transactionTaskService.getFirstTasksForCurrentUser(transactionDefinition))
                    .thenReturn(
                            tasks.stream()
                                    .map(task -> WorkflowTask.builder().key(task).build())
                                    .collect(Collectors.toList()));
        }

        transaction.setTransactionDefinition(transactionDefinition);

        return transaction;
    }

    private FormConfiguration getFormBaseObject(
            String transactionDefinitionKey, String formConfigurationKey) {

        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key(transactionDefinitionKey)
                        .build();

        return FormConfiguration.builder()
                .createdBy("testerUser")
                .transactionDefinition(transactionDefinition)
                .key(formConfigurationKey)
                .configuration(new HashMap<String, Object>())
                .id(UUID.randomUUID())
                .build();
    }

    private Schema getSchemaBaseObject(String schemaKey, boolean isParent) {
        List<DynaProperty> configs = new ArrayList<>();
        DynaProperty dp =
                isParent
                        ? new DynaProperty("child-component", DynamicEntity.class)
                        : new DynaProperty("child-component");
        configs.add(dp);
        return Schema.builder()
                .properties(configs)
                .relatedSchemas(isParent ? Map.of("child-component", "childSchemaKey") : Map.of())
                .key(schemaKey)
                .build();
    }

    private Schema getListSchemaBaseObject(String schemaKey, Class<?> contentType) {
        List<DynaProperty> configs = new ArrayList<>();
        DynaProperty dp = new DynaProperty("parent-component", List.class, contentType);

        boolean isParent = contentType.equals(DynamicEntity.class);

        configs.add(dp);
        return Schema.builder()
                .properties(configs)
                .relatedSchemas(isParent ? Map.of("parent-component", "childSchemaKey") : Map.of())
                .key(schemaKey)
                .build();
    }

    private void configureListFormConfig(
            FormConfiguration fc, Map<String, ? extends Serializable> childComponent) {
        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(
                                Map.of(
                                        "key",
                                        "parent-component",
                                        "input",
                                        true,
                                        "type",
                                        "nuverialFormList",
                                        "keyContextProvider",
                                        true,
                                        "components",
                                        List.of(childComponent)))));
    }

    private Map<String, Object> getSuccessfulCompleteNestedConfig() {
        return Map.of(
                "components",
                List.of(
                        Map.of(
                                "key",
                                "child-component",
                                "input",
                                true,
                                "components",
                                List.of(
                                        Map.of(
                                                "key",
                                                "child-component.grandchild-component",
                                                "input",
                                                false)))));
    }

    @Test
    void formConfigValidationNullSchema() {

        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");
        fc.setConfiguration(Map.of("components", List.of(Map.of("key", "child-component"))));

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("ROOT_COMPONENT", exceptionMessages.get(0).getControlName());
        assertEquals("Null schema key", exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationNotFoundSchema() {

        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);
        fc.setConfiguration(Map.of("components", List.of(Map.of("key", "child-component"))));

        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.empty());

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("ROOT_COMPONENT", exceptionMessages.get(0).getControlName());
        assertEquals(
                "Schema with key '" + schemaKey + "' not found.",
                exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationChildComponentNotDataBacked() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(Map.of("key", "NON-SCHEMA-COMPONENT", "input", false))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, false);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        // work with method data
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        var result = service.saveFormConfiguration(fc);

        // Assert
        assert result != null;
        assertNotNull(result.getId());
        var components = assertInstanceOf(List.class, result.getConfiguration().get("components"));
        assertEquals(1, components.size());
    }

    @Test
    void formConfigValidationChildComponentNotFoundInSchema() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(Map.of("key", "NON-SCHEMA-COMPONENT", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, false);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("NON-SCHEMA-COMPONENT", exceptionMessages.get(0).getControlName());
        assertEquals(
                "Component key not found in schema with key 'someSchema'",
                exceptionMessages.get(0).getErrorMessage());
    }

    @ParameterizedTest
    @MethodSource("provideClassesForTest")
    void formConfigValidationList_success(Class<?> clazz) {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        String childKey = clazz.equals(DynamicEntity.class) ? "child-component" : "text";
        var childComponent = Map.of("key", childKey, "input", true);
        configureListFormConfig(fc, childComponent);

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getListSchemaBaseObject(schemaKey, clazz);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        if (DynamicEntity.class.equals(clazz)) {
            String childSchemaKey = "childSchemaKey";
            when(schemaService.getSchemaByKey(childSchemaKey))
                    .thenReturn(Optional.of(getSchemaBaseObject(childSchemaKey, false)));
        }

        assertDoesNotThrow(() -> service.saveFormConfiguration(fc));
    }

    private static Stream<Arguments> provideClassesForTest() {
        return Stream.of(
                Arguments.of(DynamicEntity.class),
                Arguments.of(String.class),
                Arguments.of(Boolean.class),
                Arguments.of(LocalDate.class),
                Arguments.of(LocalTime.class),
                Arguments.of(Document.class),
                Arguments.of(Integer.class),
                Arguments.of(BigDecimal.class));
    }

    @Test
    void formConfigValidationDynamicEntityList_notFoundInSchema() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        var childComponent = Map.of("key", "NON-SCHEMA-COMPONENT", "input", true);
        configureListFormConfig(fc, childComponent);

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getListSchemaBaseObject(schemaKey, DynamicEntity.class);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        String childSchemaKey = "childSchemaKey";
        when(schemaService.getSchemaByKey(childSchemaKey))
                .thenReturn(Optional.of(getSchemaBaseObject(childSchemaKey, false)));

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals(
                "parent-component.NON-SCHEMA-COMPONENT", exceptionMessages.get(0).getControlName());
        assertEquals(
                "Component key not found in schema with key 'childSchemaKey'",
                exceptionMessages.get(0).getErrorMessage());
        ;
    }

    @Test
    void formConfigValidationComponentMissingKey() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(Map.of("components", List.of(Map.of("input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("A component is missing its key", exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationComponentDuplicateKey() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(
                                Map.of("key", "child-component", "input", true),
                                Map.of("key", "child-component", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals(
                "Component key is found more than once in the form configuration",
                exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationComponentEmptySectionKey() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(Map.of("key", "child-component.    .final", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals(
                "All components must have a non-blank key with non-blank sections",
                exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationSuccess() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of("components", List.of(Map.of("key", "child-component", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, false);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        // work with method data
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        var result = service.saveFormConfiguration(fc);

        // Assert
        assert result != null;
        assertNotNull(result.getId());
        var components = assertInstanceOf(List.class, result.getConfiguration().get("components"));
        assertEquals(1, components.size());
        var component = assertInstanceOf(Map.class, components.get(0));
        assertEquals("child-component", component.get("key"));
    }

    @Test
    void formConfigValidationNestedSuccess() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(getSuccessfulCompleteNestedConfig());

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, true);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        String childSchemaKey = "childSchemaKey";
        when(schemaService.getSchemaByKey(childSchemaKey))
                .thenReturn(Optional.of(getSchemaBaseObject(childSchemaKey, false)));

        // work with method data
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        var result = service.saveFormConfiguration(fc);

        // Assert
        assert result != null;
        assertNotNull(result.getId());
        var components = assertInstanceOf(List.class, result.getConfiguration().get("components"));
        assertEquals(1, components.size());
        var component = assertInstanceOf(Map.class, components.get(0));
        assertEquals("child-component", component.get("key"));

        var subComponents = assertInstanceOf(List.class, component.get("components"));
        assertEquals(1, subComponents.size());
        var subComponent = assertInstanceOf(Map.class, subComponents.get(0));
        assertEquals("child-component.grandchild-component", subComponent.get("key"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"SomeDefaultForm"})
    void testCreateDefaultFormConfiguration(String defaultFormConfigKey) {
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("testKey")
                        .name("testName")
                        .defaultFormConfigurationKey(defaultFormConfigKey)
                        .schemaKey("testSchemaKey")
                        .build();

        FormConfiguration result = service.createDefaultFormConfiguration(transactionDefinition);

        assertNotNull(result);
        assertEquals(
                (defaultFormConfigKey == null || defaultFormConfigKey.isBlank())
                        ? "testKeyDefault"
                        : defaultFormConfigKey,
                result.getKey());
        assertEquals("Default Form", result.getName());
        assertEquals("Default form for testName", result.getDescription());
        assertEquals("formio", result.getConfigurationSchema());
        assertEquals("testSchemaKey", result.getSchemaKey());
        assertEquals(transactionDefinition, result.getTransactionDefinition());
    }
}
