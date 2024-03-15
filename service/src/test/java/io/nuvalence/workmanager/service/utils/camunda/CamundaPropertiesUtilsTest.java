package io.nuvalence.workmanager.service.utils.camunda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CamundaPropertiesUtilsTest {

    @ParameterizedTest
    @CsvSource({"SUCCESS", "PROPERTY_NOT_FOUND", "NO_EXTENSION_ELEMENTS"})
    void testGetExtensionProperty(String testCase) {
        // Arrange
        String propertyName = "testProperty";
        String propertyValue = "testValue";

        FlowElement flowElement = Mockito.mock(FlowElement.class);
        CamundaProperties camundaProperties = Mockito.mock(CamundaProperties.class);
        mockBpmnModelElementInstance(List.of(camundaProperties), flowElement, testCase);
        mockCamundaProperty(
                List.of(camundaProperties),
                List.of(propertyName),
                List.of(propertyValue),
                testCase);

        // Act
        Optional<String> result =
                CamundaPropertiesUtils.getExtensionProperty(propertyName, flowElement);

        // Assert
        assertEquals(
                testCase.equals("SUCCESS") ? Optional.of(propertyValue) : Optional.empty(), result);
    }

    @ParameterizedTest
    @CsvSource({"SUCCESS", "PROPERTY_NOT_FOUND", "NO_EXTENSION_ELEMENTS"})
    void testGetExtensionPropertiesWithPrefix(String testCase) {
        // Arrange
        String prefix = "test.";
        String propertyName1 = "test.property1";
        String propertyValue1 = "testValue1";
        String propertyName2 = "test.property2";
        String propertyValue2 = "testValue2";

        List<String> propertiesName = List.of(propertyName1, propertyName2);
        List<String> propertiesValue = List.of(propertyValue1, propertyValue2);

        FlowElement flowElement = Mockito.mock(FlowElement.class);
        List<CamundaProperties> camundaPropertiesList =
                Stream.generate(() -> Mockito.mock(CamundaProperties.class))
                        .limit(propertiesName.size())
                        .collect(Collectors.toList());
        mockBpmnModelElementInstance(camundaPropertiesList, flowElement, testCase);
        mockCamundaProperty(camundaPropertiesList, propertiesName, propertiesValue, testCase);

        // Act
        Map<String, String> result =
                CamundaPropertiesUtils.getExtensionPropertiesWithPrefix(prefix, flowElement);

        // Assert
        Map<String, String> expected = new HashMap<>();

        if (testCase.equals("SUCCESS")) {
            expected.put(propertyName1.substring(prefix.length()), propertyValue1);
            expected.put(propertyName2.substring(prefix.length()), propertyValue2);
        }

        assertEquals(expected, result);
    }

    private void mockBpmnModelElementInstance(
            List<CamundaProperties> camundaPropertiesList, BaseElement element, String testCase) {

        Mockito.when(element.getExtensionElements())
                .thenReturn(Mockito.mock(ExtensionElements.class));
        Mockito.when(element.getExtensionElements().getElementsQuery())
                .thenReturn(Mockito.mock(Query.class));
        Mockito.when(
                        element.getExtensionElements()
                                .getElementsQuery()
                                .filterByType(CamundaProperties.class))
                .thenReturn(Mockito.mock(Query.class));
        Mockito.when(
                        element.getExtensionElements()
                                .getElementsQuery()
                                .filterByType(CamundaProperties.class)
                                .list())
                .thenReturn(
                        testCase.equals("PROPERTY_NOT_FOUND") ? List.of() : camundaPropertiesList);
    }

    private void mockCamundaProperty(
            List<CamundaProperties> camundaPropertiesList,
            List<String> propertiesName,
            List<String> propertiesValue,
            String testCase) {

        for (int i = 0; i < camundaPropertiesList.size(); i++) {
            CamundaProperty camundaPropertyNotificationKey = Mockito.mock(CamundaProperty.class);
            Mockito.when(camundaPropertiesList.get(i).getCamundaProperties())
                    .thenReturn(
                            testCase.equals("NO_EXTENSION_ELEMENTS")
                                    ? List.of()
                                    : List.of(camundaPropertyNotificationKey));

            Mockito.when(camundaPropertyNotificationKey.getCamundaName())
                    .thenReturn(propertiesName.get(i));
            Mockito.when(camundaPropertyNotificationKey.getCamundaValue())
                    .thenReturn(propertiesValue.get(i));
        }
    }
}
