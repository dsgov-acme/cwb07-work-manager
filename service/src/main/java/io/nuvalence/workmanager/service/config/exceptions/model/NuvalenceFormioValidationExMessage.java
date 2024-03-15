package io.nuvalence.workmanager.service.config.exceptions.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Class that formats the return of the rest endpoints to each item when there are FormIO validation errors.
 */
@Builder
@Data
public class NuvalenceFormioValidationExMessage implements Serializable {
    private static final long serialVersionUID = 2405172041950251807L;

    private List<NuvalenceFormioValidationExItem> formioValidationErrors;
}
