package io.nuvalence.workmanager.service.utils.formconfig.formio;

import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponentOption;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.validator.GenericValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * DynaBeans validator for transactions based on a specific form configuration converted to a NuvalenceFormioComponent.
 */
@Slf4j
@Component
public class NuvalenceFormioValidator {

    /**
     * Transaction update dynaEntity validator.
     *
     * @param component Form configuration converted to the Nuvalence altered version of the Form.io specification
     * @param dynaEntity Data submitted when requesting a transaction update
     * @param formioValidationErrors list of validation errors found
     */
    public void validateDataAgainstFormConfig(
            NuvalenceFormioComponent component,
            DynamicEntity dynaEntity,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        validateDataAgainstFormConfig(component, dynaEntity, "", formioValidationErrors);
    }

    private void validateDataAgainstFormConfig(
            NuvalenceFormioComponent component,
            DynamicEntity dynaEntity,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        if (component.getKey() != null) {
            Pair<Object, DynaProperty> field = getFieldValue(component.getKey(), dynaEntity);

            // Validate expressions
            validateExpressions(component, dynaEntity, partialParentPath, formioValidationErrors);

            // Validate props
            validateComponentProps(component, field, partialParentPath, formioValidationErrors);

            // Apply validators
            applyValidators(component, field, partialParentPath, formioValidationErrors);

            // Validate List sub components
            if (field != null
                    && field.getSecond() != null
                    && field.getSecond().getType() != null
                    && field.getSecond().getType().equals(List.class)
                    && field.getSecond().getContentType() != null) {

                if (field.getSecond().getContentType().equals(DynamicEntity.class)
                        || field.getSecond().getContentType().equals(List.class)) {
                    List<Object> list = (List<Object>) field.getFirst();
                    validateList(list, component, partialParentPath, formioValidationErrors);
                }
                return;
            }
        }

        validateSubComponents(component, partialParentPath, formioValidationErrors, dynaEntity);
    }

    private void validateList(
            List<Object> list,
            NuvalenceFormioComponent component,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        for (int i = 0; i < list.size(); i++) {
            Object currentObject = list.get(i);
            if (currentObject instanceof DynamicEntity dynamicEntity) {
                String parentPath =
                        component.isKeyContextProvider()
                                ? component.getKey() + "[" + i + "]"
                                : partialParentPath;

                validateSubComponents(component, parentPath, formioValidationErrors, dynamicEntity);
            }
        }
    }

    private void validateExpressions(
            NuvalenceFormioComponent component,
            DynamicEntity dynaEntity,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        if (component.getExpressions() != null) {
            overrideValidationsBasedOnExpressions(
                    component, dynaEntity, partialParentPath, formioValidationErrors);
        }
    }

    private void applyValidators(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        if (component.getValidators() != null
                && component.getValidators().getValidation() != null) {
            for (String validation : component.getValidators().getValidation()) {
                if (validation.equals("email")
                        && fieldIsNotNull(field)
                        && field.getSecond().getType().equals(String.class)) {
                    String fieldValue = (String) field.getFirst();
                    if (!GenericValidator.isEmail(fieldValue)) {
                        addValidationError(
                                component, "email", partialParentPath, formioValidationErrors);
                    }
                }
            }
        }
    }

    private void validateSubComponents(
            NuvalenceFormioComponent component,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors,
            DynamicEntity dynaEntity) {
        if (component.getComponents() != null) {
            for (NuvalenceFormioComponent subComponent : component.getComponents()) {
                validateDataAgainstFormConfig(
                        subComponent, dynaEntity, partialParentPath, formioValidationErrors);
            }
        }
    }

    private void overrideValidationsBasedOnExpressions(
            NuvalenceFormioComponent component,
            DynamicEntity dynaEntity,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        final EntityMapper mapper = EntityMapper.getInstance();
        Map<String, Object> data = mapper.convertAttributesToGenericMap(dynaEntity);

        for (Map.Entry<String, String> expressionEntry : component.getExpressions().entrySet()) {
            String expressionKey = expressionEntry.getKey();
            String expressionValue = expressionEntry.getValue();

            if ((expressionKey.equals("hide") || expressionKey.equals("props.hidden"))
                    && evaluateExpression(
                            expressionValue,
                            data,
                            component,
                            partialParentPath,
                            formioValidationErrors)) {
                clearValidationProps(component);
            }
            if ((expressionKey.equals("require") || expressionKey.equals("props.required"))
                    && evaluateExpression(
                            expressionValue,
                            data,
                            component,
                            partialParentPath,
                            formioValidationErrors)) {
                component.getProps().setRequired(true);
            }
        }
    }

    private void clearValidationProps(NuvalenceFormioComponent component) {
        component.setProps(null);
        component.setExpressions(null);
        component.setValidators(null);

        if (component.getComponents() != null) {
            for (NuvalenceFormioComponent childComponent : component.getComponents()) {
                clearValidationProps(childComponent);
            }
        }
    }

    private boolean evaluateExpression(
            String expression,
            Map<String, Object> data,
            NuvalenceFormioComponent component,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        String normalizedExpression = expression.replace("?.", ".");
        final ScriptEngine javaScriptEngine =
                new ScriptEngineManager().getEngineByName("JavaScript");

        javaScriptEngine.put("model", data);
        try {
            return Boolean.valueOf(javaScriptEngine.eval(normalizedExpression).toString());
        } catch (ScriptException e) {
            log.debug("javascript expression error", e);
            addValidationError(component, "expression", partialParentPath, formioValidationErrors);

            return false;
        }
    }

    private void validateComponentProps(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        if (component.getProps() == null) {
            return;
        }

        validateRequiredField(component, field, partialParentPath, formioValidationErrors);

        if (!fieldIsNotNull(field)) {
            return;
        }

        validateMaxValue(component, field, partialParentPath, formioValidationErrors);
        validateMinValue(component, field, partialParentPath, formioValidationErrors);
        validateRelativeMaxDate(component, field, partialParentPath, formioValidationErrors);
        validateRelativeMinDate(component, field, partialParentPath, formioValidationErrors);
        validateMaxDate(component, field, partialParentPath, formioValidationErrors);
        validateMinDate(component, field, partialParentPath, formioValidationErrors);
        validateFieldLength(component, field, partialParentPath, formioValidationErrors, true);
        validateFieldLength(component, field, partialParentPath, formioValidationErrors, false);
        validatePattern(component, field, partialParentPath, formioValidationErrors);
        validateSelectOptions(component, field, partialParentPath, formioValidationErrors);
    }

    private void validateRequiredField(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        if (component.getProps().isRequired() && !isFieldPresent(field)) {
            addValidationError(component, "required", partialParentPath, formioValidationErrors);
        }
    }

    private void validateMaxValue(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        BigDecimal max = component.getProps().getMax();
        if (max == null) {
            return;
        }

        BigDecimal value = getNumericValue(field);
        if (value != null && value.compareTo(max) > 0) {
            addValidationError(component, "max", partialParentPath, formioValidationErrors);
        }
    }

    private void validateMinValue(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        BigDecimal min = component.getProps().getMin();
        if (min == null) {
            return;
        }

        BigDecimal value = getNumericValue(field);
        if (value != null && value.compareTo(min) < 0) {
            addValidationError(component, "min", partialParentPath, formioValidationErrors);
        }
    }

    private void validateRelativeMaxDate(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        validateRelativeDate(
                component,
                field,
                partialParentPath,
                formioValidationErrors,
                component.getProps().getRelativeMaxDate(),
                LocalDate::isAfter,
                "relativeMaxDate");
    }

    private void validateRelativeMinDate(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        validateRelativeDate(
                component,
                field,
                partialParentPath,
                formioValidationErrors,
                component.getProps().getRelativeMinDate(),
                LocalDate::isBefore,
                "relativeMinDate");
    }

    private void validateRelativeDate(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors,
            String relativeDate,
            DateValidationCondition validationCondition,
            String errorType) {

        if (relativeDate == null) {
            return;
        }

        String[] parts = relativeDate.replaceFirst("^-", "").split("-");
        int numberOfUnitInt =
                Integer.parseInt((relativeDate.startsWith("-") ? "-" : "") + parts[0]);
        String dateUnit = parts[1];

        LocalDate validationDate = LocalDate.now();

        switch (dateUnit) {
            case "day":
                validationDate = validationDate.plusDays(numberOfUnitInt);
                break;
            case "week":
                validationDate = validationDate.plusWeeks(numberOfUnitInt);
                break;
            case "month":
                validationDate = validationDate.plusMonths(numberOfUnitInt);
                break;
            case "year":
                validationDate = validationDate.plusYears(numberOfUnitInt);
                break;
            default:
                throw new IllegalArgumentException("Invalid date unit");
        }

        LocalDate value = (LocalDate) field.getFirst();

        if (validationCondition.test(value, validationDate)) {
            addValidationError(component, errorType, partialParentPath, formioValidationErrors);
        }
    }

    private void validateMaxDate(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        LocalDate max = component.getProps().getMaxDate();
        if (max == null) {
            return;
        }

        LocalDate value = (LocalDate) field.getFirst();
        if (value.isAfter(max)) {
            addValidationError(component, "maxDate", partialParentPath, formioValidationErrors);
        }
    }

    private void validateMinDate(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        LocalDate min = component.getProps().getMinDate();
        if (min == null) {
            return;
        }

        LocalDate value = (LocalDate) field.getFirst();
        if (value.isBefore(min)) {
            addValidationError(component, "minDate", partialParentPath, formioValidationErrors);
        }
    }

    private void validateFieldLength(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors,
            boolean isMaxLength) {
        Integer length =
                isMaxLength
                        ? component.getProps().getMaxLength()
                        : component.getProps().getMinLength();
        if (length == null || !field.getSecond().getType().equals(String.class)) {
            return;
        }

        String fieldValue = (String) field.getFirst();
        boolean condition =
                isMaxLength ? fieldValue.length() > length : fieldValue.length() < length;
        if (condition) {
            addValidationError(
                    component,
                    isMaxLength ? "maxLength" : "minLength",
                    partialParentPath,
                    formioValidationErrors);
        }
    }

    private void validatePattern(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        String pattern = component.getProps().getPattern();
        if (pattern == null || !field.getSecond().getType().equals(String.class)) {
            return;
        }

        String fieldValue = (String) field.getFirst();
        if (!GenericValidator.matchRegexp(fieldValue, pattern)) {
            addValidationError(component, "pattern", partialParentPath, formioValidationErrors);
        }
    }

    private void validateSelectOptions(
            NuvalenceFormioComponent component,
            Pair<Object, DynaProperty> field,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        List<NuvalenceFormioComponentOption> options = component.getProps().getSelectOptions();
        if (options == null || !field.getSecond().getType().equals(String.class)) {
            return;
        }

        String fieldValue = (String) field.getFirst();
        Optional<String> valueMatched =
                options.stream()
                        .map(NuvalenceFormioComponentOption::getKey)
                        .filter(option -> option.equals(fieldValue))
                        .findFirst();

        if (valueMatched.isEmpty()) {
            addValidationError(
                    component, "selectOptions", partialParentPath, formioValidationErrors);
        }
    }

    private void addValidationError(
            NuvalenceFormioComponent control,
            String errorName,
            String partialParentPath,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        if (!partialParentPath.isBlank()) {
            partialParentPath = partialParentPath + ".";
        }

        NuvalenceFormioValidationExItem errorItem =
                NuvalenceFormioValidationExItem.builder()
                        .controlName(partialParentPath + control.getKey())
                        .errorName(errorName)
                        .build();

        if (control.getProps() != null
                && control.getProps().getFormErrorLabel() != null
                && !control.getProps().getFormErrorLabel().isBlank()) {
            errorItem.setErrorMessage(control.getProps().getFormErrorLabel());
        }

        formioValidationErrors.add(errorItem);
    }

    private Pair<Object, DynaProperty> getFieldValue(String fieldPath, DynaBean dynaBean) {
        int dotIndex = fieldPath.indexOf(".");
        try {
            if (dotIndex == -1) {
                DynaProperty property = dynaBean.getDynaClass().getDynaProperty(fieldPath);
                return new Pair<>(dynaBean.get(fieldPath), property);
            } else {
                String firstFieldName = fieldPath.substring(0, dotIndex);
                String remainingFieldName = fieldPath.substring(dotIndex + 1);

                DynaProperty dynaProperty = dynaBean.getDynaClass().getDynaProperty(firstFieldName);
                Object fieldValue = dynaBean.get(dynaProperty.getName());

                if (fieldValue instanceof DynaBean dynaBeanField) {
                    return getFieldValue(remainingFieldName, dynaBeanField);
                } else {
                    return null;
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Field {} not property in the schema", fieldPath);
            return null;
        }
    }

    private boolean isFieldPresent(Pair<Object, DynaProperty> field) {
        boolean isPresent = false;
        if (fieldIsNotNull(field)) {
            if (field.getSecond().getType().equals(String.class)) {
                String fieldValue = (String) field.getFirst();
                isPresent = !fieldValue.isBlank();
            } else {
                isPresent = true;
            }
        }
        return isPresent;
    }

    private boolean fieldIsNotNull(Pair<Object, DynaProperty> field) {
        return field != null && field.getFirst() != null && field.getSecond().getType() != null;
    }

    private BigDecimal getNumericValue(Pair<Object, DynaProperty> field) {
        BigDecimal fieldValue = null;
        if (field.getSecond().getType().equals(Integer.class)) {
            fieldValue = new BigDecimal((Integer) field.getFirst());
        } else if (field.getSecond().getType().equals(BigDecimal.class)) {
            fieldValue = (BigDecimal) field.getFirst();
        }
        return fieldValue;
    }

    @FunctionalInterface
    interface DateValidationCondition {
        boolean test(LocalDate value, LocalDate validationDate);
    }
}
