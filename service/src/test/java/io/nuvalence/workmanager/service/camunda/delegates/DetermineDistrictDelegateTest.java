package io.nuvalence.workmanager.service.camunda.delegates;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DetermineDistrictDelegateTest {
    @Mock private TransactionService service;

    @InjectMocks private DetermineDistrictDelegate delegate;

    @Test
    void executeIsValidWithValidTransaction() throws Exception {
        // Arrange
        final Transaction transaction = Transaction.builder().district("DISTRICT8").build();
        DelegateExecution execution = mock(DelegateExecution.class);
        when(service.getTransactionByProcessInstanceId(transaction.getProcessInstanceId()))
                .thenReturn(Optional.of(transaction));
        when(execution.getVariable("district")).thenReturn("DISTRICT1");

        // Act
        delegate.execute(execution);

        // Assert
        verify(service).updateTransaction(transaction);
    }

    @Test
    void executeDoesNotUpdateIfTransactionIsNotFound() throws Exception {
        // Arrange
        final Transaction transaction = Transaction.builder().district("DISTRICT8").build();
        DelegateExecution execution = mock(DelegateExecution.class);
        when(service.getTransactionByProcessInstanceId(transaction.getProcessInstanceId()))
                .thenReturn(Optional.empty());

        // Act
        delegate.execute(execution);

        // Assert
        verify(service, never()).updateTransaction(transaction);
    }

    @Test
    void executeDidNotUpdateIfDistrictIsSame() throws Exception {
        // Arrange
        final Transaction transaction = Transaction.builder().district("DISTRICT1").build();
        DelegateExecution execution = mock(DelegateExecution.class);
        when(service.getTransactionByProcessInstanceId(transaction.getProcessInstanceId()))
                .thenReturn(Optional.of(transaction));
        when(execution.getVariable("district")).thenReturn("DISTRICT1");

        // Act
        delegate.execute(execution);

        // Assert
        verify(service, never()).updateTransaction(transaction);
    }
}
