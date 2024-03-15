package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.transaction.AllowedLink;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionDefinitionException;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLink;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkNotAllowedException;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkType;
import io.nuvalence.workmanager.service.generated.models.LinkedTransaction;
import io.nuvalence.workmanager.service.repository.TransactionLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Service layer to manage transaction links.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class TransactionLinkService {
    private final TransactionDefinitionService transactionDefinitionService;
    private final AllowedLinkService allowedLinkService;
    private final TransactionService transactionService;
    private final TransactionLinkRepository repository;

    private final TransactionLinkTypeService transactionLinkTypeService;

    /**
     * Create a link between two transactions.
     * Get the TransactionDefinition of the 2 Transactions by id
     * If both transactions have the same definition then o.k.
     * Else lookup definitions and make sure that there is an AllowedLink that associates the 2
     *
     * @param transactionLink       Transaction link request
     * @param transactionLinkTypeId The transaction link type to map t0
     * @return Created transaction link
     * @throws MissingTransactionException if link references missing transaction
     * @throws MissingTransactionDefinitionException If any of the transactions have missing transaction definition
     * @throws TransactionLinkNotAllowedException    If the two transactions are not allowed to be linked
     */
    public TransactionLink saveTransactionLink(
            TransactionLink transactionLink, UUID transactionLinkTypeId)
            throws MissingTransactionException, TransactionLinkNotAllowedException,
                    MissingTransactionDefinitionException {

        if (transactionLink.getFromTransactionId().equals(transactionLink.getToTransactionId())) {
            throw new TransactionLinkNotAllowedException(
                    transactionLink.getFromTransactionId(), transactionLink.getToTransactionId());
        }

        // get the transactions
        Transaction fromTransaction = fetchTransaction(transactionLink.getFromTransactionId());
        Transaction toTransaction = fetchTransaction(transactionLink.getToTransactionId());

        boolean allowed = checkIfTransactionsAllowed(fromTransaction, toTransaction);

        TransactionLinkType linkType =
                transactionLinkTypeService
                        .getTransactionLinkTypeById(transactionLinkTypeId)
                        .orElse(null);
        transactionLink.setTransactionLinkType(linkType);

        if (!allowed) {
            throw new TransactionLinkNotAllowedException(
                    transactionLink.getFromTransactionId(), transactionLink.getToTransactionId());
        }
        return repository.save(transactionLink);
    }

    private boolean checkIfTransactionsAllowed(
            Transaction fromTransaction, Transaction toTransaction)
            throws MissingTransactionDefinitionException {

        // if the same definition o.k
        if (fromTransaction
                .getTransactionDefinitionKey()
                .equals(toTransaction.getTransactionDefinitionKey())) {
            return true;
        } else {
            // get definitions
            TransactionDefinition fromDefinition =
                    fetchTransactionDefinition(fromTransaction.getTransactionDefinitionKey());
            TransactionDefinition toDefinition =
                    fetchTransactionDefinition(toTransaction.getTransactionDefinitionKey());

            if (toDefinition == null) {
                throw new MissingTransactionDefinitionException(
                        toTransaction.getTransactionDefinitionKey());
            }
            // compare their allowed links
            List<AllowedLink> fromAllowedLinks =
                    allowedLinkService.getAllowedLinksByDefinitionKey(fromDefinition.getKey());
            List<AllowedLink> toAllowedLinks =
                    allowedLinkService.getAllowedLinksByDefinitionKey(toDefinition.getKey());
            for (AllowedLink fromLink : fromAllowedLinks) {
                for (AllowedLink toLink : toAllowedLinks) {
                    if (fromLink.getTransactionLinkType()
                            .getId()
                            .equals(toLink.getTransactionLinkType().getId())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private TransactionDefinition fetchTransactionDefinition(String transactionDefinitionKey)
            throws MissingTransactionDefinitionException {
        return transactionDefinitionService
                .getTransactionDefinitionByKey(transactionDefinitionKey)
                .orElseThrow(
                        () -> new MissingTransactionDefinitionException(transactionDefinitionKey));
    }

    private Transaction fetchTransaction(UUID transactionId) throws MissingTransactionException {

        return transactionService
                .getTransactionById(transactionId)
                .orElseThrow(() -> new MissingTransactionException(transactionId));
    }

    /**
     * Get the linked transactions for a transaction id.
     *
     * @param id Transaction to get links for
     * @return List of linked transactions
     */
    public List<LinkedTransaction> getLinkedTransactionsById(UUID id) {
        List<LinkedTransaction> results = new ArrayList<>();
        List<TransactionLink> transactionLinks = repository.getTransactionLinksById(id);
        for (TransactionLink transactionLink : transactionLinks) {
            LinkedTransaction linkedTransaction = new LinkedTransaction();
            // create the association (from/to) and the linked transaction based on this
            // transaction's id
            if (transactionLink.getFromTransactionId().equals(id)) {
                linkedTransaction.setLinkedTransactionId(transactionLink.getToTransactionId());
                linkedTransaction.setDescription(
                        transactionLink.getTransactionLinkType().getFromDescription());
            } else if (transactionLink.getToTransactionId().equals(id)) {
                linkedTransaction.setLinkedTransactionId(transactionLink.getFromTransactionId());
                linkedTransaction.setDescription(
                        transactionLink.getTransactionLinkType().getToDescription());
            }
            results.add(linkedTransaction);
        }
        return results;
    }

    /**
     * Removes the transaction links that match the param filters.
     *
     * @param transactionLinkTypeId The transaction link type to map to
     * @param fromTransactionId ID of the transaction to linking from
     * @param toTransactionId ID of the transaction to linking to
     * @throws NotFoundException If the there is no transaction links that match the param filters
     */
    public void removeTransactionLink(
            UUID transactionLinkTypeId, UUID fromTransactionId, UUID toTransactionId)
            throws NotFoundException {

        List<TransactionLink> transactionLinks =
                repository.getTransactionLinkByFromAndToAndType(
                        fromTransactionId, toTransactionId, transactionLinkTypeId);

        if (transactionLinks.isEmpty()) {
            throw new NotFoundException("Resource not found");
        }

        transactionLinks.forEach(repository::delete);
    }
}
