package io.nuvalence.workmanager.service.utils.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utils for handling Camunda properties.
 */
public class CamundaPropertiesUtils {

    private CamundaPropertiesUtils() {
        throw new AssertionError(
                "Utility class should not be instantiated, use the static methods.");
    }

    /**
     * Gets a Camunda extension property.
     *
     * @param propertyName name of the desired property.
     * @param execution the delegate execution.
     * @return an optional of the value of the property.
     */
    public static Optional<String> getExtensionProperty(
            String propertyName, DelegateExecution execution) {

        return getExtensionProperty(propertyName, execution.getBpmnModelElementInstance());
    }

    /**
     * Gets a Camunda extension property.
     *
     * @param propertyName name of the desired property.
     * @param element Camunda base element.
     * @return an optional of the value of the property.
     */
    public static Optional<String> getExtensionProperty(String propertyName, BaseElement element) {
        return element
                .getExtensionElements()
                .getElementsQuery()
                .filterByType(CamundaProperties.class)
                .list()
                .stream()
                .flatMap(properties -> properties.getCamundaProperties().stream())
                .filter(
                        property ->
                                property.getCamundaName() != null
                                        && property.getCamundaName().equals(propertyName))
                .map(CamundaProperty::getCamundaValue)
                .findFirst();
    }

    /**
     * Gets Camunda properties with a given prefix.
     *
     * @param prefix Prefix if the properties' names.
     * @param execution Camunda execution.
     *
     * @return Map of found variables.
     */
    public static Map<String, String> getExtensionPropertiesWithPrefix(
            String prefix, DelegateExecution execution) {

        return getExtensionPropertiesWithPrefix(prefix, execution.getBpmnModelElementInstance());
    }

    /**
     * Gets Camunda properties with a given prefix.
     *
     * @param prefix Prefix if the properties' names.
     * @param element Camunda base element.
     * @return Map of found variables.
     */
    public static Map<String, String> getExtensionPropertiesWithPrefix(
            String prefix, BaseElement element) {
        return element
                .getExtensionElements()
                .getElementsQuery()
                .filterByType(CamundaProperties.class)
                .list()
                .stream()
                .flatMap(properties -> properties.getCamundaProperties().stream())
                .filter(
                        property ->
                                property.getCamundaName() != null
                                        && property.getCamundaName().startsWith(prefix))
                .collect(
                        Collectors.toMap(
                                property -> property.getCamundaName().substring(prefix.length()),
                                CamundaProperty::getCamundaValue));
    }
}
