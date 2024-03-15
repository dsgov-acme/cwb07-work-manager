package io.nuvalence.workmanager.service.config.exceptions;

import io.nuvalence.workmanager.service.config.exceptions.model.RecordLinkerExceptionMessage;
import lombok.Builder;
import lombok.Getter;

/**
 * TransactionRecordLinker exceptions.
 */
@Builder
@Getter
public class RecordLinkerException extends RuntimeException {
    static final long serialVersionUID = 489453413L;

    private final RecordLinkerExceptionMessage errorContents;
}
