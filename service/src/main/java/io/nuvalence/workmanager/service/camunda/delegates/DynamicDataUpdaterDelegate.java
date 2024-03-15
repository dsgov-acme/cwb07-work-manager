package io.nuvalence.workmanager.service.camunda.delegates;

import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
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
 * Service layer to manage transaction dynamic data update sequence flow.
 */
@Slf4j
@RequiredArgsConstructor
@Component("dynamicDataUpdaterDelegate")
@Profile("!test")
public class DynamicDataUpdaterDelegate implements JavaDelegate {

    private static final String TRANSACTION_ID_KEY = "transactionId";

    private final TransactionService transactionService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable(TRANSACTION_ID_KEY);

        if (transactionId == null) {
            log.warn("DynamicDataUpdaterDelegate - transactionId not found");
            return;
        }

        Optional<Transaction> transactionOptional =
                transactionService.getTransactionById(transactionId);
        if (transactionOptional.isPresent()) {
            Transaction transaction = transactionOptional.get();

            Map<String, String> data = (Map) execution.getVariable("data");

            replaceDataValues(data, transaction.getData());
            transactionService.updateTransaction(transaction);
        }
    }

    public void replaceDataValues(Map<String, String> propertyMap, DynamicEntity transactionData) {
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            transactionData.set(key, value);
        }
    }
}
