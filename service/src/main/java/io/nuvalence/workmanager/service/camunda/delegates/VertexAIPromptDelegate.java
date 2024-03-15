package io.nuvalence.workmanager.service.camunda.delegates;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.service.vertexai.VertexAIClient;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service layer to manage vertex AI prompt sequence flow.
 */
@Slf4j
@RequiredArgsConstructor
@Component("vertexAIPromptDelegate")
@Profile("!test")
public class VertexAIPromptDelegate implements JavaDelegate {

    private static final String TRANSACTION_ID_KEY = "transactionId";
    public static final String PROMPT_PREFIX_EXTENSION_PROPERTY = "vertexai.promptPrefix";
    public static final String PROMPT_SUFFIX_PATH_EXTENSION_PROPERTY = "vertexai.promptSuffixPath";
    public static final String RESULTS_PARSE_PATTERN_EXTENSION_PROPERTY =
            "vertexai.resultsParsePattern";
    public static final String RESULT_VARIABLE_NAME_EXTENSION_PROPERTY =
            "vertexai.resultVariableName.";

    private final TransactionService transactionService;
    private final VertexAIClient vertexAIClient;

    private String promptPrefix;
    private String promptSuffixPath;
    private String resultsParsePattern;
    private Map<String, String> resultPartsNames;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable(TRANSACTION_ID_KEY);

        if (transactionId == null) {
            throw new IllegalArgumentException("TransactionId execution variable not found");
        }

        getExtensionProperties(execution);

        Optional<Transaction> transactionOptional =
                transactionService.getTransactionById(transactionId);
        if (transactionOptional.isPresent()) {
            Transaction transaction = transactionOptional.get();

            String promptSuffix = (String) transaction.getData().get(promptSuffixPath);
            String prompt = String.join("", promptPrefix, promptSuffix);

            String vertexAIResponse = vertexAIClient.askVertexAI(prompt).orElse("");
            sendExecutionVariable(execution, vertexAIResponse);
        }
    }

    private void sendExecutionVariable(DelegateExecution execution, String vertexAIResponse) {
        Pattern pattern = Pattern.compile(resultsParsePattern);
        Matcher matcher = pattern.matcher(vertexAIResponse);

        Map<String, String> vertexAIResponseMap = new HashMap<>();
        if (matcher.find()) {
            for (Map.Entry<String, String> entry : resultPartsNames.entrySet()) {
                String variableName = entry.getValue();
                int variableNamePosition = Integer.parseInt(entry.getKey());
                vertexAIResponseMap.put(variableName, matcher.group(variableNamePosition + 1));
                execution.setVariable(
                        variableName, matcher.group(variableNamePosition + 1).strip());
            }
        }
    }

    private void getExtensionProperties(DelegateExecution execution) {
        this.promptPrefix = getExtensionProperty(PROMPT_PREFIX_EXTENSION_PROPERTY, execution);
        this.promptSuffixPath =
                getExtensionProperty(PROMPT_SUFFIX_PATH_EXTENSION_PROPERTY, execution);
        this.resultsParsePattern =
                getExtensionProperty(RESULTS_PARSE_PATTERN_EXTENSION_PROPERTY, execution);
        this.resultPartsNames =
                CamundaPropertiesUtils.getExtensionPropertiesWithPrefix(
                        RESULT_VARIABLE_NAME_EXTENSION_PROPERTY, execution);
    }

    private String getExtensionProperty(String propertyName, DelegateExecution execution) {
        return CamundaPropertiesUtils.getExtensionProperty(propertyName, execution)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        propertyName + " extension property not found"));
    }
}
