package io.nuvalence.workmanager.service.camunda.delegates;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
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
 * Service layer to manage transaction status updates.
 */
@Slf4j
@RequiredArgsConstructor
@Component("transactionStatusUpdateDelegate")
@Profile("!test")
public class TransactionStatusUpdateDelegate implements JavaDelegate {

    private final TransactionService transactionService;
    private String defaultStatus = "Draft";

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable("transactionId");

        if (transactionId == null) {
            log.warn("TransactionStatusUpdateDelegate - transactionId not found");
            return;
        }

        String status = defaultStatus;
        Optional<String> statusOptional =
                CamundaPropertiesUtils.getExtensionProperty("status", execution);
        if (statusOptional.isPresent()) {
            status = statusOptional.get();
        } else {
            log.warn(
                    "TransactionStatusUpdateDelegate - status not found, the default status"
                            + " will be set: {}",
                    defaultStatus);
        }
        updateTransactionStatus(transactionId, status, execution);
    }

    private void updateTransactionStatus(
            UUID transactionId, String status, DelegateExecution execution) {
        Optional<Transaction> transactionOptional =
                transactionService.getTransactionById(transactionId);
        if (transactionOptional.isPresent()) {
            Transaction transaction = transactionOptional.get();
            transaction.setStatus(status);
            transaction.setProcessInstanceId(execution.getProcessInstanceId());
            transactionService.updateTransaction(transaction);
        }
    }
}
