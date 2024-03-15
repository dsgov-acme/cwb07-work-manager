package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationExportModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FormConfigurationMapperTest {

    private FormConfigurationMapper mapper;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mapper = FormConfigurationMapper.INSTANCE;
    }

    @Test
    void testMapConfigurationAttributes() {

        Map<String, Object> config =
                Map.of(
                        "components",
                        List.of(
                                new HashMap<>(
                                        Map.of(
                                                "key",
                                                "child-component",
                                                "components",
                                                List.of(
                                                        Map.of(
                                                                "key",
                                                                "grandchild-component",
                                                                "input",
                                                                false))))));

        Map<String, Object> mappedConfiguration = mapper.mapConfigurationAttribute(config);

        var parentComponents = assertInstanceOf(List.class, mappedConfiguration.get("components"));
        var parent = assertInstanceOf(Map.class, parentComponents.get(0));
        assertTrue((boolean) parent.get("input"));

        var childComponents = assertInstanceOf(List.class, parent.get("components"));
        var child = assertInstanceOf(Map.class, childComponents.get(0));
        assertFalse((boolean) child.get("input"));
    }

    @Test
    void formConfigToValidationConfigExportModelNullConfiguration() {
        FormConfigurationExportModel formConfigurationExportModel =
                mapper.formConfigurationToFormConfigurationExportModel(null);
        assertNull(formConfigurationExportModel);
    }

    @Test
    void formConfigToValidationConfig_NestedThenFlat() throws IOException {
        var configString =
                """
                {
                        "configuration": {
                            "components": [
                                {
                                    "components": [
                                        {
                                            "key": "personalInformation.firstName"
                                        },
                                        {
                                            "key": "personalInformation.middleName"
                                        },
                                        {
                                            "key": "personalInformation.currentAddress.city"
                                        },
                                        {
                                            "key": "personalInformation.currentAddress.country"
                                        },
                                        {
                                            "key": "personalInformation.mailingAddress.city"
                                        },
                                        {
                                            "key": "personalInformation.mailingAddress.country"
                                        }
                                    ],
                                    "key": "personalInformation"
                                }
                            ]
                        }
                }
                """;
        var config = objectMapper.readValue(configString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig = mapper.formConfigToValidationConfig(config);

        assertEquals(1, normalizedConfig.getComponents().size());
        var componentsRoot = normalizedConfig.getComponents().get(0).getComponents();
        assertEquals(4, componentsRoot.size());
        assertEquals("personalInformation.firstName", componentsRoot.get(1).getKey());
        assertEquals(
                "personalInformation.currentAddress.city",
                componentsRoot.get(0).getComponents().get(0).getKey());
    }

    @Test
    void formConfigToValidationConfig_FlatThenNested() throws IOException {
        var configsString =
                """
                {
                        "configuration": {
                                "components": [
                                {
                                        "components": [
                                        {
                                                "key": "personalInformation.currentAddress.city"
                                        },
                                        {
                                                "key": "personalInformation.currentAddress.country"
                                        }
                                        ],
                                        "key": "personalInformation.currentAddress"
                                }
                                ]
                        }
                }
                """;

        var config = objectMapper.readValue(configsString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig = mapper.formConfigToValidationConfig(config);

        assertEquals(1, normalizedConfig.getComponents().size());
        var componentsRoot = normalizedConfig.getComponents().get(0).getComponents();
        assertEquals(1, componentsRoot.size());
        assertEquals(
                "personalInformation.currentAddress.city",
                componentsRoot.get(0).getComponents().get(0).getKey());
        assertEquals(
                "personalInformation.currentAddress.country",
                componentsRoot.get(0).getComponents().get(1).getKey());
    }

    @Test
    void formConfigToValidationConfig_OnlyFlat() throws IOException {

        var configsString =
                """
                {
                        "configuration": {
                            "components": [
                                {
                                    "key": "personalInformation.currentAddress.city"
                                },
                                {
                                    "key": "personalInformation.mailingAddress.city"
                                }
                            ]
                        }
                }
                """;
        var config = objectMapper.readValue(configsString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig = mapper.formConfigToValidationConfig(config);

        assertEquals(1, normalizedConfig.getComponents().size());
        var componentsRoot = normalizedConfig.getComponents().get(0).getComponents();
        assertEquals(2, componentsRoot.size());
        assertEquals(
                "personalInformation.currentAddress.city",
                componentsRoot.get(0).getComponents().get(0).getKey());
        assertEquals(
                "personalInformation.mailingAddress.city",
                componentsRoot.get(1).getComponents().get(0).getKey());
    }

    @Test
    void formConfigToValidationConfig_ParentMismatching() throws IOException {

        var configsString =
                """
                {
                        "configuration": {
                            "components": [
                                {
                                    "key": "personalInformation.mailingAddress.something.city"
                                },
                                {
                                    "components": [
                                        {
                                            "key": "personalInformation.currentAddress.something.city"
                                        },
                                        {
                                            "key": "personalInformation.currentAddress",
                                            "props": {
                                                "pattern": "patternToCheck"
                                            }
                                        },
                                        {
                                            "key": "zrootkey",
                                            "props": {
                                                "pattern": "secondPatternToCheck"
                                            }
                                        }
                                    ],
                                    "key": "aGivenTester"
                                }
                            ]
                        }
                }
                """;
        var config = objectMapper.readValue(configsString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig = mapper.formConfigToValidationConfig(config);

        assertEquals(3, normalizedConfig.getComponents().size());

        assertEquals("aGivenTester", normalizedConfig.getComponents().get(0).getKey());
        assertNull(normalizedConfig.getComponents().get(0).getComponents());

        var componentsRoot = normalizedConfig.getComponents().get(1).getComponents();
        assertEquals(2, componentsRoot.size());
        assertEquals(
                "personalInformation.currentAddress.something.city",
                componentsRoot.get(0).getComponents().get(0).getComponents().get(0).getKey());

        assertEquals("patternToCheck", componentsRoot.get(0).getProps().getPattern());

        assertEquals(
                "personalInformation.mailingAddress.something.city",
                componentsRoot.get(1).getComponents().get(0).getComponents().get(0).getKey());

        assertEquals("zrootkey", normalizedConfig.getComponents().get(2).getKey());
        assertEquals(
                "secondPatternToCheck",
                normalizedConfig.getComponents().get(2).getProps().getPattern());
    }

    @Test
    void formConfigToValidationConfig_IgnorableElement() throws IOException {
        String configString =
                """
                {
                        "configuration": {
                                "components": [
                                {
                                        "components": [
                                        {
                                                "input": true,
                                                "key": "personalInformation.firstName"
                                        }
                                        ],
                                        "input": true,
                                        "key": "personalInformation"
                                },
                                {
                                        "components": [
                                        {
                                                "components": [
                                                {
                                                        "input": true,
                                                        "key": "documents.proofOfResidency"
                                                }
                                                ],
                                                "input": true,
                                                "key": "proofOfResidency"
                                        }
                                        ],
                                        "input": true,
                                        "key": "documents"
                                }
                                ]
                        }
                }
                """;

        var config = objectMapper.readValue(configString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig = mapper.formConfigToValidationConfig(config);

        assertEquals(3, normalizedConfig.getComponents().size());
        assertEquals(true, normalizedConfig.getComponents().get(0).isInput());
        assertEquals(true, normalizedConfig.getComponents().get(1).isInput());
        assertEquals(false, normalizedConfig.getComponents().get(2).isInput());
    }

    @Test
    void formConfigToValidationConfig_ForStepCombiningFirstLevelKeys() throws IOException {

        var config = configForStepsCombiningFirstLevelKeys();

        NuvalenceFormioComponent normalizedConfig =
                mapper.formConfigToValidationConfigForStep(config, "step2", true);

        assertEquals(3, normalizedConfig.getComponents().size());
        var components1 = normalizedConfig.getComponents().get(0).getComponents();
        assertEquals(1, components1.size());
        assertEquals("documents.jobCertificate", components1.get(0).getKey());

        var components2 = normalizedConfig.getComponents().get(1).getComponents();
        assertEquals(1, components2.size());
        assertEquals("employmentInformation.previousEmployment", components2.get(0).getKey());

        var components2child = components2.get(0).getComponents();
        assertEquals(1, components2child.size());
        assertEquals(
                "employmentInformation.previousEmployment.companyName",
                components2child.get(0).getKey());

        var childlessComponent = normalizedConfig.getComponents().get(2);
        assertNull(childlessComponent.getComponents());
        assertEquals(false, childlessComponent.isInput());
    }

    @Test
    void formConfigToValidationConfig_ForStepCombiningFirstLevelKeys_NotFoundStep()
            throws IOException {

        var config = configForStepsCombiningFirstLevelKeys();

        var exception =
                assertThrows(
                        ProvidedDataException.class,
                        () -> mapper.formConfigToValidationConfigForStep(config, "step4", false));

        assertEquals(
                "The provided formStepKey does not exist in the form configuration",
                exception.getMessage());
    }

    private FormConfiguration configForStepsCombiningFirstLevelKeys() throws IOException {
        var configsString =
                """
                {
                        "configuration": {
                                "components": [
                                {
                                        "key": "step1",
                                        "components": [
                                        {
                                                "key": "personalInformation.currentAddress.city"
                                        },
                                        {
                                                "key": "documents.id"
                                        }
                                        ]
                                },
                                {
                                        "key": "step2",
                                        "components": [
                                        {
                                                "key": "employmentInformation.previousEmployment.companyName"
                                        },
                                        {
                                                "key": "documents.jobCertificate"
                                        }
                                        ]
                                }
                                ]
                        }
                }
                """;

        return objectMapper.readValue(configsString, FormConfiguration.class);
    }
}
