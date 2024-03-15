package io.nuvalence.workmanager.service.domain.transaction;

/**
 * Failure when a referenced transaction definition cannot be retrieved.
 */
public class MissingTransactionDefinitionException extends Exception {
    private static final long serialVersionUID = -6363056564900533582L;

    /**
     * Constructs new MissingTransactionDefinitionException.
     *
     * @param transactionDefinitionKey missing transaction definition key
     */
    public MissingTransactionDefinitionException(String transactionDefinitionKey) {
        super(
                "Transaction references non-existent definition with Key: "
                        + transactionDefinitionKey);
    }
}
