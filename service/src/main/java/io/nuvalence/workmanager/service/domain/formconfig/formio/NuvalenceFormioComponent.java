package io.nuvalence.workmanager.service.domain.formconfig.formio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Altered version of the Form.io specification for components,
 * representing a form field or a group of fields.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NuvalenceFormioComponent {
    private String key;
    private String type;
    private boolean input;
    private NuvalenceFormioComponentProperties props;
    private List<NuvalenceFormioComponent> components;
    private NuvalenceFormioValidationConfiguration validators;
    private Map<String, String> expressions;
    private boolean keyContextProvider;
}
