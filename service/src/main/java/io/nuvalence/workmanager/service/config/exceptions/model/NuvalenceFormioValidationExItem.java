package io.nuvalence.workmanager.service.config.exceptions.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Class that formats the return from rest endpoints when there are FormIO validation errors.
 */
@Builder
@Data
public class NuvalenceFormioValidationExItem implements Serializable {

    private static final long serialVersionUID = 1905122041950251207L;

    private String controlName;
    private String errorName;
    private String errorMessage;
    private String formStepKey;
}
