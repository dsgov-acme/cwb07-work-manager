package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Service layer to manage task retrieval.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class WorkflowTasksService {
    private final ProcessEngine processEngine;
    private final TransactionDefinitionService transactionDefinitionService;

    /**
     * Gets all statuses from workflow process definitions.
     * Defined as Camunda extension properties.
     *
     * @param type     public or internal, defaults to public
     * @param category optional param to search by definition category
     * @param keys     optional param to search by definition key
     * @return List of available statuses
     **/
    public List<String> getCamundaStatuses(final String type, String category, List<String> keys) {
        // default to public
        StatusType statusType = StatusType.PUBLIC;
        try {
            statusType = StatusType.valueOf(type.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            log.warn(
                    "Provided status search type {} is not enum value, defaulting to public.",
                    type);
        }
        List<ProcessDefinition> processDefinitions =
                createProcessDefinitionSearchAndRetrieve(category, keys);

        // use Set remove duplicates, i.e. maybe 2 statuses map to 1 public status
        LinkedHashSet<String> distinctStatuses = new LinkedHashSet<>();
        for (ProcessDefinition definition : processDefinitions) {
            try {
                BpmnModelInstance modelInstance =
                        Bpmn.readModelFromStream(new FileInputStream(definition.getResourceName()));
                Collection<CamundaProperty> properties =
                        modelInstance.getModelElementsByType(CamundaProperty.class);
                for (CamundaProperty property : properties) {
                    if (property.getAttributeValue("name").equals(statusType.propertyName)) {
                        distinctStatuses.add(property.getCamundaValue());
                    }
                }
            } catch (FileNotFoundException e) {
                log.warn(
                        "error parsing bpmn file {} for workflow statuses",
                        definition.getResourceName(),
                        e);
            }
        }
        return new ArrayList<>(distinctStatuses);
    }

    /**
     * Gets a map of public to internal statuses via Camunda extension properties.
     *
     * @param category optional param to search by definition category
     * @param keys     optional param to search by definition key
     * @return a map of public to internal statuses
     */
    public Map<String, List<String>> getStatusMap(String category, List<String> keys) {
        List<ProcessDefinition> processDefinitions =
                createProcessDefinitionSearchAndRetrieve(category, keys);
        Map<String, List<String>> statusMap = new HashMap<>();

        for (ProcessDefinition definition : processDefinitions) {
            processDefinitionAndAddStatus(definition, statusMap);
        }

        return statusMap;
    }

    private void processDefinitionAndAddStatus(
            ProcessDefinition definition, Map<String, List<String>> statusMap) {
        try {
            BpmnModelInstance modelInstance =
                    Bpmn.readModelFromStream(new FileInputStream(definition.getResourceName()));
            Collection<CamundaProperty> properties =
                    modelInstance.getModelElementsByType(CamundaProperty.class);

            for (CamundaProperty property : properties) {
                processCamundaProperty(property, statusMap);
            }
        } catch (FileNotFoundException e) {
            log.warn(
                    "error parsing bpmn file {} for workflow statuses",
                    definition.getResourceName(),
                    e);
        }
    }

    private void processCamundaProperty(
            CamundaProperty property, Map<String, List<String>> statusMap) {
        if (property.getAttributeValue("name").equals(StatusType.PUBLIC.propertyName)) {
            String publicStatus = property.getCamundaValue();

            Optional<CamundaProperty> internalStatusProperty =
                    property
                            .getParentElement()
                            .getChildElementsByType(CamundaProperty.class)
                            .stream()
                            .filter(
                                    p ->
                                            p.getAttributeValue("name")
                                                    .equals(StatusType.INTERNAL.propertyName))
                            .findFirst();

            if (internalStatusProperty.isPresent()) {
                statusMap
                        .computeIfAbsent(publicStatus, k -> new ArrayList<>())
                        .add(internalStatusProperty.get().getCamundaValue());
            }
        }
    }

    private List<ProcessDefinition> createProcessDefinitionSearchAndRetrieve(
            String category, List<String> keys) {
        if (category == null && keys == null) {
            // get all
            return processEngine
                    .getRepositoryService()
                    .createProcessDefinitionQuery()
                    .latestVersion()
                    .list();
        } else if (category != null) {
            // a transaction definition category search could return multiple definition keys, so it
            // has to iterate
            List<ProcessDefinition> results = new ArrayList<>();
            List<TransactionDefinition> transactionDefinitions =
                    transactionDefinitionService.getTransactionDefinitionsByPartialCategoryMatch(
                            category);
            for (TransactionDefinition transactionDefinition : transactionDefinitions) {
                List<ProcessDefinition> resultsForKey =
                        processEngine
                                .getRepositoryService()
                                .createProcessDefinitionQuery()
                                .processDefinitionKey(
                                        transactionDefinition.getProcessDefinitionKey())
                                .latestVersion()
                                .list();
                results.addAll(resultsForKey);
            }
            return results;
        } else {
            List<ProcessDefinition> combinedProcessDefinitions = new ArrayList<>();
            for (String singleKey : keys) {
                List<ProcessDefinition> processDefinitionsForSingleKey =
                        processEngine
                                .getRepositoryService()
                                .createProcessDefinitionQuery()
                                .processDefinitionKey(singleKey)
                                .latestVersion()
                                .list();
                combinedProcessDefinitions.addAll(processDefinitionsForSingleKey);
            }
            return combinedProcessDefinitions;
        }
    }

    /**
     * A request for public statuses maps to Camunda property publicStatus.
     * A request for internal statuses maps to Camunda property status.
     **/
    public enum StatusType {
        PUBLIC("publicStatus"),
        INTERNAL("status");

        public final String propertyName;

        private StatusType(String propertyName) {
            this.propertyName = propertyName;
        }
    }

    /**
     * Gets all Camunda workflows.
     *
     * @param pageable Information about page number and size.
     * @param sortOrder direction in which to sort the workflows.
     *
     * @return All Camunda workflows after being paged.
     */
    public Page<ProcessDefinition> getAllWorkflows(Pageable pageable, String sortOrder) {
        ProcessDefinitionQuery processDefinitionQuery =
                processEngine
                        .getRepositoryService()
                        .createProcessDefinitionQuery()
                        .active()
                        .latestVersion();

        long total = processDefinitionQuery.count();
        List<ProcessDefinition> processDefinitions =
                processDefinitionQuery.listPage((int) pageable.getOffset(), pageable.getPageSize());

        processDefinitions.sort(
                sortOrder.equalsIgnoreCase("ASC")
                        ? Comparator.comparing(ProcessDefinition::getKey)
                        : Comparator.comparing(ProcessDefinition::getKey).reversed());

        return new PageImpl<>(processDefinitions, pageable, total);
    }

    /**
     * Gets a workflow given its key.
     *
     * @param processDefinitionKey key to find the Camunda workflow.
     *
     * @return The workflow.
     *
     * @throws NotFoundException if the workflow is not found.
     */
    public ProcessDefinition getSingleWorkflow(String processDefinitionKey) {
        ProcessDefinitionQuery query =
                processEngine.getRepositoryService().createProcessDefinitionQuery();

        ProcessDefinition processDefinition =
                query.processDefinitionKey(processDefinitionKey).latestVersion().singleResult();

        if (processDefinition == null) {
            throw new NotFoundException(
                    "No process definition found with key: " + processDefinitionKey);
        }

        return processDefinition;
    }

    /**
     * Retrieves a list of the Camunda user tasks for a workflow with a given definition key.
     *
     * @param processDefinitionKey Key to find the workflow.
     *
     * @return The list of Camunda user tasks.
     */
    public List<UserTask> getListOfTasksByProcessDefinitionKey(String processDefinitionKey) {
        ProcessDefinition processDefinition = getSingleWorkflow(processDefinitionKey);

        BpmnModelInstance bpmnModelInstance =
                processEngine
                        .getRepositoryService()
                        .getBpmnModelInstance(processDefinition.getId());
        return new ArrayList<>(bpmnModelInstance.getModelElementsByType(UserTask.class));
    }

    public BpmnModelInstance getBpmnModelInstance(String processDefinitionId) {
        return processEngine.getRepositoryService().getBpmnModelInstance(processDefinitionId);
    }
}
