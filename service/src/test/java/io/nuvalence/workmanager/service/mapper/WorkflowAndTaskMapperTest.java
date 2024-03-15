package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.generated.models.TaskModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowModel;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class WorkflowAndTaskMapperTest {

    private WorkflowAndTaskMapper mapper;

    @BeforeEach
    void setup() {
        mapper = Mappers.getMapper(WorkflowAndTaskMapper.class);
    }

    @Test
    void processDefinitionToWorkflowModelTest() {
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getId()).thenReturn("id");
        when(processDefinition.getKey()).thenReturn("key");
        when(processDefinition.getName()).thenReturn("name");
        when(processDefinition.getDescription()).thenReturn("description");

        WorkflowModel expected = new WorkflowModel();
        expected.setProcessDefinitionId("id");
        expected.setProcessDefinitionKey("key");
        expected.setName("name");
        expected.setDescription("description");

        WorkflowModel actual = mapper.processDefinitionToWorkflowModel(processDefinition);

        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void userTaskToTaskModelTest() {
        UserTask userTask = mock(UserTask.class);
        when(userTask.getName()).thenReturn("name");
        when(userTask.getId()).thenReturn("id");

        TaskModel actual = mapper.userTaskToTaskModel(userTask);

        TaskModel expected = new TaskModel();
        expected.setName("name");
        expected.setId("id");

        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void testMapProcessDefinitionNameGetName() {
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getName()).thenReturn("name");

        String actual = mapper.mapProcessDefinitionName(processDefinition);

        assertEquals("name", actual);
    }

    @Test
    void testMapProcessDefinitionNameGetKey() {
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getKey()).thenReturn("key");

        String actual = mapper.mapProcessDefinitionName(processDefinition);

        assertEquals("key", actual);
    }
}
