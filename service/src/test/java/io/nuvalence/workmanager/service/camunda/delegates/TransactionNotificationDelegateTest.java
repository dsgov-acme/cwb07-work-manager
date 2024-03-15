package io.nuvalence.workmanager.service.camunda.delegates;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.SendNotificationService;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class TransactionNotificationDelegateTest {

    @Mock private DelegateExecution execution;

    @Mock private TransactionService transactionService;

    @Mock private SendNotificationService sendNotificationService;

    private Transaction transaction;

    private TransactionNotificationDelegate delegate;

    @BeforeEach
    void setUp() {
        transaction = Transaction.builder().id(UUID.randomUUID()).build();

        delegate = new TransactionNotificationDelegate(transactionService, sendNotificationService);
    }

    @Test
    void testTransactionNotificationDelegate() throws Exception {
        when(execution.getVariable("transactionId")).thenReturn(transaction.getId());
        when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        CamundaProperties camundaProperties = Mockito.mock(CamundaProperties.class);
        mockBpmnModelElementInstance(camundaProperties);
        mockCamundaProperty(camundaProperties);

        delegate.execute(execution);

        Map<String, String> properties = new HashMap<>();
        properties.put("transactionId", "id");

        verify(sendNotificationService, times(1))
                .sendTransactionNotification(transaction, "TestTemplate", properties);
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testTransactionNotificationDelegateTransactionNotFound(CapturedOutput output)
            throws Exception {
        when(execution.getVariable("transactionId")).thenReturn(transaction.getId());
        when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.empty());

        delegate.execute(execution);
        String outputString = output.getOut();
        assertTrue(
                outputString.contains(
                        String.format(
                                "Transaction %s was not found and notification could not be sent.",
                                transaction.getId())));
        verify(sendNotificationService, never()).sendTransactionNotification(any(), any(), any());
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testTransactionNotificationDelegateMissingCamundaProperties(CapturedOutput output)
            throws Exception {
        when(execution.getVariable("transactionId")).thenReturn(transaction.getId());
        when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        CamundaProperties camundaProperties = Mockito.mock(CamundaProperties.class);
        mockBpmnModelElementInstance(camundaProperties);

        delegate.execute(execution);
        String outputString = output.getOut();
        assertTrue(
                outputString.contains(
                        String.format(
                                "Camunda properties are missing, could not send notification for"
                                        + " %s",
                                transaction.getId())));
        verify(sendNotificationService, never()).sendTransactionNotification(any(), any(), any());
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
                .thenReturn(List.of(camundaProperties));
    }

    private void mockCamundaProperty(CamundaProperties camundaProperties) {
        CamundaProperty camundaPropertyNotificationKey = Mockito.mock(CamundaProperty.class);
        CamundaProperty camundaPropertyNotificationParameterTransactionId =
                Mockito.mock(CamundaProperty.class);
        Mockito.when(camundaProperties.getCamundaProperties())
                .thenReturn(
                        List.of(
                                camundaPropertyNotificationKey,
                                camundaPropertyNotificationParameterTransactionId));
        Mockito.when(camundaPropertyNotificationKey.getCamundaName())
                .thenReturn("notification.key");
        Mockito.when(camundaPropertyNotificationKey.getCamundaValue()).thenReturn("TestTemplate");
        Mockito.when(camundaPropertyNotificationParameterTransactionId.getCamundaName())
                .thenReturn("notification.parameter.transactionId");
        Mockito.when(camundaPropertyNotificationParameterTransactionId.getCamundaValue())
                .thenReturn("id");
    }
}
