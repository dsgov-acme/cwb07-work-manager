package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.FormConfigurationSelectionRule;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.generated.models.FormConfigSelectionRuleExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps transaction definitions between the following 2 forms.
 *
 * <ul>
 *     <li>API Models (
 *      {@link io.nuvalence.workmanager.service.generated.models.TransactionDefinitionUpdateModel}
 *         and {@link io.nuvalence.workmanager.service.generated.models.TransactionDefinitionResponseModel})</li>
 *     <li>Logic/Persistence Model
 *     ({@link io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition})</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface TransactionDefinitionMapper extends LazyLoadingAwareMapper {
    TransactionDefinitionMapper INSTANCE = Mappers.getMapper(TransactionDefinitionMapper.class);

    TransactionDefinitionResponseModel transactionDefinitionToResponseModel(
            TransactionDefinition value);

    @Mapping(
            target = "subjectType",
            expression = "java(stringToProfileType(model.getSubjectType()))")
    @Mapping(
            target = "allowedRelatedPartyTypes",
            expression = "java(stringsToProfileTypes(model.getAllowedRelatedPartyTypes()))")
    TransactionDefinition updateModelToTransactionDefinition(
            TransactionDefinitionUpdateModel model);

    @Mapping(
            target = "subjectType",
            expression = "java(stringToProfileType(model.getSubjectType()))")
    @Mapping(
            target = "allowedRelatedPartyTypes",
            expression = "java(stringsToProfileTypes(model.getAllowedRelatedPartyTypes()))")
    TransactionDefinition createModelToTransactionDefinition(
            TransactionDefinitionCreateModel model);

    /**
     * Converts a TransactionDefinition to a TransactionDefinitionExportModel.
     *
     * @param value           the TransactionDefinition.
     * @return the TransactionDefinitionExportModel.
     */
    default TransactionDefinitionExportModel
            transactionDefinitionToTransactionDefinitionExportModel(TransactionDefinition value) {
        if (value == null) {
            return null;
        }

        TransactionDefinitionExportModel model = new TransactionDefinitionExportModel();
        model.setKey(value.getKey());
        model.setName(value.getName());
        model.setProcessDefinitionKey(value.getProcessDefinitionKey());
        model.setDefaultStatus(value.getDefaultStatus());
        model.setCategory(value.getCategory());
        model.setDefaultFormConfigurationKey(value.getDefaultFormConfigurationKey());
        model.setSchemaKey(value.getSchemaKey());
        model.setTransactionDefinitionSetKey(value.getTransactionDefinitionSetKey());
        model.setSubjectType(value.getSubjectType().getValue());
        model.setAllowedRelatedPartyTypes(
                value.getAllowedRelatedPartyTypes().stream().map(ProfileType::getValue).toList());
        model.setIsPublicVisible(value.getIsPublicVisible());

        model.setFormConfigurationSelectionRules(
                formConfigurationSelectionRuleToExportModel(
                        value.getFormConfigurationSelectionRules()));

        return model;
    }

    default List<FormConfigSelectionRuleExportModel> formConfigurationSelectionRuleToExportModel(
            List<FormConfigurationSelectionRule> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        List<FormConfigSelectionRuleExportModel> models = new ArrayList<>();
        for (FormConfigurationSelectionRule value : values) {
            models.add(formConfigurationSelectionRuleToExportModel(value));
        }

        return models;
    }

    @Mapping(source = "task", target = "task")
    @Mapping(source = "viewer", target = "viewer")
    @Mapping(source = "context", target = "context")
    @Mapping(source = "formConfigurationKey", target = "formConfigurationKey")
    FormConfigSelectionRuleExportModel formConfigurationSelectionRuleToExportModel(
            FormConfigurationSelectionRule value);

    default ProfileType stringToProfileType(String value) {
        if (value == null) {
            return ProfileType.INDIVIDUAL;
        }
        return ProfileType.fromValue(value);
    }

    default Set<ProfileType> stringsToProfileTypes(List<String> strings) {
        Set<ProfileType> result = new HashSet<>();
        if (strings == null) {
            return result;
        }
        for (String s : strings) {
            result.add(stringToProfileType(s));
        }

        return result;
    }
}
