package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.MissingTaskException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.utils.camunda.CamundaWorkflowInspector;
import io.nuvalence.workmanager.service.utils.camunda.CamundaWorkflowInspectorCache;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionTaskServiceTest {

    @Mock private ProcessEngine processEngine;

    @Mock private TaskService taskService;

    @Mock private TaskQuery taskQuery;

    @Mock private EntityMapper entityMapper;

    @Mock private RuntimeService runtimeService;

    @Mock private AuthorizationHandler authorizationHandler;

    @Mock private CamundaWorkflowInspectorCache camundaWorkflowInspectorCache;

    @Mock private HistoryService historyService;

    private TransactionTaskService service;

    @BeforeEach
    void setup() {
        service =
                new TransactionTaskService(
                        processEngine,
                        entityMapper,
                        authorizationHandler,
                        camundaWorkflowInspectorCache);

        Mockito.lenient().when(processEngine.getTaskService()).thenReturn(taskService);
        Mockito.lenient().when(taskService.createTaskQuery()).thenReturn(taskQuery);
        Mockito.lenient().when(taskQuery.processInstanceId(any())).thenReturn(taskQuery);
        Mockito.lenient().when(taskQuery.active()).thenReturn(taskQuery);

        final CamundaWorkflowInspector inspector =
                new CamundaWorkflowInspector(
                        Bpmn.readModelFromStream(
                                getClass().getResourceAsStream("/TestWorkflow.bpmn")));
        Mockito.lenient()
                .when(camundaWorkflowInspectorCache.getByProcessDefinitionId(any()))
                .thenReturn(inspector);
        Mockito.lenient()
                .when(camundaWorkflowInspectorCache.getByProcessInstanceId(any()))
                .thenReturn(inspector);
        Mockito.lenient().when(processEngine.getHistoryService()).thenReturn(historyService);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        UserToken.builder()
                                .userType("agency")
                                .roles(Collections.emptyList())
                                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeTask() throws MissingTaskException {
        // Arrange
        final DynamicEntity entity = new DynamicEntity(Schema.builder().build());
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .data(entity)
                        .processInstanceId("process-instance")
                        .status("incomplete")
                        .build();
        final Map<String, Object> variables = Map.of("foo", "bar");
        when(entityMapper.convertAttributesToGenericMap(entity)).thenReturn(variables);
        final TaskEntity task = new TaskEntity("task-id");
        task.setTaskDefinitionKey("task1");
        when(taskQuery.processInstanceId(transaction.getProcessInstanceId())).thenReturn(taskQuery);
        when(taskQuery.taskDefinitionKey(task.getId())).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task));

        // Act
        service.completeTask(transaction, task.getId(), "Submit");

        // Assert
        verify(taskService)
                .complete(
                        task.getId(),
                        Map.of(
                                "transactionId",
                                transaction.getId(),
                                "data",
                                variables,
                                "action",
                                "Submit"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidAction"})
    void completeTask_invalidAction(String action) {
        // Arrange
        final DynamicEntity entity = new DynamicEntity(Schema.builder().build());
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .data(entity)
                        .processInstanceId("process-instance")
                        .status("incomplete")
                        .build();
        final TaskEntity task = new TaskEntity("task-id");
        task.setTaskDefinitionKey("task1");

        when(taskQuery.processInstanceId(transaction.getProcessInstanceId())).thenReturn(taskQuery);
        when(taskQuery.taskDefinitionKey(task.getId())).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task));

        // Act
        String taskId = task.getId();
        Exception exception =
                assertThrows(
                        ProvidedDataException.class,
                        () -> service.completeTask(transaction, taskId, action));

        // Assert
        String expectedMessage =
                "Invalid action provided for current task. Allowed action(s): Submit";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void completeTaskThrowsMissingTaskExceptionWhenTaskDoesntExist() {
        // Arrange
        final DynamicEntity entity = new DynamicEntity(Schema.builder().build());
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .data(entity)
                        .processInstanceId("process-instance")
                        .build();
        when(taskQuery.processInstanceId(transaction.getProcessInstanceId())).thenReturn(taskQuery);
        when(taskQuery.taskDefinitionKey("task-id")).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of());

        // Act and Assert
        assertThrows(
                MissingTaskException.class,
                () -> service.completeTask(transaction, "task-id", "foo"));
    }

    @Test
    void testStartTask() {
        final Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();

        String processDefinitionKey = "myProcessDefinitionKey";
        when(processEngine.getRuntimeService()).thenReturn(runtimeService);

        RuntimeService runtimeService = processEngine.getRuntimeService();

        ProcessInstance processInstanceMock = mock(ProcessInstance.class);
        when(processEngine
                        .getRuntimeService()
                        .startProcessInstanceByKey(eq(processDefinitionKey), anyMap()))
                .thenReturn(processInstanceMock);

        // Call the method under test
        service.startTask(transaction, processDefinitionKey);
        // Verify that the runtime service's startProcessInstanceByKey method was called with the
        // correct arguments
        verify(runtimeService)
                .startProcessInstanceByKey(
                        processDefinitionKey, Map.of("transactionId", transaction.getId()));
    }

    @Test
    void getActiveTasksForCurrentUser() {
        final Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();
        final TaskEntity task = new TaskEntity("task1");
        task.setTaskDefinitionKey("task1");
        when(taskQuery.list()).thenReturn(List.of(task));

        List<WorkflowTask> tasks = service.getActiveTasksForCurrentUser(transaction);

        assertEquals(1, tasks.size());
        assertEquals("task1", tasks.get(0).getKey());
    }

    @Test
    void getFirstTasksForCurrentUser() {
        // Arrange
        final CamundaWorkflowInspector inspectorMock = mock(CamundaWorkflowInspector.class);

        when(inspectorMock.getWorkflowFirstTasks())
                .thenReturn(
                        List.of(
                                WorkflowTask.builder().key("task1").build(),
                                WorkflowTask.builder().key("task2").build()));

        final TransactionDefinition trxnDef =
                TransactionDefinition.builder()
                        .processDefinitionKey("myProcessDefinitionKey")
                        .build();

        when(camundaWorkflowInspectorCache.getByProcessDefinitionKey(
                        trxnDef.getProcessDefinitionKey()))
                .thenReturn(inspectorMock);

        when(inspectorMock.isCurrentUserAllowed("task1", authorizationHandler, trxnDef))
                .thenReturn(true);
        when(inspectorMock.isCurrentUserAllowed("task2", authorizationHandler, trxnDef))
                .thenReturn(false);

        // Act
        List<WorkflowTask> result = service.getFirstTasksForCurrentUser(trxnDef);

        // Assert
        assertEquals(1, result.size());
        assertEquals("task1", result.get(0).getKey());
    }

    @Test
    void hasReachedEndEvent_ReturnsTrueWhenProcessHasEnded() {
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .processInstanceId("process-instance-id")
                        .build();
        final HistoricProcessInstanceQuery historicProcessInstanceQuery =
                mock(HistoricProcessInstanceQuery.class);
        final HistoricProcessInstance historicProcessInstance = mock(HistoricProcessInstance.class);

        when(historicProcessInstance.getEndTime()).thenReturn(new Date());
        when(historyService.createHistoricProcessInstanceQuery())
                .thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceId(transaction.getProcessInstanceId()))
                .thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.singleResult()).thenReturn(historicProcessInstance);

        boolean hasEnded = service.hasReachedEndEvent(transaction);

        assertTrue(hasEnded);
        verify(historyService).createHistoricProcessInstanceQuery();
        verify(historicProcessInstanceQuery).processInstanceId(transaction.getProcessInstanceId());
        verify(historicProcessInstanceQuery).singleResult();
        verify(historicProcessInstance).getEndTime();
    }

    @Test
    void hasReachedEndEvent_ReturnsFalseWhenProcessHasNotEnded() {
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .processInstanceId("process-instance-id")
                        .build();
        final HistoricProcessInstanceQuery historicProcessInstanceQuery =
                mock(HistoricProcessInstanceQuery.class);
        final HistoricProcessInstance historicProcessInstance = mock(HistoricProcessInstance.class);

        when(historicProcessInstance.getEndTime()).thenReturn(null);
        when(historyService.createHistoricProcessInstanceQuery())
                .thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceId(transaction.getProcessInstanceId()))
                .thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.singleResult()).thenReturn(historicProcessInstance);

        boolean hasEnded = service.hasReachedEndEvent(transaction);

        assertFalse(hasEnded);
        verify(historyService).createHistoricProcessInstanceQuery();
        verify(historicProcessInstanceQuery).processInstanceId(transaction.getProcessInstanceId());
        verify(historicProcessInstanceQuery).singleResult();
        verify(historicProcessInstance, times(1)).getEndTime();
    }
}
