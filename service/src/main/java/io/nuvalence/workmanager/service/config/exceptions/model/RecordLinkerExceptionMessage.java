package io.nuvalence.workmanager.service.config.exceptions.model;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * TransactionRecordLinker exceptions message.
 */
@Builder
@Getter
public class RecordLinkerExceptionMessage implements Serializable {

    static final long serialVersionUID = 394815178429529534L;

    private final String recordDefinitionKey;
    private final List<NuvalenceFormioValidationExItem> fieldKeyErrors;
    private final List<NuvalenceFormioValidationExItem> fieldValueErrors;
}
