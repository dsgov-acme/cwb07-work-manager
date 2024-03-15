/*package io.nuvalence.workmanager.service.camunda.delegates;


@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class SimpleTransactionAuditEventDelegateTest {

    private AuditEventService transactionAuditEventService;

    private DelegateExecution execution;

    private SimpleTransactionAuditEventDelegate service;

    private TransactionService transactionService;

    private RequestContextTimestamp requestContextTimestamp;

    @BeforeEach
    void setUp() {
        transactionAuditEventService = mock(AuditEventService.class);
        execution = mock(DelegateExecution.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        transactionService = mock(TransactionService.class);

        requestContextTimestamp = mock(RequestContextTimestamp.class);

        SecurityContextHolder.setContext(securityContext);

        service =
                new SimpleTransactionAuditEventDelegate(
                        transactionAuditEventService, transactionService);
    }

    @Test
    void testExecute() throws Exception {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID transactionId = UUID.randomUUID();
            OffsetDateTime contextTimestamp = OffsetDateTime.now();
            String originatorId = UUID.randomUUID().toString();

            String userId = UUID.randomUUID().toString();
            String externalId = "externalId";
            String status = "status";
            Transaction transaction =
                    Transaction.builder()
                            .id(transactionId)
                            .lastUpdatedTimestamp(contextTimestamp)
                            .subjectUserId(userId)
                            .externalId(externalId)
                            .status(status)
                            .build();
            Optional<Transaction> optionalTransaction = Optional.of(transaction);

            when(execution.getVariable("transactionId")).thenReturn(transactionId);
            when(transactionService.getTransactionById(transactionId))
                    .thenReturn(optionalTransaction);
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(originatorId);
            CamundaProperties camundaProperties = Mockito.mock(CamundaProperties.class);
            mockBpmnModelElementInstance(camundaProperties);
            mockCamundaProperty(camundaProperties);

            service.execute(execution);

            String summary =
                    String.format(
                            "Recording audit event for submission of transaction %s, with"
                                    + " externalId %s and previous status %s",
                            transactionId, externalId, status);
            TransactionSubmittedAuditEventDto transactionSubmittedAuditEventDto =
                    new TransactionSubmittedAuditEventDto(originatorId);

            verify(transactionAuditEventService, times(1))
                    .sendActivityAuditEvent(
                            originatorId,
                            originatorId,
                            summary,
                            transactionId,
                            AuditEventBusinessObject.TRANSACTION,
                            transactionSubmittedAuditEventDto.toJson(),
                            AuditActivityType.TRANSACTION_SUBMITTED);
        }
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testExecuteMissingTransaction(CapturedOutput output) throws Exception {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID transactionId = UUID.randomUUID();
            String originatorId = UUID.randomUUID().toString();
            when(execution.getVariable("transactionId")).thenReturn(transactionId);
            when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(originatorId);
            service.execute(execution);
            verify(execution).getVariable("transactionId");
            verify(transactionService).getTransactionById(transactionId);

            String outputString = output.getOut();

            assertTrue(
                    outputString.contains(
                            String.format(
                                    "Transaction %s not found, could not post transaction submitted"
                                            + " audit event.",
                                    transactionId)));
            verify(transactionAuditEventService, never())
                    .sendActivityAuditEvent(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void testExecuteExceptionPath() throws Exception {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID transactionId = UUID.randomUUID();
            String originatorId = UUID.randomUUID().toString();
            when(execution.getVariable("transactionId")).thenReturn(transactionId);
            when(transactionService.getTransactionById(any())).thenThrow(RuntimeException.class);
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(originatorId);
            service.execute(execution);
            verify(transactionAuditEventService, never())
                    .sendActivityAuditEvent(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testExecuteMissingCamundaProperties(CapturedOutput output) throws Exception {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID transactionId = UUID.randomUUID();
            OffsetDateTime contextTimestamp = OffsetDateTime.now();
            String originatorId = UUID.randomUUID().toString();

            String userId = UUID.randomUUID().toString();
            String externalId = "externalId";
            String status = "status";
            Transaction transaction =
                    Transaction.builder()
                            .id(transactionId)
                            .lastUpdatedTimestamp(contextTimestamp)
                            .subjectUserId(userId)
                            .externalId(externalId)
                            .status(status)
                            .build();
            Optional<Transaction> optionalTransaction = Optional.of(transaction);

            when(execution.getVariable("transactionId")).thenReturn(transactionId);
            when(transactionService.getTransactionById(transactionId))
                    .thenReturn(optionalTransaction);
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(originatorId);
            CamundaProperties camundaProperties = Mockito.mock(CamundaProperties.class);
            mockBpmnModelElementInstance(camundaProperties);
            CamundaProperty camundaPropertyType = Mockito.mock(CamundaProperty.class);
            CamundaProperty camundaPropertySummary = Mockito.mock(CamundaProperty.class);
            Mockito.when(camundaProperties.getCamundaProperties())
                    .thenReturn(List.of(camundaPropertyType, camundaPropertySummary));
            Mockito.when(camundaPropertyType.getCamundaName()).thenReturn("");
            Mockito.when(camundaPropertySummary.getCamundaName()).thenReturn("summary");
            Mockito.when(camundaPropertySummary.getCamundaValue())
                    .thenReturn(
                            "Recording audit event for submission of transaction {{transactionId}},"
                                + " with externalId {{externalId}} and previous status {{status}}");

            service.execute(execution);
            verify(execution).getVariable("transactionId");
            verify(transactionService).getTransactionById(transactionId);

            String outputString = output.getOut();

            assertTrue(
                    outputString.contains(
                            String.format(
                                    "Camunda properties are missing, could not post transaction"
                                            + " submitted audit event for %s.",
                                    transactionId)));
            verify(transactionAuditEventService, never())
                    .sendActivityAuditEvent(any(), any(), any(), any(), any(), any(), any());
        }
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
                                .singleResult())
                .thenReturn(camundaProperties);
    }

    private void mockCamundaProperty(CamundaProperties camundaProperties) {
        CamundaProperty camundaPropertyType = Mockito.mock(CamundaProperty.class);
        CamundaProperty camundaPropertySummary = Mockito.mock(CamundaProperty.class);
        Mockito.when(camundaProperties.getCamundaProperties())
                .thenReturn(List.of(camundaPropertyType, camundaPropertySummary));
        Mockito.when(camundaPropertyType.getCamundaName()).thenReturn("activity_event_type");
        Mockito.when(camundaPropertyType.getCamundaValue()).thenReturn("transaction_submitted");
        Mockito.when(camundaPropertySummary.getCamundaName()).thenReturn("summary");
        Mockito.when(camundaPropertySummary.getCamundaValue())
                .thenReturn(
                        "Recording audit event for submission of transaction {{transactionId}},"
                                + " with externalId {{externalId}} and previous status {{status}}");
    }
}
*/
