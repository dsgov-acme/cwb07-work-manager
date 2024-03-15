package io.nuvalence.workmanager.service.domain.formconfig.formio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Altered version of the Form.io specification for component's validators,
 * specifies validation rules for the field
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NuvalenceFormioValidationConfiguration {
    private List<String> validation;
}
