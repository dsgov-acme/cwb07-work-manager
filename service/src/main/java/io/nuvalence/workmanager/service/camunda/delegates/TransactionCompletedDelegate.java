package io.nuvalence.workmanager.service.camunda.delegates;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Service layer to set transaction completion or undo transaction completion.
 */
@Slf4j
@RequiredArgsConstructor
@Component("transactionCompletedDelegate")
@Profile("!test")
public class TransactionCompletedDelegate implements JavaDelegate {

    private final TransactionService transactionService;
    private final RequestContextTimestamp requestContextTimestamp;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable("transactionId");

        if (transactionId == null) {
            log.warn("TransactionCompletedDelegate - transactionId not found");
            return;
        }

        boolean undoCompleted =
                Boolean.parseBoolean(
                        CamundaPropertiesUtils.getExtensionProperty("undoCompleted", execution)
                                .orElse(String.valueOf(false)));
        markTransactionAsCompleted(transactionId, undoCompleted);
    }

    @SuppressWarnings("PMD.NullAssignment")
    private void markTransactionAsCompleted(UUID transactionId, boolean undoCompleted) {
        Optional<Transaction> transactionOptional =
                transactionService.getTransactionById(transactionId);
        if (transactionOptional.isPresent()) {
            Transaction transaction = transactionOptional.get();
            transaction.setIsCompleted(undoCompleted ? false : true);
            transaction.setCompletedOn(
                    undoCompleted ? null : requestContextTimestamp.getCurrentTimestamp());
            transactionService.updateTransaction(transaction);
        }
    }
}
