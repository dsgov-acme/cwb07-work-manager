package io.nuvalence.workmanager.service.camunda.delegates;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.SendNotificationService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Delegate to send notifications when transaction milestones are hit.
 */
@RequiredArgsConstructor
@Component("transactionNotificationDelegate")
@Profile("!test")
@Slf4j
public class TransactionNotificationDelegate implements JavaDelegate {

    private final TransactionService transactionService;

    private final SendNotificationService sendNotificationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable("transactionId");
        Optional<Transaction> optionalTransaction =
                transactionService.getTransactionById(transactionId);
        if (optionalTransaction.isEmpty()) {
            log.warn(
                    "Transaction {} was not found and notification could not be sent.",
                    transactionId);
            return;
        }
        Transaction transaction = optionalTransaction.get();

        Optional<String> optionalNotificationKey =
                CamundaPropertiesUtils.getExtensionProperty("notification.key", execution);
        if (optionalNotificationKey.isEmpty()) {
            log.warn(
                    "Camunda properties are missing, could not send notification for {}",
                    transactionId);
            return;
        }
        Map<String, String> propertiesWithPrefix =
                CamundaPropertiesUtils.getExtensionPropertiesWithPrefix(
                        "notification.parameter.", execution);

        log.debug(
                "Sending notification for transaction {} with key {} ",
                transactionId,
                optionalNotificationKey.get());

        sendNotificationService.sendTransactionNotification(
                transaction, optionalNotificationKey.get(), propertiesWithPrefix);
    }
}
