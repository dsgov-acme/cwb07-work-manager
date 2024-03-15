package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.transaction.DashboardColumnConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardTabConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DisplayFormat;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetDataRequirement;
import io.nuvalence.workmanager.service.generated.models.ColumnExportModel;
import io.nuvalence.workmanager.service.generated.models.ConstraintExportModel;
import io.nuvalence.workmanager.service.generated.models.DashboardConfigurationExportModel;
import io.nuvalence.workmanager.service.generated.models.TabExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardResultModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for transaction definition set.
 */
@Mapper(componentModel = "spring")
public interface TransactionDefinitionSetMapper extends LazyLoadingAwareMapper {
    TransactionDefinitionSetMapper INSTANCE =
            Mappers.getMapper(TransactionDefinitionSetMapper.class);

    TransactionDefinitionSetResponseModel transactionDefinitionSetToResponseModel(
            TransactionDefinitionSet value);

    TransactionDefinitionSet updateModelToTransactionDefinitionSet(
            TransactionDefinitionSetUpdateModel model);

    TransactionDefinitionSet createModelToTransactionDefinitionSet(
            TransactionDefinitionSetCreateModel model);

    default TransactionDefinitionSetDashboardResultModel dashboardToDashboardModel(
            DashboardConfiguration dashboardConfiguration) {
        return Mappers.getMapper(DashboardConfigurationMapper.class)
                .dashboardConfigurationToDashboardResultModel(dashboardConfiguration, null);
    }

    @Mapping(source = "key", target = "key")
    @Mapping(source = "workflow", target = "workflow")
    @Mapping(source = "dashboardConfiguration", target = "dashboardConfiguration")
    @Mapping(source = "constraints", target = "constraints")
    TransactionDefinitionSetExportModel transactionDefinitionSetToExportModel(
            TransactionDefinitionSet value);

    @Mapping(source = "dashboardLabel", target = "dashboardLabel")
    @Mapping(source = "menuIcon", target = "menuIcon")
    @Mapping(source = "columns", target = "columns")
    @Mapping(source = "tabs", target = "tabs")
    DashboardConfigurationExportModel dashboardConfigurationToExportModel(
            DashboardConfiguration value);

    @Mapping(source = "path", target = "path")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "contentType", target = "contentType")
    ConstraintExportModel constraintToExportModel(TransactionDefinitionSetDataRequirement value);

    @Mapping(source = "columnLabel", target = "columnLabel")
    @Mapping(source = "attributePath", target = "attributePath")
    @Mapping(source = "sortable", target = "sortable")
    @Mapping(
            source = "displayFormat",
            target = "displayFormat",
            qualifiedByName = "displayFormatToString")
    ColumnExportModel columnToExportModel(DashboardColumnConfiguration value);

    @Mapping(source = "tabLabel", target = "tabLabel")
    @Mapping(source = "filter", target = "filter")
    TabExportModel tabToExportModel(DashboardTabConfiguration value);

    @Named("displayFormatToString")
    default String displayFormatToString(DisplayFormat displayFormat) {
        return displayFormat == null ? null : displayFormat.name();
    }
}
