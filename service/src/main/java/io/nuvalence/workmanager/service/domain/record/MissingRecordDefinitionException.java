package io.nuvalence.workmanager.service.domain.record;

import java.io.Serial;

/**
 * Failure when a referenced record definition cannot be retrieved.
 */
public class MissingRecordDefinitionException extends Exception {
    @Serial private static final long serialVersionUID = -6414900533582L;

    /**
     * Constructs new MissingRecordDefinitionException.
     *
     * @param recordDefinitionKey missing record definition key
     */
    public MissingRecordDefinitionException(String recordDefinitionKey) {
        super("Record references non-existent definition with Key: " + recordDefinitionKey);
    }
}
