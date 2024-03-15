package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.generated.models.TaskModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowModel;
import io.nuvalence.workmanager.service.service.WorkflowTasksService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Maps Camunda workflows and user tasks to api models.
 */
@Mapper(componentModel = "spring", uses = WorkflowTasksService.class)
public interface WorkflowAndTaskMapper {

    @Mapping(source = "id", target = "processDefinitionId")
    @Mapping(source = "key", target = "processDefinitionKey")
    @Mapping(source = ".", target = "name", qualifiedByName = "mapProcessDefinitionName")
    @Mapping(
            source = ".",
            target = "description",
            qualifiedByName = "mapProcessDefinitionDescription")
    WorkflowModel processDefinitionToWorkflowModel(ProcessDefinition processDefinition);

    /**
     * Map the process definition name to the workflow name.
     *
     * @param processDefinition process definition
     *
     * @return process definition name if present, otherwise the process definition key
     */
    @Named("mapProcessDefinitionName")
    default String mapProcessDefinitionName(ProcessDefinition processDefinition) {
        if (processDefinition.getName() != null && !processDefinition.getName().isEmpty()) {
            return processDefinition.getName();
        } else {
            return processDefinition.getKey();
        }
    }

    /**
     * Map the process definition description to the workflow description.
     *
     * @param processDefinition process definition
     *
     * @return process definition description if present, otherwise an empty string
     */
    @Named("mapProcessDefinitionDescription")
    default String mapProcessDefinitionDescription(ProcessDefinition processDefinition) {
        if (processDefinition.getDescription() != null) {
            return processDefinition.getDescription();
        } else {
            return "";
        }
    }

    TaskModel userTaskToTaskModel(UserTask userTask);
}
