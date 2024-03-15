package io.nuvalence.workmanager.service.camunda.delegates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.service.TransactionService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionStatusUpdateDelegateTest {

    @Mock private DelegateExecution execution;

    private UUID transactionId = UUID.randomUUID();
    private UUID transactionDefinitionKey = UUID.randomUUID();
    private String status = "Draft";
    private String processInstanceId = "processInstanceId";
    private Clock clock;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }

    @Test
    void testTransactionStatusUpdateDelegate_happyPath() throws Exception {
        // Arrange
        stubGetExecutionVariables();

        // Mock BpmnModelElementInstance
        CamundaProperties camundaProperties = Mockito.mock(CamundaProperties.class);
        mockBpmnModelElementInstance(camundaProperties);

        // Mock CamundaProperty
        mockCamundaProperty(camundaProperties);

        TransactionService transactionService = Mockito.mock(TransactionService.class);
        mockTransaction(transactionService);

        TransactionStatusUpdateDelegate statusUpdateDelegate =
                new TransactionStatusUpdateDelegate(transactionService);

        // Act and Assert
        statusUpdateDelegate.execute(execution);

        Mockito.verify(transactionService).getTransactionById(transactionId);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        Mockito.verify(transactionService).updateTransaction(transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(status, capturedTransaction.getStatus());
        assertEquals(processInstanceId, capturedTransaction.getProcessInstanceId());
    }

    @Test
    void testTransactionStatusUpdateDelegate_nullTransactionId() throws Exception {
        // Arrange
        Mockito.when(execution.getVariable("transactionId")).thenReturn(null);

        TransactionService transactionService = Mockito.mock(TransactionService.class);
        TransactionStatusUpdateDelegate statusUpdateDelegate =
                new TransactionStatusUpdateDelegate(transactionService);

        // Act and Assert
        statusUpdateDelegate.execute(execution);

        // Verify that transactionService methods were not called
        Mockito.verifyNoInteractions(transactionService);
    }

    @Test
    void testTransactionStatusUpdateDelegate_noStatusOptional() throws Exception {
        // Arrange
        stubGetExecutionVariables();

        // Mock BpmnModelElementInstance and CamundaProperties
        mockBpmnModelElementInstance(null);

        TransactionService transactionService = Mockito.mock(TransactionService.class);
        mockTransaction(transactionService);

        TransactionStatusUpdateDelegate statusUpdateDelegate =
                new TransactionStatusUpdateDelegate(transactionService);

        // Act and Assert
        statusUpdateDelegate.execute(execution);

        Mockito.verify(transactionService).getTransactionById(transactionId);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        Mockito.verify(transactionService).updateTransaction(transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(status, capturedTransaction.getStatus());
        assertEquals(processInstanceId, capturedTransaction.getProcessInstanceId());
    }

    @Test
    void testTransactionStatusUpdateDelegate_noTransactionOptional() throws Exception {
        // Arrange
        Mockito.when(execution.getVariable("transactionId")).thenReturn(transactionId);

        // Mock BpmnModelElementInstance and CamundaProperties
        CamundaProperties camundaProperties = Mockito.mock(CamundaProperties.class);
        mockBpmnModelElementInstance(camundaProperties);

        // Mock CamundaProperty
        mockCamundaProperty(camundaProperties);

        TransactionService transactionService = Mockito.mock(TransactionService.class);
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.empty());

        TransactionStatusUpdateDelegate statusUpdateDelegate =
                new TransactionStatusUpdateDelegate(transactionService);

        // Act and Assert
        statusUpdateDelegate.execute(execution);

        // Verify that transactionService methods were called with default status
        Mockito.verify(transactionService).getTransactionById(transactionId);
        Mockito.verify(transactionService, Mockito.never())
                .updateTransaction(Mockito.any(Transaction.class));
    }

    private void stubGetExecutionVariables() {
        Mockito.when(execution.getVariable("transactionId")).thenReturn(transactionId);
        Mockito.when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
    }

    private void mockBpmnModelElementInstance(CamundaProperties camundaProperties) {
        FlowElement flowElement = Mockito.mock(FlowElement.class);
        Mockito.when(flowElement.getExtensionElements())
                .thenReturn(Mockito.mock(ExtensionElements.class));
        Mockito.when(execution.getBpmnModelElementInstance()).thenReturn(flowElement);
        Mockito.when(flowElement.getExtensionElements().getElementsQuery())
                .thenReturn(Mockito.mock(Query.class));
        Mockito.when(
                        flowElement
                                .getExtensionElements()
                                .getElementsQuery()
                                .filterByType(CamundaProperties.class))
                .thenReturn(Mockito.mock(Query.class));
        Mockito.when(
                        flowElement
                                .getExtensionElements()
                                .getElementsQuery()
                                .filterByType(CamundaProperties.class)
                                .list())
                .thenReturn(camundaProperties == null ? List.of() : List.of(camundaProperties));
    }

    private void mockCamundaProperty(CamundaProperties camundaProperties) {
        CamundaProperty camundaProperty = Mockito.mock(CamundaProperty.class);
        Mockito.when(camundaProperties.getCamundaProperties())
                .thenReturn(Collections.singletonList(camundaProperty));
        Mockito.when(camundaProperty.getCamundaName()).thenReturn("status");
        Mockito.when(camundaProperty.getCamundaValue()).thenReturn(status);
    }

    private void mockTransaction(TransactionService transactionService) {
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .transactionDefinitionId(transactionDefinitionKey)
                        .transactionDefinitionKey("DefinitionKey")
                        .processInstanceId("")
                        .status("")
                        .priority(TransactionPriority.MEDIUM)
                        .createdTimestamp(OffsetDateTime.now(clock))
                        .lastUpdatedTimestamp(OffsetDateTime.now(clock))
                        .data(new DynamicEntity(Schema.builder().name("schema").build()))
                        .externalId("y")
                        .build();

        Optional<Transaction> transactionOptional = Optional.of(transaction);
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(transactionOptional);
        Mockito.when(transactionService.updateTransaction(transaction)).thenReturn(transaction);
    }
}
