package io.nuvalence.workmanager.service.jobs;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Job to update transactions that are completed.
 */
@ExtendWith(MockitoExtension.class)
class TransactionCompletedUpdaterJobTest {

    @Mock private TransactionService transactionService;

    @InjectMocks private TransactionCompletedUpdaterJob job;

    @Test
    void testRun() {
        when(transactionService.updateCompletedTransactions()).thenReturn(5);
        job.run();
        verify(transactionService, times(1)).updateCompletedTransactions();
    }

    @Test
    void testInit() {
        when(transactionService.updateCompletedTransactions()).thenReturn(5);
        job.run();
        verify(transactionService, times(1)).updateCompletedTransactions();
    }
}
