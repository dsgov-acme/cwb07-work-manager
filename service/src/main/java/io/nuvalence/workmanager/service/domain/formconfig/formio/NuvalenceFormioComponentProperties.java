package io.nuvalence.workmanager.service.domain.formconfig.formio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Altered version of the Form.io specification for component's props,
 * contains properties that configure the behavior of the field
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NuvalenceFormioComponentProperties {
    private boolean required;
    private BigDecimal max;
    private BigDecimal min;
    private Integer maxLength;
    private Integer minLength;
    private String pattern;
    private LocalDate minDate;
    private LocalDate maxDate;
    private String relativeMinDate;
    private String relativeMaxDate;
    private String formErrorLabel; // custom error message
    private List<NuvalenceFormioComponentOption> selectOptions;
}
