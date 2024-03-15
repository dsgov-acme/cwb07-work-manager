package io.nuvalence.workmanager.service.controllers;

import static org.mockito.ArgumentMatchers.any;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.service.DocumentManagementService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionService;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TriggerDocumentProcessorTest {

    @Mock private TransactionService transactionService;
    @Mock private SchemaService schemaService;
    @Mock private DocumentManagementService documentManagementService;

    @Test
    void testCall() {
        UUID transactionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        List<CustomerProvidedDocument> customerProvidedDocuments =
                List.of(
                        CustomerProvidedDocument.builder()
                                .id(documentId)
                                .dataPath("dataPath")
                                .build());
        Schema schema = Mockito.mock(Schema.class);
        DynaProperty[] dynaPropertines = {};
        Mockito.when(schema.getDynaProperties()).thenReturn(dynaPropertines);

        DynamicEntity dynamicEntity = new DynamicEntity(schema);
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .data(dynamicEntity)
                        .customerProvidedDocuments(customerProvidedDocuments)
                        .build();
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        List<String> processors = List.of("processor1", "processor2");
        Mockito.when(schemaService.getDocumentProcessorsInSchemaPath(any(), any()))
                .thenReturn(processors);

        TriggerDocumentProcessor triggerDocumentProcessor =
                new TriggerDocumentProcessor(
                        transactionId,
                        transactionService,
                        schemaService,
                        documentManagementService);

        triggerDocumentProcessor.call();

        Mockito.verify(documentManagementService, Mockito.times(1))
                .initiateDocumentProcessing(documentId, processors);
    }

    @Test
    void testCall_no_documents() {
        UUID transactionId = UUID.randomUUID();
        List<CustomerProvidedDocument> customerProvidedDocuments = List.of();
        Schema schema = Mockito.mock(Schema.class);
        DynaProperty[] dynaPropertines = {};
        Mockito.when(schema.getDynaProperties()).thenReturn(dynaPropertines);

        DynamicEntity dynamicEntity = new DynamicEntity(schema);
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .data(dynamicEntity)
                        .customerProvidedDocuments(customerProvidedDocuments)
                        .build();
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        TriggerDocumentProcessor triggerDocumentProcessor =
                new TriggerDocumentProcessor(
                        transactionId,
                        transactionService,
                        schemaService,
                        documentManagementService);

        triggerDocumentProcessor.call();

        Mockito.verify(documentManagementService, Mockito.never())
                .initiateDocumentProcessing(any(), any());
    }

    @Test
    void testCall_no_processors() {
        UUID transactionId = UUID.randomUUID();
        List<CustomerProvidedDocument> customerProvidedDocuments =
                List.of(CustomerProvidedDocument.builder().dataPath("dataPath").build());
        Schema schema = Mockito.mock(Schema.class);
        DynaProperty[] dynaPropertines = {};
        Mockito.when(schema.getDynaProperties()).thenReturn(dynaPropertines);

        DynamicEntity dynamicEntity = new DynamicEntity(schema);
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .data(dynamicEntity)
                        .customerProvidedDocuments(customerProvidedDocuments)
                        .build();
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        Mockito.when(schemaService.getDocumentProcessorsInSchemaPath(any(), any()))
                .thenReturn(List.of());

        TriggerDocumentProcessor triggerDocumentProcessor =
                new TriggerDocumentProcessor(
                        transactionId,
                        transactionService,
                        schemaService,
                        documentManagementService);

        triggerDocumentProcessor.call();

        Mockito.verify(documentManagementService, Mockito.never())
                .initiateDocumentProcessing(any(), any());
    }
}
