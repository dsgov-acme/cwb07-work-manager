package io.nuvalence.workmanager.service.utils.camunda;

import static java.util.stream.Collectors.groupingBy;

import com.google.common.collect.Sets;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import jakarta.transaction.Transactional;

/**
 * Checks for consistency between Camunda process definitions and transaction/form config definitions within WM.
 */
@SuppressWarnings("checkstyle:CyclomaticComplexity")
@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ConsistencyChecker {
    private final TransactionDefinitionRepository transactionDefinitionRepository;
    private final ProcessEngine processEngine;
    private final GenericApplicationContext applicationContext;

    /**
     * Runs the consistency check.
     *
     * @return A list of consistency issues that have been found.
     */
    public List<String> check() {
        List<String> issues = new ArrayList<>();
        Map<TransactionDefinition, ProcessDefinition> validProcessDefinitions = new HashMap<>();

        // get all transaction definitions
        // TODO: update getAllDefinitions to use temporal (latest) call
        Map<String, List<TransactionDefinition>> transactionDefinitions =
                transactionDefinitionRepository.findAll().stream()
                        .collect(groupingBy(TransactionDefinition::getProcessDefinitionKey));
        Map<String, ProcessDefinition> processDefinitions =
                processEngine
                        .getRepositoryService()
                        .createProcessDefinitionQuery()
                        .latestVersion()
                        .list()
                        .stream()
                        .collect(Collectors.toMap(ProcessDefinition::getKey, pd -> pd));
        Set<String> applicationDelegateNames =
                this.applicationContext.getBeansOfType(JavaDelegate.class).values().stream()
                        .filter(d -> d.getClass().isAnnotationPresent(Named.class))
                        .map(d -> d.getClass().getAnnotation(Named.class).value())
                        .collect(Collectors.toSet());
        Set<String> usedDelegateNames = new HashSet<>();

        // check if transaction_definition records map to a valid process definition
        for (String processDefinitionKey :
                Sets.difference(transactionDefinitions.keySet(), processDefinitions.keySet())) {
            issues.add(
                    String.format(
                            "No Camunda process definition defined with key: '%s'.",
                            processDefinitionKey));
        }

        // the inverse of the above (checking the all process definitions have a map to a
        // transaction_definition record)
        for (String processDefinitionKey :
                Sets.difference(processDefinitions.keySet(), transactionDefinitions.keySet())) {
            issues.add(
                    String.format(
                            "No existing transaction definition has been declared for process"
                                    + " definition with key: '%s'.",
                            processDefinitionKey));
        }

        // the valid process definitions are those within camunda and also having a backing record
        // in our database
        Sets.intersection(transactionDefinitions.keySet(), processDefinitions.keySet())
                .forEach(
                        pd ->
                                transactionDefinitions
                                        .get(pd)
                                        .forEach(
                                                td ->
                                                        validProcessDefinitions.put(
                                                                td, processDefinitions.get(pd))));

        // iterate through all valid transaction definitions and ensure the tasks that are mapped
        // exist in camunda
        for (Map.Entry<TransactionDefinition, ProcessDefinition> entry :
                validProcessDefinitions.entrySet()) {
            BpmnModelInstance bpmnModelInstance =
                    processEngine
                            .getRepositoryService()
                            .getBpmnModelInstance(entry.getValue().getId());

            Set<String> camundaDelegateNames = new HashSet<>();
            camundaDelegateNames.addAll(
                    getDelegateNameListFromModelByType(
                            bpmnModelInstance,
                            ServiceTask.class,
                            ServiceTask::getCamundaDelegateExpression));
            camundaDelegateNames.addAll(
                    getDelegateNameListFromModelByType(
                            bpmnModelInstance,
                            SendTask.class,
                            SendTask::getCamundaDelegateExpression));
            camundaDelegateNames.addAll(
                    getDelegateNameListFromModelByType(
                            bpmnModelInstance,
                            BusinessRuleTask.class,
                            BusinessRuleTask::getCamundaDelegateExpression));
            camundaDelegateNames.addAll(
                    getDelegateNameListFromModelByType(
                            bpmnModelInstance,
                            MessageEventDefinition.class,
                            MessageEventDefinition::getCamundaDelegateExpression));
            camundaDelegateNames.addAll(
                    getDelegateNameListFromModelByType(
                            bpmnModelInstance,
                            CamundaExecutionListener.class,
                            CamundaExecutionListener::getCamundaDelegateExpression));

            for (String delegateName :
                    Sets.difference(camundaDelegateNames, applicationDelegateNames)) {
                issues.add(
                        String.format(
                                "A delegate expression named '%s' was declared in '%s' but no"
                                        + " matching JavaDelegate class with a @Named attribute has"
                                        + " been found.",
                                delegateName, entry.getValue().getResourceName()));
            }

            usedDelegateNames.addAll(
                    Sets.intersection(camundaDelegateNames, applicationDelegateNames));
        }

        for (String delegateName : Sets.difference(applicationDelegateNames, usedDelegateNames)) {
            issues.add(
                    String.format(
                            "A delegate exists in the application named '%s' that is not in use"
                                    + " within Camunda.",
                            delegateName));
        }

        return issues;
    }

    private <T extends ModelElementInstance> List<String> getDelegateNameListFromModelByType(
            BpmnModelInstance bpmnModelInstance,
            Class<T> typeClass,
            Function<T, String> delegateExpressionFunction) {
        return bpmnModelInstance.getModelElementsByType(typeClass).stream()
                .filter(t -> StringUtils.isNotEmpty(delegateExpressionFunction.apply(t)))
                .map(t -> getDelegateNameFromExpression(delegateExpressionFunction.apply(t)))
                .toList();
    }

    private String getDelegateNameFromExpression(String delegateExpression) {
        if (StringUtils.isEmpty(delegateExpression)) {
            return delegateExpression;
        }

        // Use concise character class syntax '\\W' instead of '[^a-zA-Z0-9_]'
        // This will match any non-word character (equivalent to [^a-zA-Z0-9_])
        return delegateExpression.replaceAll("\\W", "");
    }
}
