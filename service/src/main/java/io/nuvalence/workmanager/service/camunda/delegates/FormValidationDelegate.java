package io.nuvalence.workmanager.service.camunda.delegates;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.FormConfigurationService;
import io.nuvalence.workmanager.service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs all form validations.
 */
@RequiredArgsConstructor
@Component("formValidationDelegate")
@Profile("!test")
@Slf4j
public class FormValidationDelegate implements JavaDelegate {
    private final TransactionService transactionService;

    private final FormConfigurationService formConfigurationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable("transactionId");
        Optional<Transaction> optionalTransaction =
                transactionService.getTransactionById(transactionId);
        if (optionalTransaction.isEmpty()) {
            log.warn(
                    "Transaction {} not found, could not run full transaction validation.",
                    transactionId);
            return;
        }
        Transaction transaction = optionalTransaction.get();

        String defaultFormConfigurationKey =
                transaction.getTransactionDefinition().getDefaultFormConfigurationKey();

        String transactionDefinitionKey = transaction.getTransactionDefinition().getKey();
        Optional<FormConfiguration> formConfigurationOptional =
                formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, defaultFormConfigurationKey);
        if (formConfigurationOptional.isEmpty()) {
            log.warn(
                    "Form configuration with key {} not found, could not run full transaction"
                            + " validation.",
                    defaultFormConfigurationKey);
            return;
        }
        FormConfiguration formConfiguration = formConfigurationOptional.get();

        ArrayList<Map<String, Object>> configurationComponents;
        try {
            Map<String, Object> config = formConfiguration.getConfiguration();
            if (config != null && config.get("components") != null) {
                configurationComponents =
                        ((ArrayList<Map<String, Object>>) config.get("components"));
            } else {
                log.warn("Components or configuration is null.");
                throw new BusinessLogicException(
                        "Components could not be obtained for the provided form configuration.");
            }
        } catch (ClassCastException e) {
            log.warn("An exception occurred obtaining configuration components {}", e.getMessage());
            throw new BusinessLogicException(
                    "An unexpected component structure was found for the provided form"
                            + " configuration.");
        }

        List<NuvalenceFormioValidationExItem> formioErrors = new ArrayList<>();
        configurationComponents.stream()
                .forEach(
                        component -> {
                            try {
                                transactionService.validateFormStep(
                                        (String) component.get("key"),
                                        transactionDefinitionKey,
                                        transaction,
                                        execution.getCurrentActivityName(),
                                        "");
                            } catch (
                                    NuvalenceFormioValidationException
                                            nuvalenceFormioValidationException) {
                                formioErrors.addAll(
                                        nuvalenceFormioValidationException
                                                .getFormioValidationErrors()
                                                .getFormioValidationErrors());
                            }
                        });

        if (!formioErrors.isEmpty()) {
            NuvalenceFormioValidationExMessage formioValidationExMessage =
                    NuvalenceFormioValidationExMessage.builder()
                            .formioValidationErrors(formioErrors)
                            .build();

            throw new NuvalenceFormioValidationException(formioValidationExMessage);
        }
    }
}
