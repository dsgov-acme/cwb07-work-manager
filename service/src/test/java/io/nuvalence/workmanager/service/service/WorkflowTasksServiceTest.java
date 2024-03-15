package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstanceQuery;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class WorkflowTasksServiceTest {

    private TransactionDefinitionService transactionDefService;
    private ProcessEngine processEngine;
    private WorkflowTasksService service;

    @BeforeEach
    void setUp() {
        // mocks
        transactionDefService = mock(TransactionDefinitionService.class);
        processEngine = mock(ProcessEngine.class);

        // init service
        service = new WorkflowTasksService(processEngine, transactionDefService);
    }

    @Test
    void shouldCreateInstance() throws Exception {
        assertNotNull(service);
        assertNotNull(processEngine);
        assertNotNull(transactionDefService);
    }

    @Test
    void searchCamundaStatusesByProcessKeyDefault() throws Exception {

        // vars
        String resourceName = "searchCamundaStatusesByProcessKeyDefault";
        UUID processId = UUID.randomUUID();

        // readable bpmn model
        Files.write(Paths.get(resourceName), getBpmnModelString().getBytes("UTF-8"));

        // process def query
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery(resourceName, processId);

        // method test
        List<String> response =
                service.getCamundaStatuses(
                        "nonSpecifiedTypeDefaultsToPublic", null, List.of(processId.toString()));

        // checks and asserts
        verify(processDefinitionQuery, times(1)).processDefinitionKey(anyString());

        assertNotNull(response);
        assertNotNull(response.get(0));
        assertEquals("publicPropValue", response.get(0));

        Files.delete(Paths.get(resourceName));
        assert (new File(resourceName).exists() == false);
    }

    @Test
    void searchCamundaStatusesGetAllDefault() throws Exception {

        // vars
        String resourceName = "searchCamundaStatusesGetAllDefault";

        // readable bpmn model
        Files.write(Paths.get(resourceName), getBpmnModelString().getBytes("UTF-8"));

        // process def query
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery(resourceName, null);

        // method test
        List<String> response =
                service.getCamundaStatuses("nonSpecifiedTypeDefaultsToPublic", null, null);

        // checks and asserts
        verify(processDefinitionQuery, times(0)).processDefinitionKey(anyString());

        assertNotNull(response);
        assertNotNull(response.get(0));
        assertEquals("publicPropValue", response.get(0));

        Files.delete(Paths.get(resourceName));
        assert (new File(resourceName).exists() == false);
    }

    @Test
    void searchCamundaStatusesGetAllInternal() throws Exception {

        // vars
        String resourceName = "searchCamundaStatusesGetAllInternal";

        // readable bpmn model
        Files.write(Paths.get(resourceName), getBpmnModelString().getBytes("UTF-8"));

        // process def query
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery(resourceName, null);

        // method test
        List<String> response = service.getCamundaStatuses("internal", null, null);

        // checks and asserts
        verify(processDefinitionQuery, times(0)).processDefinitionKey(anyString());

        assertNotNull(response);
        assertNotNull(response.get(0));
        assertEquals("internalPropValue", response.get(0));

        Files.delete(Paths.get(resourceName));
        assert (new File(resourceName).exists() == false);
    }

    @Test
    void searchCamundaStatusesByProcessKeyNotFoundProcess() throws Exception {

        // vars
        String resourceName = "NOT-FINDABLE-PROCESS";

        // process def query
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery(resourceName, null);

        // method test
        List<String> response = service.getCamundaStatuses("internal", null, null);

        // checks and asserts
        assertNotNull(response);
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> {
                    response.get(0);
                });

        verify(processDefinitionQuery, times(0)).processDefinitionKey(anyString());
    }

    @Test
    void searchCamundaStatusesByCategory() throws Exception {

        // vars
        String resourceName = "searchCamundaStatusesByCategory";
        UUID processId = UUID.randomUUID();
        String category = "somecategory";

        // readable bpmn model
        Files.write(Paths.get(resourceName), getBpmnModelString().getBytes("UTF-8"));

        // transaction def query by category
        configTransactionDefService(category, processId);
        // process def query
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery(resourceName, processId);

        // method test
        List<String> response =
                service.getCamundaStatuses("nonSpecifiedTypeDefaultsToPublic", category, null);

        // checks and asserts
        verify(processDefinitionQuery, times(1)).processDefinitionKey(anyString());
        verify(transactionDefService, times(1))
                .getTransactionDefinitionsByPartialCategoryMatch(category);

        assertNotNull(response);
        assertNotNull(response.get(0));
        assertEquals("publicPropValue", response.get(0));

        Files.delete(Paths.get(resourceName));
        assert (new File(resourceName).exists() == false);
    }

    @Test
    void getCamundaStatusesMap() throws Exception {

        // vars
        String resourceName = "getCamundaStatusesMap";
        UUID processId = UUID.randomUUID();
        String category = "somecategory";

        // readable bpmn model
        Files.write(Paths.get(resourceName), getBpmnModelString().getBytes("UTF-8"));

        // transaction def query by category
        configTransactionDefService(category, processId);
        // process def query
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery(resourceName, processId);

        // method test
        Map<String, List<String>> response = service.getStatusMap(category, null);

        // checks and asserts
        verify(processDefinitionQuery, times(1)).processDefinitionKey(anyString());
        verify(transactionDefService, times(1))
                .getTransactionDefinitionsByPartialCategoryMatch(category);

        assertNotNull(response);
        assertNotNull(response.get("publicPropValue"));
        assertEquals("internalPropValue", response.get("publicPropValue").get(0));

        Files.delete(Paths.get(resourceName));
        assert (new File(resourceName).exists() == false);
    }

    @Test
    void getCamundaStatusesMapModelResourceNotFound() throws Exception {

        // vars
        String resourceName = "getCamundaStatusesMapModelResourceNotFound";
        UUID processId = UUID.randomUUID();
        String category = "somecategory";

        // transaction def query by category
        configTransactionDefService(category, processId);
        // process def query
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery(resourceName, processId);

        // method test
        Map<String, List<String>> response = service.getStatusMap(category, null);

        // checks and asserts
        verify(processDefinitionQuery, times(1)).processDefinitionKey(anyString());
        verify(transactionDefService, times(1))
                .getTransactionDefinitionsByPartialCategoryMatch(category);

        assertNotNull(response);
        assertNull(response.get("publicPropValue"));
    }

    @Test
    void getSingleWorkflowShouldReturnProcessDefinition() throws Exception {
        UUID processDefinitionKey = UUID.randomUUID();
        ProcessDefinitionQuery processDefinitionQuery =
                initProcessDefinitionQuery("someResourceName", processDefinitionKey);

        ProcessDefinition response = service.getSingleWorkflow(processDefinitionKey.toString());

        verify(processDefinitionQuery, times(1))
                .processDefinitionKey(processDefinitionKey.toString());
        assertNotNull(response);
        assertEquals(processDefinitionKey.toString(), response.getKey());
    }

    @Test
    void getSingleWorkflowShouldThrowNotFoundException() {
        UUID processDefinitionKey = UUID.randomUUID();
        initProcessDefinitionQueryToThrowNotFound(processDefinitionKey);

        assertThrowsNotFoundForGetSingleWorkflow(processDefinitionKey);
    }

    private void assertThrowsNotFoundForGetSingleWorkflow(UUID processDefinitionKey) {
        String processDefinitionKeyString = processDefinitionKey.toString();
        assertThrows(
                NotFoundException.class,
                () -> service.getSingleWorkflow(processDefinitionKeyString));
    }

    @Test
    void getListOfTasksByProcessDefinitionKeyShouldReturnUserTasks() {
        UUID processDefinitionKey = UUID.randomUUID();
        initProcessDefinitionQuery("test", processDefinitionKey);
        ProcessDefinition processDefinition =
                service.getSingleWorkflow(processDefinitionKey.toString());
        UserTask userTask = mock(UserTask.class);
        BpmnModelInstance bpmnModelInstance = mock(BpmnModelInstance.class);
        when(processEngine.getRepositoryService().getBpmnModelInstance(processDefinition.getId()))
                .thenReturn(bpmnModelInstance);
        Collection<UserTask> tasks = new ArrayList<>();
        tasks.add(userTask);
        when(bpmnModelInstance.getModelElementsByType(UserTask.class)).thenReturn(tasks);

        List<UserTask> response =
                service.getListOfTasksByProcessDefinitionKey(processDefinitionKey.toString());

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(userTask, response.get(0));
    }

    @Test
    void getBpmnModelInstanceShouldReturnBpmnModelInstance() {
        String processDefinitionId = UUID.randomUUID().toString();
        BpmnModelInstance bpmnModelInstance = mock(BpmnModelInstance.class);
        RepositoryService repositoryService = mock(RepositoryService.class);
        when(processEngine.getRepositoryService()).thenReturn(repositoryService);
        when(repositoryService.getBpmnModelInstance(processDefinitionId))
                .thenReturn(bpmnModelInstance);

        BpmnModelInstance response = service.getBpmnModelInstance(processDefinitionId);

        assertNotNull(response);
        assertEquals(bpmnModelInstance, response);
    }

    @Test
    void testAllWorkflows() {
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);

        ProcessDefinition definitionOne = mock(ProcessDefinition.class);
        ProcessDefinition definitionTwo = mock(ProcessDefinition.class);

        List<ProcessDefinition> processDefinitions =
                new ArrayList<>(List.of(definitionOne, definitionTwo));

        Pageable pageable = PageRequest.of(1, 10);

        RepositoryService repositoryService = mock(RepositoryService.class);
        when(processEngine.getRepositoryService()).thenReturn(repositoryService);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.active()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.count()).thenReturn(2L);
        when(processDefinitionQuery.listPage((int) pageable.getOffset(), pageable.getPageSize()))
                .thenReturn(processDefinitions);

        when(definitionOne.getKey()).thenReturn("A");
        when(definitionTwo.getKey()).thenReturn("B");

        Page<ProcessDefinition> result = service.getAllWorkflows(pageable, "ASC");

        assertEquals(2, result.get().count());
        assertTrue(result.get().anyMatch(definition -> definition.getKey().equals("A")));
        assertTrue(result.get().anyMatch(definition -> definition.getKey().equals("B")));
    }

    private void initProcessDefinitionQueryToThrowNotFound(UUID processId) {
        RepositoryService repositoryService = mock(RepositoryService.class);
        when(processEngine.getRepositoryService()).thenReturn(repositoryService);

        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);

        if (processId != null) {
            when(processDefinitionQuery.processDefinitionKey(processId.toString()))
                    .thenReturn(processDefinitionQuery);
        }

        lenient().when(processDefinitionQuery.list()).thenReturn(new ArrayList<>());
        lenient().when(processDefinitionQuery.singleResult()).thenReturn(null);
    }

    private ProcessDefinitionQuery initProcessDefinitionQuery(String resourceName, UUID processId) {

        // process def query
        RepositoryService repositoryService = mock(RepositoryService.class);
        when(processEngine.getRepositoryService()).thenReturn(repositoryService);

        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);

        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity();

        // selective process id config
        if (processId != null) {
            when(processDefinitionQuery.processDefinitionKey(processId.toString()))
                    .thenReturn(processDefinitionQuery);
            processDefinition.setKey(processId.toString());
        }

        processDefinition.setResourceName(resourceName);

        List<ProcessDefinition> processDefinitions = new ArrayList<ProcessDefinition>();
        processDefinitions.add(processDefinition);

        lenient().when(processDefinitionQuery.list()).thenReturn(processDefinitions);
        lenient().when(processDefinitionQuery.singleResult()).thenReturn(processDefinition);

        return processDefinitionQuery;
    }

    private TaskQuery initTaskQuery(String taskName, String taskId, String processDefId) {

        // tasks query
        TaskService taskService = mock(TaskService.class);
        when(processEngine.getTaskService()).thenReturn(taskService);

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId(anyString())).thenReturn(taskQuery);

        // task config
        TaskEntity task = new TaskEntity();
        task.setName(taskName);
        task.setTaskDefinitionKey(taskId);
        task.setProcessDefinitionId(processDefId);
        List<Task> taskList = new ArrayList<Task>();
        taskList.add(task);

        when(taskQuery.list()).thenReturn(taskList);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);

        return taskQuery;
    }

    private HistoricTaskInstanceQuery initHistoricTaskInstanceQuery(
            String taskName, String taskId, UUID processId) {

        // completed tasks query
        HistoryService historyService = mock(HistoryService.class);
        when(processEngine.getHistoryService()).thenReturn(historyService);

        HistoricTaskInstanceQuery historicTaskInstanceQuery = mock(HistoricTaskInstanceQuery.class);
        when(historyService.createHistoricTaskInstanceQuery())
                .thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.processInstanceId(processId.toString()))
                .thenReturn(historicTaskInstanceQuery);
        when(historicTaskInstanceQuery.finished()).thenReturn(historicTaskInstanceQuery);

        // completed task config
        HistoricTaskInstanceEntity taskInstance = new HistoricTaskInstanceEntity();
        taskInstance.setName(taskName);
        taskInstance.setTaskDefinitionKey(taskId);
        ArrayList<HistoricTaskInstance> completedTasksList = new ArrayList<HistoricTaskInstance>();
        completedTasksList.add(taskInstance);

        when(historicTaskInstanceQuery.list()).thenReturn(completedTasksList);

        return historicTaskInstanceQuery;
    }

    private RepositoryService initRepositoryService(String resourceName) {

        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getResourceName()).thenReturn(resourceName);
        RepositoryService repositoryService = mock(RepositoryService.class);
        when(repositoryService.getProcessDefinition(anyString())).thenReturn(processDefinition);
        when(processEngine.getRepositoryService()).thenReturn(repositoryService);

        return repositoryService;
    }

    private void configTransactionDefService(String category, UUID processId) {

        TransactionDefinition transactionDef = new TransactionDefinition();
        transactionDef.setCategory(category);
        transactionDef.setProcessDefinitionKey(processId.toString());
        var transactionsList = new ArrayList<TransactionDefinition>();
        transactionsList.add(transactionDef);

        when(transactionDefService.getTransactionDefinitionsByPartialCategoryMatch(category))
                .thenReturn(transactionsList);
    }

    private String getBpmnModelString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<bpmn:definitions"
                + " xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
                + "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"
                + "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"
                + "  xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n"
                + "  xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\""
                + " id=\"PROCESS_DEFINITION_DIAGRAM_1\"\n"
                + "  targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\""
                + " exporterVersion=\"5.11.0\"\n"
                + "  modeler:executionPlatform=\"Camunda Platform\""
                + " modeler:executionPlatformVersion=\"7.15.0\">\n"
                + "  <bpmn:collaboration id=\"Collaboration_0slndn1\">\n"
                + "    <bpmn:participant id=\"Participant_07k0cyw\" name=\"Test\""
                + " processRef=\"PROCESS_DEFINITION_KEY\" />\n"
                + "  </bpmn:collaboration>\n"
                + "  <bpmn:process id=\"PROCESS_DEFINITION_KEY\" name=\"PROCESS_DEFINITION_KEY\""
                + " isExecutable=\"true\">\n"
                + "    <bpmn:sequenceFlow id=\"Flow_0y7ki00\" sourceRef=\"StartEvent_1\""
                + " targetRef=\"TASK_1\" />\n"
                + "    <bpmn:exclusiveGateway id=\"ExclusiveGateway_1\" />\n"
                + "    <bpmn:endEvent id=\"EndEvent_1\">\n"
                + "      <bpmn:extensionElements />\n"
                + "      <bpmn:incoming>Flow_1rerpnm</bpmn:incoming>\n"
                + "    </bpmn:endEvent>\n"
                + "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\">\n"
                + "      <bpmn:outgoing>Flow_0y7ki00</bpmn:outgoing>\n"
                + "    </bpmn:startEvent>\n"
                + "    <bpmn:userTask id=\"TASK_1\" name=\"Task\">\n"
                + "      <bpmn:extensionElements>\n"
                + "        <camunda:formData>\n"
                + "          <camunda:formField>\n"
                + "            <camunda:properties>\n"
                + "              <camunda:property id=\"publicPropId\" value=\"publicPropValue\""
                + " name=\"publicStatus\" />\n"
                + "              <camunda:property id=\"internalPropId\""
                + " value=\"internalPropValue\" name=\"status\" />\n"
                + "            </camunda:properties>\n"
                + "          </camunda:formField>\n"
                + "        </camunda:formData>\n"
                + "      </bpmn:extensionElements>\n"
                + "      <bpmn:incoming>Flow_0y7ki00</bpmn:incoming>\n"
                + "      <bpmn:outgoing>Flow_0jrde5d</bpmn:outgoing>\n"
                + "    </bpmn:userTask>\n"
                + "    <bpmn:sequenceFlow id=\"Flow_1rerpnm\" sourceRef=\"Activity_1hdaye6\""
                + " targetRef=\"EndEvent_1\" />\n"
                + "    <bpmn:serviceTask id=\"Activity_1hdaye6\" name=\"Test Delegate\"\n"
                + "      camunda:delegateExpression=\"#{TestDelegate}\">\n"
                + "      <bpmn:extensionElements />\n"
                + "      <bpmn:incoming>Flow_1h2vdua</bpmn:incoming>\n"
                + "      <bpmn:outgoing>Flow_1rerpnm</bpmn:outgoing>\n"
                + "    </bpmn:serviceTask>\n"
                + "    <bpmn:sequenceFlow id=\"Flow_0jrde5d\" sourceRef=\"TASK_1\""
                + " targetRef=\"Gateway_1rmevdh\" />\n"
                + "    <bpmn:sequenceFlow id=\"Flow_1h2vdua\" sourceRef=\"Gateway_1rmevdh\""
                + " targetRef=\"Activity_1hdaye6\" />\n"
                + "    <bpmn:exclusiveGateway id=\"Gateway_1rmevdh\">\n"
                + "      <bpmn:incoming>Flow_0jrde5d</bpmn:incoming>\n"
                + "      <bpmn:outgoing>Flow_1h2vdua</bpmn:outgoing>\n"
                + "    </bpmn:exclusiveGateway>\n"
                + "  </bpmn:process>\n"
                + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\""
                + " bpmnElement=\"Collaboration_0slndn1\">\n"
                + "      <bpmndi:BPMNShape id=\"Participant_07k0cyw_di\""
                + " bpmnElement=\"Participant_07k0cyw\"\n"
                + "        isHorizontal=\"true\">\n"
                + "        <dc:Bounds x=\"129\" y=\"80\" width=\"751\" height=\"250\" />\n"
                + "        <bpmndi:BPMNLabel />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"Event_0snp829_di\" bpmnElement=\"EndEvent_1\">\n"
                + "        <dc:Bounds x=\"772\" y=\"179\" width=\"36\" height=\"36\" />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\""
                + " bpmnElement=\"StartEvent_1\">\n"
                + "        <dc:Bounds x=\"222\" y=\"179\" width=\"36\" height=\"36\" />\n"
                + "        <bpmndi:BPMNLabel>\n"
                + "          <dc:Bounds x=\"229\" y=\"222\" width=\"25\" height=\"14\" />\n"
                + "        </bpmndi:BPMNLabel>\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"Activity_1m34esl_di\" bpmnElement=\"TASK_1\">\n"
                + "        <dc:Bounds x=\"310\" y=\"157\" width=\"100\" height=\"80\" />\n"
                + "        <bpmndi:BPMNLabel />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"Activity_1fygaiv_di\""
                + " bpmnElement=\"Activity_1hdaye6\">\n"
                + "        <dc:Bounds x=\"630\" y=\"157\" width=\"100\" height=\"80\" />\n"
                + "        <bpmndi:BPMNLabel />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"Gateway_1rmevdh_di\""
                + " bpmnElement=\"Gateway_1rmevdh\" isMarkerVisible=\"true\">\n"
                + "        <dc:Bounds x=\"485\" y=\"172\" width=\"50\" height=\"50\" />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNEdge id=\"Flow_0y7ki00_di\" bpmnElement=\"Flow_0y7ki00\">\n"
                + "        <di:waypoint x=\"258\" y=\"197\" />\n"
                + "        <di:waypoint x=\"310\" y=\"197\" />\n"
                + "      </bpmndi:BPMNEdge>\n"
                + "      <bpmndi:BPMNEdge id=\"Flow_1rerpnm_di\" bpmnElement=\"Flow_1rerpnm\">\n"
                + "        <di:waypoint x=\"730\" y=\"197\" />\n"
                + "        <di:waypoint x=\"772\" y=\"197\" />\n"
                + "      </bpmndi:BPMNEdge>\n"
                + "      <bpmndi:BPMNEdge id=\"Flow_0jrde5d_di\" bpmnElement=\"Flow_0jrde5d\">\n"
                + "        <di:waypoint x=\"410\" y=\"197\" />\n"
                + "        <di:waypoint x=\"485\" y=\"197\" />\n"
                + "      </bpmndi:BPMNEdge>\n"
                + "      <bpmndi:BPMNEdge id=\"Flow_1h2vdua_di\" bpmnElement=\"Flow_1h2vdua\">\n"
                + "        <di:waypoint x=\"535\" y=\"197\" />\n"
                + "        <di:waypoint x=\"630\" y=\"197\" />\n"
                + "      </bpmndi:BPMNEdge>\n"
                + "    </bpmndi:BPMNPlane>\n"
                + "  </bpmndi:BPMNDiagram>\n"
                + "</bpmn:definitions>\n";
    }
}
