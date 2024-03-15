package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationCreateModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationExportModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationRenderModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationResponseModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maps form configurations.
 *
 * <ul>
 *     <li>Logic/Persistence Model
 *     ({@link io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration})</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface FormConfigurationMapper {

    static final String DEFAULT_ROOT_CONFIG_KEY = "ROOT_COMPONENT";

    FormConfigurationMapper INSTANCE = Mappers.getMapper(FormConfigurationMapper.class);

    @Mapping(
            target = "configuration",
            expression = "java(mapConfigurationAttribute(model.getConfiguration()))")
    FormConfiguration mapModelToFormConfiguration(FormConfigurationUpdateModel model);

    @Mapping(
            target = "configuration",
            expression = "java(mapConfigurationAttribute(model.getConfiguration()))")
    FormConfiguration mapCreationModelToFormConfiguration(FormConfigurationCreateModel model);

    /**
     * Set dataBacked property to default as true for all components.
     *
     * @param configuration the configuration element from the form configuration model
     * @return the configuration element with all dataBacked properties set to true if not already set
     */
    default Map<String, Object> mapConfigurationAttribute(Map<String, Object> configuration) {
        if (configuration != null && configuration.containsKey("components")) {
            List<Map<String, Object>> components =
                    (List<Map<String, Object>>) configuration.get("components");
            for (Map<String, Object> component : components) {
                if (!component.containsKey("input")) {
                    component.put("input", true);
                }
                if (component.containsKey("components")) {
                    mapConfigurationAttribute(component);
                }
            }
        }
        return configuration;
    }

    FormConfigurationResponseModel mapFormConfigurationToModel(FormConfiguration formConfiguration);

    FormConfigurationRenderModel mapFormConfigurationToRenderModel(
            FormConfiguration formConfiguration);

    /**
     * Converts a FormConfiguration to a FormConfigurationExportModel.
     *
     * @param value           the FormConfiguration.
     * @return the FormConfigurationExportModel.
     */
    default FormConfigurationExportModel formConfigurationToFormConfigurationExportModel(
            FormConfiguration value) {
        if (value == null) {
            return null;
        }

        FormConfigurationExportModel model = new FormConfigurationExportModel();
        model.setTransactionDefinitionKey(value.getTransactionDefinitionKey());
        model.setKey(value.getKey());
        model.setName(value.getName());
        model.setSchemaKey(value.getSchemaKey());
        model.setConfigurationSchema(value.getConfigurationSchema());
        model.setConfiguration(value.getConfiguration());

        return model;
    }

    /**
     * Converts a FormConfiguration to a NuvalenceFormioComponent tree. Which is used for 
     * validation purposes, and not to be persisted.
     * The data structure intended to be persisted is
     * the {@link io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration FormConfiguration}
     * object received here.
     *
     *
     * @param formConfiguration the FormConfiguration to convert
     * @return the data structure used for validation
     */
    default NuvalenceFormioComponent formConfigToValidationConfig(
            FormConfiguration formConfiguration) {

        return formConfigToValidationConfigForStep(formConfiguration, null, true);
    }

    /**
     * Converts a FormConfiguration to a NuvalenceFormioComponent tree <b>only with the contents of the wanted step.</b> Which is used for 
     * validation purposes, and not to be persisted.
     * <br>
     * <b>NOTE: If the formStepKey is null, the entire form, containing all steps is returned.</b> 
     * <br>
     * The data structure intended to be persisted is
     * the {@link io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration FormConfiguration}
     * object received here.
     *
     *
     * @param formConfiguration the FormConfiguration to convert
     * @return the data structure used for validation
     */
    default NuvalenceFormioComponent formConfigToValidationConfigForStep(
            FormConfiguration formConfiguration, String formStepKey, boolean normalizeFormioTree) {

        NuvalenceFormioComponent rootFormConfigComponent =
                SpringConfig.getMapper()
                        .convertValue(
                                formConfiguration.getConfiguration(),
                                NuvalenceFormioComponent.class);
        rootFormConfigComponent.setKey(DEFAULT_ROOT_CONFIG_KEY);

        if (formStepKey != null) {
            NuvalenceFormioComponent stepWanted =
                    rootFormConfigComponent.getComponents().stream()
                            .filter(c -> c.getKey().equals(formStepKey))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new ProvidedDataException(
                                                    "The provided formStepKey does not exist in the"
                                                            + " form configuration"));

            rootFormConfigComponent.getComponents().clear();
            rootFormConfigComponent.getComponents().add(stepWanted);
        }

        if (normalizeFormioTree) {
            normalizeRelativeKeys(rootFormConfigComponent, null);
            normalizeComponentTree(rootFormConfigComponent);
        }

        return rootFormConfigComponent;
    }

    /**
     * Recursively normalize the components tree, by structuring components by their keys and 
     * converting flat only components into nested components.
     *
     *
     * @param rootComponent the component to normalize
     */
    private void normalizeComponentTree(NuvalenceFormioComponent rootComponent) {

        List<NuvalenceFormioComponent> originalComponentsList = rootComponent.getComponents();
        if (originalComponentsList == null || originalComponentsList.isEmpty()) {
            return;
        }

        TreeMap<String, NuvalenceFormioComponent> allKeysAndComponents = new TreeMap<>();
        TreeMap<String, NuvalenceFormioComponent> newKeysAndComponents = new TreeMap<>();
        HashMap<String, NuvalenceFormioComponent> originalParentComponents = new HashMap<>();

        getAllKeysAndComponentsRecursively(
                originalComponentsList, allKeysAndComponents, originalParentComponents);

        var newComponentsList = new ArrayList<NuvalenceFormioComponent>();

        // set components
        for (var entry : allKeysAndComponents.entrySet()) {
            String parentKey = getParentKey(entry.getKey());

            if (parentKey == null) {
                newComponentsList.add(entry.getValue());

            } else {
                var parentComponent =
                        allKeysAndComponents.getOrDefault(
                                parentKey,
                                createAllParentsNeeded(
                                        parentKey,
                                        allKeysAndComponents,
                                        newKeysAndComponents,
                                        newComponentsList));
                parentComponent.getComponents().add(entry.getValue());
            }
        }

        cleanNotNeededData(allKeysAndComponents, originalParentComponents);

        rootComponent.setComponents(newComponentsList);
    }

    private String getParentKey(String key) {
        int lastIndex = key.lastIndexOf(".");
        return lastIndex != -1 ? key.substring(0, lastIndex) : null;
    }

    /**
     * Complete the keys that are inside components with the keyContextProvider property as true,
     * making the keys absolute.
     *
     * @param rootComponent the root component
     * @param partialParentKey the partial parent key
     */
    private void normalizeRelativeKeys(
            NuvalenceFormioComponent rootComponent, String partialParentKey) {
        if (partialParentKey != null) {
            rootComponent.setKey(partialParentKey + "." + rootComponent.getKey());
        }

        if (rootComponent.getComponents() != null) {
            for (int i = 0; i < rootComponent.getComponents().size(); i++) {
                String parentKey =
                        rootComponent.isKeyContextProvider()
                                ? rootComponent.getKey()
                                : partialParentKey;
                normalizeRelativeKeys(rootComponent.getComponents().get(i), parentKey);
            }
        }
    }

    /**
     * Cleans up unnecessary data within the provided TreeMap of NuvalenceFormioComponent objects.
     * It checks each entry's components list and sets it to null if it is either null or empty.
     *
     * @param allKeysAndComponents A TreeMap containing NuvalenceFormioComponent objects indexed by strings.
     *                            This method will modify the components of the entries in the TreeMap.
     * @param originalParentComponents A HashMap containing NuvalenceFormioComponent that
     *                                 are not sub-components or are ignorable.
     */
    private void cleanNotNeededData(
            TreeMap<String, NuvalenceFormioComponent> allKeysAndComponents,
            HashMap<String, NuvalenceFormioComponent> originalParentComponents) {
        for (var entry : allKeysAndComponents.entrySet()) {
            if (entry.getValue().getComponents() == null
                    || entry.getValue().getComponents().isEmpty()) {
                entry.getValue().setComponents(null);
                if (originalParentComponents.containsKey(entry.getKey())) {
                    entry.getValue().setInput(false);
                }
            }
        }
    }

    /**
     * Create all parents needed for the provided key, and add them to the new components list to be searchable later. 
     * Also return the last parent created.
     *
     *
     * @param parentKeyToCreate the key of the parent to create
     * @param allKeysAndComponents the map of all keys and components
     * @param newKeysAndComponents the map of new keys and components
     * @param newComponentsList the list of new components to include the first parent if it doesn't exist
     * @return the last parent created
     */
    private NuvalenceFormioComponent createAllParentsNeeded(
            String parentKeyToCreate,
            TreeMap<String, NuvalenceFormioComponent> allKeysAndComponents,
            TreeMap<String, NuvalenceFormioComponent> newKeysAndComponents,
            ArrayList<NuvalenceFormioComponent> newComponentsList) {

        String[] allParentsSectionsNeeded = parentKeyToCreate.split("\\.");

        NuvalenceFormioComponent lastParent = allKeysAndComponents.get(allParentsSectionsNeeded[0]);
        lastParent =
                lastParent == null
                        ? newKeysAndComponents.computeIfAbsent(
                                allParentsSectionsNeeded[0],
                                key -> {
                                    NuvalenceFormioComponent newLastParent =
                                            new NuvalenceFormioComponent();
                                    newLastParent.setKey(allParentsSectionsNeeded[0]);
                                    newLastParent.setComponents(new ArrayList<>());
                                    newLastParent.setInput(true);

                                    // first parent needs to be added to the new list of components
                                    newComponentsList.add(newLastParent);

                                    return newLastParent;
                                })
                        : lastParent;

        for (int i = 1; i < allParentsSectionsNeeded.length; i++) {
            String newParentKey = lastParent.getKey() + "." + allParentsSectionsNeeded[i];
            NuvalenceFormioComponent newParentNeeded = allKeysAndComponents.get(newParentKey);

            if (newParentNeeded == null) {
                newParentNeeded = newKeysAndComponents.get(newParentKey);

                if (newParentNeeded == null) {
                    newParentNeeded = new NuvalenceFormioComponent();
                    newParentNeeded.setKey(newParentKey);
                    newParentNeeded.setComponents(new ArrayList<>());
                    newParentNeeded.setInput(true);

                    newKeysAndComponents.put(newParentKey, newParentNeeded);
                    lastParent.getComponents().add(newParentNeeded);
                }
            }

            lastParent = newParentNeeded;
        }

        return lastParent;
    }

    /**
     * Recursively get all keys and components from the components tree, into the provided target map,
     * and inform of all originally parent components.
     *
     *
     * @param componentsToTraverse the source components to organize into the target map
     * @param targetMap the target map to organize the components into
     * @param originalParentComponents the map to inform of all originally parent components
     */
    private void getAllKeysAndComponentsRecursively(
            List<NuvalenceFormioComponent> componentsToTraverse,
            TreeMap<String, NuvalenceFormioComponent> targetMap,
            Map<String, NuvalenceFormioComponent> originalParentComponents) {
        for (var component : componentsToTraverse) {

            String key = component.getKey();
            if (key == null || key.isBlank()) {
                throwFormIoException(null, "A component is missing its key");
            }

            // check there are no empty strings in key sections
            String[] keySections = key.split("\\.");
            for (String keySection : keySections) {
                if (keySection.isBlank()) {
                    throwFormIoException(
                            key,
                            "All components must have a non-blank key with non-blank sections");
                }
            }

            key = key.trim();
            component.setKey(key);
            if (targetMap.containsKey(key)) {
                throwFormIoException(
                        key, "Component key is found more than once in the form configuration");
            } else {
                targetMap.put(key, component);
            }

            if (component.getComponents() != null && !component.getComponents().isEmpty()) {
                getAllKeysAndComponentsRecursively(
                        component.getComponents(), targetMap, originalParentComponents);
                originalParentComponents.put(component.getKey(), component);
            }
            component.setComponents(new ArrayList<>());
        }
    }

    /**
     * Throws a NuvalenceFormioValidationException with the provided key and message.
     *
     * @param key the optional component key with the problem
     * @param message error message
     */
    private void throwFormIoException(String key, String message) {
        throw new NuvalenceFormioValidationException(
                NuvalenceFormioValidationExMessage.builder()
                        .formioValidationErrors(
                                List.of(
                                        NuvalenceFormioValidationExItem.builder()
                                                .controlName(key)
                                                .errorMessage(message)
                                                .build()))
                        .build());
    }
}
