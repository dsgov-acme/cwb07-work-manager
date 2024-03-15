package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.generated.models.DashboardCountsModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardResultModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper for dashboard configuration.
 */
@Mapper(componentModel = "spring")
public interface DashboardConfigurationMapper {
    @Mapping(
            target = "transactionSet",
            expression = "java(dashboardConfiguration.getTransactionDefinitionSet().getKey())")
    @Mapping(target = "transactionDefinitionKeys", source = "transactionDefinitionKeys")
    TransactionDefinitionSetDashboardResultModel dashboardConfigurationToDashboardResultModel(
            DashboardConfiguration dashboardConfiguration, List<String> transactionDefinitionKeys);

    /**
     * Maps a count to a dashboard count model.
     *
     * @param label label for the count.
     * @param count count.
     *
     * @return dashboard count model.
     */
    default DashboardCountsModel mapCount(String label, Long count) {
        DashboardCountsModel dashboardCountsModel = new DashboardCountsModel();
        dashboardCountsModel.setTabLabel(label);
        dashboardCountsModel.setCount(count);
        return dashboardCountsModel;
    }
}
