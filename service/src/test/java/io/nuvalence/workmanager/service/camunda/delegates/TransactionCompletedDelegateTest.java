package io.nuvalence.workmanager.service.camunda.delegates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionCompletedDelegateTest {

    @Mock private DelegateExecution execution;

    @Mock private TransactionService transactionService;

    @InjectMocks private TransactionCompletedDelegate delegate;

    @Mock private RequestContextTimestamp requestContextTimestamp;

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void testExecute_HappyPath(boolean undoCompleted) throws Exception {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();

        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        if (!undoCompleted) {
            when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(OffsetDateTime.now());
        }

        try (MockedStatic<CamundaPropertiesUtils> mockedStatic =
                mockStatic(CamundaPropertiesUtils.class)) {

            mockedStatic
                    .when(
                            () ->
                                    CamundaPropertiesUtils.getExtensionProperty(
                                            "undoCompleted", execution))
                    .thenReturn(undoCompleted ? Optional.of("true") : Optional.empty());

            delegate.execute(execution);
        }

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionService).getTransactionById(transactionId);
        verify(transactionService).updateTransaction(transactionCaptor.capture());
        if (undoCompleted) {
            assertFalse(transactionCaptor.getValue().getIsCompleted());
            assertNull(transactionCaptor.getValue().getCompletedOn());
        } else {
            assertTrue(transactionCaptor.getValue().getIsCompleted());
            assertNotNull(transactionCaptor.getValue().getCompletedOn());
        }
    }

    @Test
    void testExecute_NullTransactionId() throws Exception {
        when(execution.getVariable("transactionId")).thenReturn(null);

        delegate.execute(execution);

        verifyNoInteractions(transactionService);
    }
}
