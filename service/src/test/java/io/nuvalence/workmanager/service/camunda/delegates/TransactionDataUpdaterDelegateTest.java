package io.nuvalence.workmanager.service.camunda.delegates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import org.apache.commons.beanutils.DynaProperty;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionDataUpdaterDelegateTest {

    @InjectMocks private TransactionDataUpdaterDelegate transactionDataUpdaterDelegate;

    @Mock private TransactionService transactionService;

    @Mock private DelegateExecution delegateExecution;

    @Captor private ArgumentCaptor<Transaction> transactionCaptor;

    private UUID transactionId;

    @BeforeEach
    public void setup() {
        transactionId = UUID.randomUUID();
    }

    @Test
    void testExecute_transactionIdIsNull() {
        when(delegateExecution.getVariable("transactionId")).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionDataUpdaterDelegate.execute(delegateExecution));
    }

    @Test
    void testExecute_transactionIdIsNotNull_transactionNotFound() {
        when(delegateExecution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

        try (MockedStatic<CamundaPropertiesUtils> mockedStatic =
                mockStatic(CamundaPropertiesUtils.class)) {
            extensionPropertiesStubs(mockedStatic);
            transactionDataUpdaterDelegate.execute(delegateExecution);

            verify(transactionService, times(1)).getTransactionById(transactionId);
        }
    }

    @Test
    void testExecute_success() {

        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .data(new DynamicEntity(getSchema()))
                        .subjectProfileId(UUID.randomUUID())
                        .build();

        when(delegateExecution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        try (MockedStatic<CamundaPropertiesUtils> mockedStatic =
                mockStatic(CamundaPropertiesUtils.class)) {
            extensionPropertiesStubs(mockedStatic);
            executionPropertiesStubs();
            transactionDataUpdaterDelegate.execute(delegateExecution);

            verify(transactionService, times(1)).getTransactionById(transactionId);
            verify(transactionService, times(1)).updateTransaction(transactionCaptor.capture());

            Transaction updatedTransaction = transactionCaptor.getValue();

            assertEquals("value1", updatedTransaction.getData().get("var1"));
            assertEquals("value2", updatedTransaction.getData().get("var2"));
            assertEquals("value3", updatedTransaction.getData().get("var3"));
            assertEquals("Draft", updatedTransaction.getStatus());
            assertEquals(TransactionPriority.HIGH, updatedTransaction.getPriority());
        }
    }

    private void extensionPropertiesStubs(MockedStatic<CamundaPropertiesUtils> mockedStatic) {
        mockedStatic
                .when(
                        () ->
                                CamundaPropertiesUtils.getExtensionPropertiesWithPrefix(
                                        "transactionDataUpdate.mapping.", delegateExecution))
                .thenReturn(
                        new HashMap<>() {
                            {
                                put("var1", "data.var1");
                                put("var2", "data.var2");
                                put("var3", "data.var3");
                                put("priority", "priority");
                                put("status", "status");
                            }
                        });
    }

    private void executionPropertiesStubs() {
        when(delegateExecution.getVariable("var1")).thenReturn("value1");
        when(delegateExecution.getVariable("var2")).thenReturn("value2");
        when(delegateExecution.getVariable("var3")).thenReturn("value3");
        when(delegateExecution.getVariable("priority")).thenReturn("HIGH");
        when(delegateExecution.getVariable("status")).thenReturn("Draft");
    }

    Schema getSchema() {
        DynaProperty var1 = new DynaProperty("var1", String.class);
        DynaProperty var2 = new DynaProperty("var2", String.class);
        DynaProperty var3 = new DynaProperty("var3", String.class);

        return Schema.builder().id(UUID.randomUUID()).properties(List.of(var1, var2, var3)).build();
    }
}
