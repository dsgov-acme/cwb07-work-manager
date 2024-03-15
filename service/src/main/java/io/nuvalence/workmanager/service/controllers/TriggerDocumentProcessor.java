package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.service.DocumentManagementService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Triggers the document processing for a transaction.
 */
@Slf4j
public class TriggerDocumentProcessor implements Callable<String> {

    private final UUID transactionId;
    private final TransactionService transactionService;
    private final SchemaService schemaService;
    private final DocumentManagementService documentManagementService;

    /**
     * Constructor for this class.
     *
     * @param transactionId ID of the transaction to process
     * @param transactionService Service level class for manipulating transaction objects
     * @param schemaService Service level class for manipulating schema objects
     * @param documentManagementService Service level class for manipulating document objects
     */
    public TriggerDocumentProcessor(
            UUID transactionId,
            TransactionService transactionService,
            SchemaService schemaService,
            DocumentManagementService documentManagementService) {
        this.transactionId = transactionId;
        this.transactionService = transactionService;
        this.schemaService = schemaService;
        this.documentManagementService = documentManagementService;
    }

    /**
     * Triggers the document processing for a transaction.
     *
     * @return null
     */
    @Override
    public String call() {
        transactionService
                .getTransactionById(transactionId)
                .ifPresent(
                        transaction -> {
                            List<CustomerProvidedDocument> customerProvidedDocuments =
                                    transaction.getCustomerProvidedDocuments();
                            final Schema schema = transaction.getData().getSchema();

                            for (CustomerProvidedDocument customerProvidedDocument :
                                    customerProvidedDocuments) {
                                List<String> processorsNames =
                                        schemaService.getDocumentProcessorsInSchemaPath(
                                                customerProvidedDocument.getDataPath(), schema);
                                if (CollectionUtils.isNotEmpty(processorsNames)) {
                                    log.debug(
                                            "Initiating document processing for document {} with"
                                                    + " processors {}",
                                            customerProvidedDocument.getId(),
                                            processorsNames);
                                    documentManagementService.initiateDocumentProcessing(
                                            customerProvidedDocument.getId(), processorsNames);
                                }
                            }
                        });
        return null;
    }
}
