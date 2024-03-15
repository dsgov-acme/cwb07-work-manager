package io.nuvalence.workmanager.service.domain.transaction;

import java.util.UUID;

/**
 * Failure when a referenced transaction cannot be retrieved.
 */
public class MissingTransactionException extends Exception {
    private static final long serialVersionUID = 6419138342625649821L;

    public MissingTransactionException(UUID transactionId) {
        super("Missing transaction reference with ID: " + transactionId.toString());
    }
}
