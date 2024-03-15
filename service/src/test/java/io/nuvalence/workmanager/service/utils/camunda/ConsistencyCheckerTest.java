package io.nuvalence.workmanager.service.utils.camunda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionRepository;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.impl.instance.SequenceFlowImpl;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.community.mockito.QueryMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Named;

@ExtendWith(MockitoExtension.class)
class ConsistencyCheckerTest {
    @Mock private TransactionDefinitionRepository transactionDefinitionRepository;
    @Mock private ProcessEngine processEngine;
    @Mock private GenericApplicationContext applicationContext;
    @Mock private RepositoryService repositoryService;
    @Mock private ProcessDefinition processDefinition;
    private ConsistencyChecker consistencyChecker;

    @BeforeEach
    void setup() {
        this.consistencyChecker =
                new ConsistencyChecker(
                        transactionDefinitionRepository, processEngine, applicationContext);
    }

    @Test
    void check() {
        setupStubbings(
                "PROCESS_DEFINITION_KEY",
                "TASK_1",
                "PROCESS_DEFINITION_KEY",
                "TASK_1",
                "TestDelegate");

        List<String> issues = consistencyChecker.check();

        assertEquals(0, issues.size());
    }

    @Test
    void check_no_camunda_process_definition_key_found() {
        setupStubbings(
                "PROCESS_DEFINITION_KEY2",
                "TASK_1",
                "PROCESS_DEFINITION_KEY", // changed to have a mismatch
                "TASK_1",
                "TestDelegate");

        List<String> issues = consistencyChecker.check();

        assertTrue(
                issues.contains(
                        String.format(
                                "No Camunda process definition defined with key: '%s'.",
                                "PROCESS_DEFINITION_KEY2")));
    }

    @Test
    void check_no_transaction_definition_with_camunda_process_definition_key_found() {
        setupStubbings(
                "PROCESS_DEFINITION_KEY",
                "TASK_1",
                "PROCESS_DEFINITION_KEY2", // changed to have a mismatch
                "TASK_1",
                "TestDelegate");

        List<String> issues = consistencyChecker.check();

        assertTrue(
                issues.contains(
                        String.format(
                                "No existing transaction definition has been declared for process "
                                        + "definition with key: '%s'.",
                                "PROCESS_DEFINITION_KEY2")));
    }

    @Test
    void check_no_JavaDelegate_found_for_camunda_delegate() {
        setupStubbings(
                "PROCESS_DEFINITION_KEY",
                "TASK_1",
                "PROCESS_DEFINITION_KEY",
                "TASK_1",
                "TestDelegate1");

        List<String> issues = consistencyChecker.check();

        assertTrue(
                issues.contains(
                        String.format(
                                "A delegate expression named '%s' was declared in '%s' but no"
                                        + " matching JavaDelegate class with a @Named attribute has"
                                        + " been found.",
                                "TestDelegate1", "test.bpmn")));
    }

    @Test
    void check_unused_JavaDelegate() {
        setupStubbings(
                "PROCESS_DEFINITION_KEY",
                "TASK_1",
                "PROCESS_DEFINITION_KEY",
                "TASK_1",
                "TestDelegate2");

        List<String> issues = consistencyChecker.check();

        assertTrue(
                issues.contains(
                        String.format(
                                "A delegate exists in the application named '%s' that is not in "
                                        + "use within Camunda.",
                                "TestDelegate")));
    }

    @Test
    void testNotify() {
        // Create mock objects
        DelegateExecution delegateExecution = mock(DelegateExecution.class);
        FlowElement flowElement = mock(FlowElement.class);
        ModelInstance modelInstance = mock(ModelInstance.class);
        SequenceFlowImpl sequenceFlow = mock(SequenceFlowImpl.class);
        ExtensionElements extensionElements = mock(ExtensionElements.class);
        CamundaProperties camundaProperties = mock(CamundaProperties.class);
        CamundaProperty statusProperty = mock(CamundaProperty.class);
        CamundaProperty publicStatusProperty = mock(CamundaProperty.class);
        Query<ModelElementInstance> s = mock(Query.class);
        Query<CamundaProperties> camundaPropertiesQuery = mock(Query.class);
        when(delegateExecution.getBpmnModelElementInstance()).thenReturn(flowElement);
        when(delegateExecution.getCurrentTransitionId()).thenReturn("test");
        when(flowElement.getModelInstance()).thenReturn(modelInstance);
        when(modelInstance.getModelElementById("test")).thenReturn(sequenceFlow);
        when(sequenceFlow.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getElementsQuery()).thenReturn(s);
        when(s.filterByType(CamundaProperties.class)).thenReturn(camundaPropertiesQuery);
        when(camundaPropertiesQuery.singleResult()).thenReturn(camundaProperties);
        when(camundaPropertiesQuery.singleResult().getCamundaProperties())
                .thenReturn(Arrays.asList(statusProperty, publicStatusProperty));

        when(statusProperty.getAttributeValue("name")).thenReturn("status");
        when(statusProperty.getCamundaValue()).thenReturn("In Progress");
        when(publicStatusProperty.getAttributeValue("name")).thenReturn("publicStatus");
        when(publicStatusProperty.getCamundaValue()).thenReturn("Active");

        // Create an instance of the SequenceFlowExecutionListener
        SequenceFlowExecutionListener listener = new SequenceFlowExecutionListener();

        // Invoke the notify method
        listener.notify(delegateExecution);

        // Verify that the variables were set correctly
        verify(delegateExecution).setVariable("status", "In Progress");
        verify(delegateExecution).setVariable("publicStatus", "Active");
    }

    @Test
    void testNotifyWithNoSequenceFlow() {
        // Create mock objects
        DelegateExecution delegateExecution = mock(DelegateExecution.class);
        FlowElement flowElement = mock(FlowElement.class);
        ModelInstance modelInstance = mock(ModelInstance.class);
        when(delegateExecution.getBpmnModelElementInstance()).thenReturn(flowElement);
        when(delegateExecution.getCurrentTransitionId()).thenReturn("test");
        when(flowElement.getModelInstance()).thenReturn(modelInstance);
        when(modelInstance.getModelElementById("test")).thenReturn(null);

        // Create an instance of the SequenceFlowExecutionListener
        SequenceFlowExecutionListener listener = new SequenceFlowExecutionListener();

        // Invoke the notify method
        listener.notify(delegateExecution);

        verify(delegateExecution, never()).setVariable(anyString(), anyString());
    }

    private void setupStubbings(
            String processDefinitionKey,
            String taskName,
            String camundaProcessDefinitionKey,
            String camundaTaskName,
            String camundaDelegateName) {
        // when calls are setup as lenient since they're used for all calls
        lenient().when(processDefinition.getKey()).thenReturn(camundaProcessDefinitionKey);
        lenient().when(processDefinition.getId()).thenReturn("PROCESS_INSTANCE_ID");
        lenient().when(processDefinition.getResourceName()).thenReturn("test.bpmn");
        lenient().when(processEngine.getRepositoryService()).thenReturn(repositoryService);
        lenient()
                .when(applicationContext.getBeansOfType(JavaDelegate.class))
                .thenReturn(Map.of("TestDelegate", new MockDelegate()));
        QueryMocks.mockProcessDefinitionQuery(processEngine.getRepositoryService())
                .list(List.of(processDefinition));
        lenient()
                .when(repositoryService.getBpmnModelInstance(anyString()))
                .thenReturn(
                        Bpmn.readModelFromStream(
                                getValidBpmnModelString(
                                        camundaTaskName,
                                        camundaProcessDefinitionKey,
                                        camundaDelegateName)));
        TransactionDefinition transactionDefinition =
                getTransactionDefinition(processDefinitionKey, taskName);
        lenient()
                .when(transactionDefinitionRepository.findAll())
                .thenReturn(List.of(transactionDefinition));
    }

    private TransactionDefinition getTransactionDefinition(
            String processDefinitionKey, String taskName) {

        return TransactionDefinition.builder()
                .id(UUID.randomUUID())
                .key("TRANSACTION_DEFINITION_KEY")
                .name("Transaction Definition")
                .category("CATEGORY")
                .processDefinitionKey(processDefinitionKey)
                .schemaKey("SCHEMA_1")
                .defaultStatus("draft")
                .build();
    }

    private InputStream getValidBpmnModelString(
            String taskName, String processDefinitionKey, String delegateName) {
        return new ByteArrayInputStream(
                ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                     + "<bpmn:definitions"
                     + " xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
                     + " xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\""
                     + " xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\""
                     + " xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\""
                     + " xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\""
                     + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                     + " xmlns:bioc=\"http://bpmn.io/schema/bpmn/biocolor/1.0\""
                     + " xmlns:color=\"http://www.omg.org/spec/BPMN/non-normative/color/1.0\""
                     + " xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\""
                     + " id=\"Definitions_1343dfk\" targetNamespace=\"http://bpmn.io/schema/bpmn\""
                     + " exporter=\"Camunda Modeler\" exporterVersion=\"5.0.0\""
                     + " modeler:executionPlatform=\"Camunda Platform\""
                     + " modeler:executionPlatformVersion=\"7.15.0\">\n"
                     + "  <bpmn:collaboration id=\"Collaboration_0slndn1\">\n"
                     + "    <bpmn:participant id=\"Participant_07k0cyw\" name=\"Test\""
                     + " processRef=\""
                                + processDefinitionKey
                                + "\" />\n"
                                + "  </bpmn:collaboration>\n"
                                + "  <bpmn:process id=\""
                                + processDefinitionKey
                                + "\" name=\""
                                + processDefinitionKey
                                + "\" isExecutable=\"true\">\n"
                                + "    <bpmn:sequenceFlow id=\"Flow_0y7ki00\""
                                + " sourceRef=\"StartEvent_1\" targetRef=\""
                                + taskName
                                + "\" />\n"
                                + "    <bpmn:endEvent id=\"EndEvent_1\">\n"
                                + "      <bpmn:extensionElements />\n"
                                + "      <bpmn:incoming>Flow_1rerpnm</bpmn:incoming>\n"
                                + "    </bpmn:endEvent>\n"
                                + "    <bpmn:sequenceFlow id=\"Flow_0pk29ve\" sourceRef=\""
                                + taskName
                                + "\" targetRef=\"Activity_1hdaye6\">\n"
                                + "      <bpmn:extensionElements />\n"
                                + "    </bpmn:sequenceFlow>\n"
                                + "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\">\n"
                                + "      <bpmn:outgoing>Flow_0y7ki00</bpmn:outgoing>\n"
                                + "    </bpmn:startEvent>\n"
                                + "    <bpmn:userTask id=\""
                                + taskName
                                + "\" name=\"Task\">\n"
                                + "      <bpmn:extensionElements />\n"
                                + "      <bpmn:incoming>Flow_0y7ki00</bpmn:incoming>\n"
                                + "      <bpmn:outgoing>Flow_0pk29ve</bpmn:outgoing>\n"
                                + "    </bpmn:userTask>\n"
                                + "    <bpmn:sequenceFlow id=\"Flow_1rerpnm\""
                                + " sourceRef=\"Activity_1hdaye6\" targetRef=\"EndEvent_1\" />\n"
                                + "    <bpmn:serviceTask id=\"Activity_1hdaye6\" name=\"Test"
                                + " Delegate\" camunda:delegateExpression=\"#{"
                                + delegateName
                                + "}\">\n"
                                + "      <bpmn:extensionElements />\n"
                                + "      <bpmn:incoming>Flow_0pk29ve</bpmn:incoming>\n"
                                + "      <bpmn:outgoing>Flow_1rerpnm</bpmn:outgoing>\n"
                                + "    </bpmn:serviceTask>\n"
                                + "  </bpmn:process>\n"
                                + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                                + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\""
                                + " bpmnElement=\"Collaboration_0slndn1\">\n"
                                + "      <bpmndi:BPMNShape id=\"Participant_07k0cyw_di\""
                                + " bpmnElement=\"Participant_07k0cyw\" isHorizontal=\"true\">\n"
                                + "        <dc:Bounds x=\"129\" y=\"80\" width=\"751\""
                                + " height=\"250\" />\n"
                                + "        <bpmndi:BPMNLabel />\n"
                                + "      </bpmndi:BPMNShape>\n"
                                + "      <bpmndi:BPMNEdge id=\"Flow_1rerpnm_di\""
                                + " bpmnElement=\"Flow_1rerpnm\">\n"
                                + "        <di:waypoint x=\"730\" y=\"197\" />\n"
                                + "        <di:waypoint x=\"772\" y=\"197\" />\n"
                                + "      </bpmndi:BPMNEdge>\n"
                                + "      <bpmndi:BPMNEdge id=\"Flow_0pk29ve_di\""
                                + " bpmnElement=\"Flow_0pk29ve\" bioc:stroke=\"#1e88e5\""
                                + " color:border-color=\"#1e88e5\">\n"
                                + "        <di:waypoint x=\"410\" y=\"197\" />\n"
                                + "        <di:waypoint x=\"630\" y=\"197\" />\n"
                                + "        <bpmndi:BPMNLabel>\n"
                                + "          <dc:Bounds x=\"498\" y=\"206\" width=\"50\""
                                + " height=\"27\" />\n"
                                + "        </bpmndi:BPMNLabel>\n"
                                + "      </bpmndi:BPMNEdge>\n"
                                + "      <bpmndi:BPMNEdge id=\"Flow_0y7ki00_di\""
                                + " bpmnElement=\"Flow_0y7ki00\">\n"
                                + "        <di:waypoint x=\"258\" y=\"197\" />\n"
                                + "        <di:waypoint x=\"310\" y=\"197\" />\n"
                                + "      </bpmndi:BPMNEdge>\n"
                                + "      <bpmndi:BPMNShape id=\"Event_0snp829_di\""
                                + " bpmnElement=\"EndEvent_1\">\n"
                                + "        <dc:Bounds x=\"772\" y=\"179\" width=\"36\""
                                + " height=\"36\" />\n"
                                + "      </bpmndi:BPMNShape>\n"
                                + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\""
                                + " bpmnElement=\"StartEvent_1\">\n"
                                + "        <dc:Bounds x=\"222\" y=\"179\" width=\"36\""
                                + " height=\"36\" />\n"
                                + "        <bpmndi:BPMNLabel>\n"
                                + "          <dc:Bounds x=\"229\" y=\"222\" width=\"25\""
                                + " height=\"14\" />\n"
                                + "        </bpmndi:BPMNLabel>\n"
                                + "      </bpmndi:BPMNShape>\n"
                                + "      <bpmndi:BPMNShape id=\"Activity_1m34esl_di\""
                                + " bpmnElement=\""
                                + taskName
                                + "\">\n"
                                + "        <dc:Bounds x=\"310\" y=\"157\" width=\"100\""
                                + " height=\"80\" />\n"
                                + "        <bpmndi:BPMNLabel />\n"
                                + "      </bpmndi:BPMNShape>\n"
                                + "      <bpmndi:BPMNShape id=\"Activity_1fygaiv_di\""
                                + " bpmnElement=\"Activity_1hdaye6\">\n"
                                + "        <dc:Bounds x=\"630\" y=\"157\" width=\"100\""
                                + " height=\"80\" />\n"
                                + "        <bpmndi:BPMNLabel />\n"
                                + "      </bpmndi:BPMNShape>\n"
                                + "    </bpmndi:BPMNPlane>\n"
                                + "  </bpmndi:BPMNDiagram>\n"
                                + "</bpmn:definitions>\n")
                        .getBytes(StandardCharsets.UTF_8));
    }

    @Named(value = "TestDelegate")
    private static class MockDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) throws Exception {}
    }
}
