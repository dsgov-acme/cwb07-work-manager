package io.nuvalence.workmanager.service.utils.camunda;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Cache to store workflow inspectors.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CamundaWorkflowInspectorCache {
    private final ProcessEngine processEngine;
    private final LoadingCache<String, CamundaWorkflowInspector> cache =
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .build(
                            new CacheLoader<>() {
                                @Override
                                public @NonNull CamundaWorkflowInspector load(
                                        @NonNull String processDefinitionId) {
                                    log.debug(
                                            "Loading workflow inspector for process definition ID:"
                                                    + " {}",
                                            processDefinitionId);
                                    final ProcessDefinition processDefinition =
                                            processEngine
                                                    .getRepositoryService()
                                                    .getProcessDefinition(processDefinitionId);
                                    return new CamundaWorkflowInspector(
                                            Bpmn.readModelFromStream(
                                                    processEngine
                                                            .getRepositoryService()
                                                            .getResourceAsStream(
                                                                    processDefinition
                                                                            .getDeploymentId(),
                                                                    processDefinition
                                                                            .getResourceName())));
                                }
                            });

    /**
     * Get the workflow inspector for the process definition by ID instead of by key.
     *
     * @param processDefinitionId ID of process definition
     * @return workflow inspector
     */
    public CamundaWorkflowInspector getByProcessDefinitionId(String processDefinitionId) {
        // Process definition IDs are not always a single UUID, those can be composed strings. So we
        // aren't to use UUID here.
        return cache.getUnchecked(processDefinitionId);
    }

    /**
     * Get the workflow inspector for the process instance.
     *
     * @param processInstanceId ID of process instance
     * @return workflow inspector
     */
    public CamundaWorkflowInspector getByProcessInstanceId(String processInstanceId) {
        final String processDefinitionId =
                getProcessInstance(processInstanceId)
                        .map(ProcessInstance::getProcessDefinitionId)
                        .or(
                                () ->
                                        getHistoricProcessInstance(processInstanceId)
                                                .map(
                                                        HistoricProcessInstance
                                                                ::getProcessDefinitionId))
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No process definition found for process instance"
                                                        + " ID: "
                                                        + processInstanceId));
        return getByProcessDefinitionId(processDefinitionId);
    }

    /**
     * Get the workflow inspector for the process definition by process definition key.
     *
     * @param processDefinitionKey Key of process definition
     * @return workflow inspector
     */
    public CamundaWorkflowInspector getByProcessDefinitionKey(String processDefinitionKey) {
        final String processDefinitionId =
                Optional.ofNullable(
                                processEngine
                                        .getRepositoryService()
                                        .createProcessDefinitionQuery()
                                        .processDefinitionKey(processDefinitionKey)
                                        .latestVersion()
                                        .singleResult())
                        .map(ProcessDefinition::getId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No process definition found for process definition"
                                                        + " key: "
                                                        + processDefinitionKey));

        return getByProcessDefinitionId(processDefinitionId);
    }

    private Optional<ProcessInstance> getProcessInstance(String processInstanceId) {
        return Optional.ofNullable(
                processEngine
                        .getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult());
    }

    private Optional<HistoricProcessInstance> getHistoricProcessInstance(String processInstanceId) {
        return Optional.ofNullable(
                processEngine
                        .getHistoryService()
                        .createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult());
    }
}
