package io.nuvalence.workmanager.service.domain.transaction;

/**
 * Failure when a referenced task does not exist on a transaction.
 */
public class MissingTaskException extends Exception {

    private static final long serialVersionUID = 7047403749507689054L;

    /**
     * Constructs a new MissingTaskException with relevant information.
     *
     * @param transaction transaction that is missing requested task
     * @param taskId      ID of requested task
     */
    public MissingTaskException(final Transaction transaction, final String taskId) {
        super(
                String.format(
                        "Unable to find task with key [%s] in transaction with ID %s",
                        taskId, transaction.getId()));
    }
}
