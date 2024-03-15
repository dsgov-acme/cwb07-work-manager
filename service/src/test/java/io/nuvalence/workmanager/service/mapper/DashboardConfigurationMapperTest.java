package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.transaction.DashboardColumnConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardTabConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DisplayFormat;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardResultModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class DashboardConfigurationMapperTest {
    private final DashboardConfigurationMapper mapper =
            Mappers.getMapper(DashboardConfigurationMapper.class);

    @Test
    void testDashboardConfigurationToDashboardModel() {
        DashboardColumnConfiguration dashboardColumnConfiguration =
                DashboardColumnConfiguration.builder()
                        .id(UUID.randomUUID())
                        .columnLabel("label")
                        .attributePath("attributePath")
                        .sortable(true)
                        .displayFormat(DisplayFormat.DATE)
                        .build();
        DashboardTabConfiguration dashboardTabConfiguration =
                DashboardTabConfiguration.builder()
                        .id(UUID.randomUUID())
                        .tabLabel("label")
                        .filter(Map.of("key", "value"))
                        .build();
        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder()
                        .id(UUID.randomUUID())
                        .dashboardLabel("label")
                        .menuIcon("menuIcon")
                        .columns(List.of(dashboardColumnConfiguration))
                        .tabs(List.of(dashboardTabConfiguration))
                        .build();
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .id(UUID.randomUUID())
                        .key("testKey")
                        .dashboardConfiguration(dashboardConfiguration)
                        .build();
        dashboardConfiguration.setTransactionDefinitionSet(transactionDefinitionSet);

        TransactionDefinitionSetDashboardResultModel result =
                mapper.dashboardConfigurationToDashboardResultModel(
                        dashboardConfiguration, List.of("key"));

        assertEquals(dashboardConfiguration.getDashboardLabel(), result.getDashboardLabel());
        assertEquals(1, result.getColumns().size());
        assertEquals(
                dashboardConfiguration.getColumns().get(0).getColumnLabel(),
                result.getColumns().get(0).getColumnLabel());
        assertEquals(1, result.getTabs().size());
        assertEquals(
                dashboardConfiguration.getTabs().get(0).getTabLabel(),
                result.getTabs().get(0).getTabLabel());
    }
}
