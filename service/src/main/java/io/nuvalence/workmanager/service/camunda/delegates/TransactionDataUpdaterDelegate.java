package io.nuvalence.workmanager.service.camunda.delegates;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer to manage transaction priority update sequence flow.
 */
@Slf4j
@RequiredArgsConstructor
@Component("transactionDataUpdaterDelegate")
@Profile("!test")
public class TransactionDataUpdaterDelegate implements JavaDelegate {

    private static final String TRANSACTION_ID_KEY = "transactionId";

    private final TransactionService transactionService;

    private Map<String, String> variablePathsMap;

    @Override
    public void execute(DelegateExecution execution) {
        UUID transactionId = (UUID) execution.getVariable(TRANSACTION_ID_KEY);

        if (transactionId == null) {
            throw new IllegalArgumentException("TransactionId execution variable not found");
        }

        this.variablePathsMap =
                getExtensionPropertiesWithPrefix("transactionDataUpdate.mapping.", execution);

        transactionService
                .getTransactionById(transactionId)
                .ifPresent(
                        transaction -> {
                            for (String key : variablePathsMap.keySet()) {
                                try {
                                    setAttributeValue(transaction, key, execution.getVariable(key));
                                } catch (Exception e) {
                                    log.error(
                                            "TransactionDataUpdaterDelegate - Error setting"
                                                    + " variable value",
                                            e);
                                }
                            }

                            transactionService.updateTransaction(transaction);
                        });
    }

    private void setAttributeValue(Transaction transaction, String key, Object value)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        String attributePath = variablePathsMap.get(key);

        value = validateValueType(transaction, value, attributePath);
        BeanUtils.setProperty(transaction, attributePath, value);
    }

    private Object validateValueType(Transaction transaction, Object value, String attributePath)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        PropertyDescriptor descriptor =
                PropertyUtils.getPropertyDescriptor(transaction, attributePath);
        if (descriptor != null) {
            Class<?> propertyType = descriptor.getPropertyType();
            if (propertyType.isEnum() && value instanceof String valueString) {
                value = Enum.valueOf((Class<Enum>) propertyType, valueString);
            }
        }
        return value;
    }

    private Map<String, String> getExtensionPropertiesWithPrefix(
            String prefix, DelegateExecution execution) {
        return CamundaPropertiesUtils.getExtensionPropertiesWithPrefix(prefix, execution);
    }
}
