package io.nuvalence.workmanager.service.camunda.delegates;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionSubmitDelegateTest {

    private TransactionService transactionService;
    private DelegateExecution execution;
    private TransactionSubmitDelegate service;
    private RequestContextTimestamp requestContextTimestamp;

    @BeforeEach
    void setUp() {
        // mocks
        transactionService = mock(TransactionService.class);
        execution = mock(DelegateExecution.class);
        requestContextTimestamp = mock(RequestContextTimestamp.class);

        // init service
        service = new TransactionSubmitDelegate(transactionService, requestContextTimestamp);
    }

    @Test
    void testExecute() throws Exception {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder().id(transactionId).build();

        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        OffsetDateTime contextTimestamp = OffsetDateTime.now();
        when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(contextTimestamp);

        service.execute(execution);

        OffsetDateTime submitted = transaction.getSubmittedOn();

        assert submitted != null;
        assert submitted.isEqual(contextTimestamp);

        verify(transactionService, times(1)).getTransactionById(transactionId);
        verify(transactionService, times(1)).updateTransaction(transaction);
        verifyNoMoreInteractions(transactionService);
    }

    @Test
    void testNoTransaction() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

        service.execute(execution);

        verify(transactionService, times(1)).getTransactionById(transactionId);
        verify(transactionService, never()).updateTransaction(any());
        verifyNoMoreInteractions(transactionService);
    }

    @Test
    void testNullId() throws Exception {

        when(execution.getVariable("transactionId")).thenReturn(null);

        service.execute(execution);

        verifyNoInteractions(transactionService);
    }
}
