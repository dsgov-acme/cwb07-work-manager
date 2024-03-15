package io.nuvalence.workmanager.service.domain.formconfig.formio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Form.io option for a select field
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NuvalenceFormioComponentOption {
    private String key;
    private String displayTextValue;
}
