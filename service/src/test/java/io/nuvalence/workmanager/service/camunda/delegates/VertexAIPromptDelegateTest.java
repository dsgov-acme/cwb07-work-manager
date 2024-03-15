package io.nuvalence.workmanager.service.camunda.delegates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.service.vertexai.VertexAIClient;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import org.apache.commons.beanutils.DynaProperty;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

class VertexAIPromptDelegateTest {

    private TransactionService transactionService;
    private DelegateExecution execution;
    private VertexAIPromptDelegate delegate;
    private VertexAIClient vertexAIClient;

    @BeforeEach
    void setUp() {
        transactionService = mock(TransactionService.class);
        execution = mock(DelegateExecution.class);
        vertexAIClient = mock(VertexAIClient.class);

        delegate = new VertexAIPromptDelegate(transactionService, vertexAIClient);
    }

    @Test
    void testExecute_transactionIdNotFound() throws Exception {
        when(execution.getVariable("transactionId")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> delegate.execute(execution));
    }

    @Test
    void testExecute_transactionIdFound_transactionNotFound() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(any(UUID.class))).thenReturn(Optional.empty());

        try (MockedStatic<CamundaPropertiesUtils> mockedStatic =
                mockStatic(CamundaPropertiesUtils.class)) {
            extensionPropertiesStubs(mockedStatic, false, false, false);
            delegate.execute(execution);

            verify(execution, never()).setVariable(anyString(), any());
        }
    }

    @Test
    void testExecute_success() throws Exception {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .data(new DynamicEntity(getSchema()))
                        .subjectProfileId(UUID.randomUUID())
                        .build();

        String prompt = "Some text";
        transaction.getData().set("var1", prompt);

        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(any(UUID.class)))
                .thenReturn(Optional.of(transaction));
        when(vertexAIClient.askVertexAI(any(String.class)))
                .thenReturn(Optional.of("responsePart1-responsePart2-responsePart3"));

        try (MockedStatic<CamundaPropertiesUtils> mockedStatic =
                mockStatic(CamundaPropertiesUtils.class)) {
            extensionPropertiesStubs(mockedStatic, false, false, false);
            delegate.execute(execution);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(execution, times(3)).setVariable(anyString(), captor.capture());

            List<String> allValues = captor.getAllValues();
            assertEquals("responsePart1", allValues.get(0));
            assertEquals("responsePart2", allValues.get(1));
            assertEquals("responsePart3", allValues.get(2));
        }
    }

    @ParameterizedTest
    @MethodSource("extensionPropertyTestCases")
    void testExecute_extensionPropertyNotFound(
            boolean promptPrefixIsEmpty,
            boolean promptSuffixPathIsEmpty,
            boolean resultsParsePatternIsEmpty)
            throws Exception {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .data(new DynamicEntity(getSchema()))
                        .subjectProfileId(UUID.randomUUID())
                        .build();

        String prompt = "Some text";
        transaction.getData().set("var1", prompt);

        when(execution.getVariable("transactionId")).thenReturn(transactionId);

        try (MockedStatic<CamundaPropertiesUtils> mockedStatic =
                mockStatic(CamundaPropertiesUtils.class)) {
            extensionPropertiesStubs(
                    mockedStatic,
                    promptPrefixIsEmpty,
                    promptSuffixPathIsEmpty,
                    resultsParsePatternIsEmpty);

            if (!promptPrefixIsEmpty && !promptSuffixPathIsEmpty && !resultsParsePatternIsEmpty) {
                when(execution.getVariable("transactionId")).thenReturn(transactionId);
                when(transactionService.getTransactionById(any(UUID.class)))
                        .thenReturn(Optional.of(transaction));
                when(vertexAIClient.askVertexAI(any(String.class)))
                        .thenReturn(Optional.of("responsePart1-responsePart2-responsePart3"));

                delegate.execute(execution);

                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                verify(execution, times(3)).setVariable(anyString(), captor.capture());

                List<String> allValues = captor.getAllValues();
                assertEquals("responsePart1", allValues.get(0));
                assertEquals("responsePart2", allValues.get(1));
                assertEquals("responsePart3", allValues.get(2));

            } else {
                assertThrows(IllegalArgumentException.class, () -> delegate.execute(execution));
            }
        }
    }

    private static Stream<Arguments> extensionPropertyTestCases() {
        return Stream.of(
                Arguments.of(false, false, false),
                Arguments.of(false, false, true),
                Arguments.of(false, true, false),
                Arguments.of(false, true, true),
                Arguments.of(true, false, false),
                Arguments.of(true, false, true),
                Arguments.of(true, true, false),
                Arguments.of(true, true, true));
    }

    private void extensionPropertiesStubs(
            MockedStatic<CamundaPropertiesUtils> mockedStatic,
            boolean promptPrefixIsEmpty,
            boolean promptSuffixPathIsEmpty,
            boolean resultsParsePatternIsEmpty) {
        mockedStatic
                .when(
                        () ->
                                CamundaPropertiesUtils.getExtensionProperty(
                                        "vertexai.promptPrefix", execution))
                .thenReturn(promptPrefixIsEmpty ? Optional.empty() : Optional.of("promptPrefix"));
        mockedStatic
                .when(
                        () ->
                                CamundaPropertiesUtils.getExtensionProperty(
                                        "vertexai.promptSuffixPath", execution))
                .thenReturn(promptSuffixPathIsEmpty ? Optional.empty() : Optional.of("var1"));
        mockedStatic
                .when(
                        () ->
                                CamundaPropertiesUtils.getExtensionProperty(
                                        "vertexai.resultsParsePattern", execution))
                .thenReturn(
                        resultsParsePatternIsEmpty
                                ? Optional.empty()
                                : Optional.of("^([^-\\n]+)-([^-\\n]+)-([^-\\n]+)$"));

        mockedStatic
                .when(
                        () ->
                                CamundaPropertiesUtils.getExtensionPropertiesWithPrefix(
                                        "vertexai.resultVariableName.", execution))
                .thenReturn(
                        new HashMap<>() {
                            {
                                put("0", "var1");
                                put("1", "var2");
                                put("2", "var3");
                            }
                        });
    }

    Schema getSchema() {
        DynaProperty var1 = new DynaProperty("var1", String.class);
        DynaProperty var2 = new DynaProperty("var2", String.class);
        DynaProperty var3 = new DynaProperty("var3", String.class);

        return Schema.builder().id(UUID.randomUUID()).properties(List.of(var1, var2, var3)).build();
    }
}
