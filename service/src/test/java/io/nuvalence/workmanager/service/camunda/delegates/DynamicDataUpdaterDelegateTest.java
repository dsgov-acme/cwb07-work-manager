package io.nuvalence.workmanager.service.camunda.delegates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import org.apache.commons.beanutils.DynaProperty;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class DynamicDataUpdaterDelegateTest {

    @Mock private TransactionService transactionService;

    @Mock private DelegateExecution execution;

    @Captor private ArgumentCaptor<Transaction> transactionCaptor;

    private DynamicDataUpdaterDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new DynamicDataUpdaterDelegate(transactionService);
    }

    @Test
    void testExecute_happyPath() throws Exception {
        // Setup
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = createTransaction();

        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        Map<String, String> data = Map.of("key1", "newValue1", "key2", "newValue2");
        when(execution.getVariable("data")).thenReturn(data);

        // Execute
        delegate.execute(execution);

        // Verify
        verify(transactionService).updateTransaction(transactionCaptor.capture());
        Transaction updated = transactionCaptor.getValue();
        assertEquals("newValue1", updated.getData().get("key1"));
        assertEquals("newValue2", updated.getData().get("key2"));
    }

    @Test
    void testExecute_missingTransactionId() throws Exception {
        delegate.execute(execution);

        verify(transactionService, never()).updateTransaction(any());
    }

    @Test
    void testExecute_transactionNotFound() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

        delegate.execute(execution);

        verify(transactionService, never()).updateTransaction(any());
    }

    private Transaction createTransaction() {
        UUID transactionId = UUID.randomUUID();
        return Transaction.builder()
                .id(transactionId)
                .data(new DynamicEntity(getSchema()))
                .subjectProfileId(UUID.randomUUID())
                .build();
    }

    Schema getSchema() {
        DynaProperty key1 = new DynaProperty("key1", String.class);
        DynaProperty key2 = new DynaProperty("key2", String.class);
        DynaProperty key3 = new DynaProperty("key3", String.class);
        return Schema.builder().id(UUID.randomUUID()).properties(List.of(key1, key2, key3)).build();
    }
}
