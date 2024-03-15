package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.transaction.DashboardColumnConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardTabConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetDataRequirement;
import io.nuvalence.workmanager.service.generated.models.ColumnExportModel;
import io.nuvalence.workmanager.service.generated.models.ConstraintExportModel;
import io.nuvalence.workmanager.service.generated.models.DashboardColumnModel;
import io.nuvalence.workmanager.service.generated.models.DashboardConfigurationExportModel;
import io.nuvalence.workmanager.service.generated.models.DashboardTabModel;
import io.nuvalence.workmanager.service.generated.models.TabExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetConstraintModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardRequestModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardResultModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetUpdateModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class TransactionDefinitionSetMapperTest {

    private TransactionDefinitionSet transactionDefinitionSet;
    private TransactionDefinitionSetExportModel exportModel;

    private DashboardConfiguration dashboardConfiguration;
    private DashboardConfigurationExportModel dashboardExportModel;

    private TransactionDefinitionSetDataRequirement constraint;
    private ConstraintExportModel constraintExportModel;

    private DashboardColumnConfiguration column;
    private ColumnExportModel columnExportModel;

    private DashboardTabConfiguration tab;
    private TabExportModel tabExportModel;

    private TransactionDefinitionSetResponseModel transactionDefinitionSetResponseModel;
    private TransactionDefinitionSetUpdateModel transactionDefinitionSetUpdateModel;
    private TransactionDefinitionSetDashboardResultModel
            transactionDefinitionSetDashboardResultModel;

    private TransactionDefinitionSetMapper mapper;

    @BeforeEach
    void setup() {
        constraint =
                TransactionDefinitionSetDataRequirement.builder()
                        .path("path")
                        .contentType("contentType")
                        .type("type")
                        .build();

        column =
                DashboardColumnConfiguration.builder()
                        .columnLabel("column")
                        .attributePath("path")
                        .sortable(true)
                        .build();

        tab =
                DashboardTabConfiguration.builder()
                        .tabLabel("tab")
                        .filter(Map.of("key", "value"))
                        .build();

        dashboardConfiguration =
                DashboardConfiguration.builder()
                        .dashboardLabel("label")
                        .menuIcon("icon")
                        .columns(List.of(column))
                        .tabs(List.of(tab))
                        .transactionDefinitionSet(
                                TransactionDefinitionSet.builder().key("key").build())
                        .build();

        transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .dashboardConfiguration(dashboardConfiguration)
                        .constraints(List.of(constraint))
                        .workflow("workflow")
                        .key("key")
                        .build();

        TransactionDefinitionSetConstraintModel constraint =
                new TransactionDefinitionSetConstraintModel();
        constraint.setPath("path");
        constraint.setContentType("contentType");
        constraint.setType("type");

        DashboardColumnModel column = new DashboardColumnModel();
        column.setColumnLabel("column");
        column.setAttributePath("path");
        column.setSortable(true);

        DashboardTabModel tab = new DashboardTabModel();
        tab.setTabLabel("tab");
        tab.setFilter(Map.of("key", "value"));

        TransactionDefinitionSetDashboardRequestModel dashboard =
                new TransactionDefinitionSetDashboardRequestModel();
        dashboard.setDashboardLabel("label");
        dashboard.setMenuIcon("icon");
        dashboard.setColumns(List.of(column));
        dashboard.setTabs(List.of(tab));

        transactionDefinitionSetUpdateModel = new TransactionDefinitionSetUpdateModel();
        transactionDefinitionSetUpdateModel.setWorkflow("workflow");
        transactionDefinitionSetUpdateModel.setDashboardConfiguration(dashboard);
        transactionDefinitionSetUpdateModel.setConstraints(List.of(constraint));

        transactionDefinitionSetDashboardResultModel =
                new TransactionDefinitionSetDashboardResultModel();
        transactionDefinitionSetDashboardResultModel.setDashboardLabel("label");
        transactionDefinitionSetDashboardResultModel.setMenuIcon("icon");
        transactionDefinitionSetDashboardResultModel.setColumns(List.of(column));
        transactionDefinitionSetDashboardResultModel.setTabs(List.of(tab));
        transactionDefinitionSetDashboardResultModel.setTransactionSet("key");

        transactionDefinitionSetResponseModel = new TransactionDefinitionSetResponseModel();
        transactionDefinitionSetResponseModel.setWorkflow("workflow");
        transactionDefinitionSetResponseModel.setConstraints(List.of(constraint));
        transactionDefinitionSetResponseModel.setDashboardConfiguration(dashboard);
        transactionDefinitionSetResponseModel.setKey("key");

        constraintExportModel = new ConstraintExportModel();
        constraintExportModel.setPath("path");
        constraintExportModel.setType("type");
        constraintExportModel.setContentType("contentType");

        columnExportModel = new ColumnExportModel();
        columnExportModel.setColumnLabel("column");
        columnExportModel.setAttributePath("path");
        columnExportModel.setSortable(true);

        tabExportModel = new TabExportModel();
        tabExportModel.setTabLabel("tab");
        tabExportModel.setFilter(Map.of("key", "value"));

        dashboardExportModel = new DashboardConfigurationExportModel();
        dashboardExportModel.setDashboardLabel("label");
        dashboardExportModel.setMenuIcon("icon");
        dashboardExportModel.setColumns(List.of(columnExportModel));
        dashboardExportModel.setTabs(List.of(tabExportModel));

        exportModel = new TransactionDefinitionSetExportModel();
        exportModel.setWorkflow("workflow");
        exportModel.setDashboardConfiguration(dashboardExportModel);
        exportModel.setConstraints(List.of(constraintExportModel));

        mapper = TransactionDefinitionSetMapper.INSTANCE;
    }

    @Test
    void testDashboardConfigurationToExportModel() {
        assertEquals(
                dashboardExportModel,
                mapper.dashboardConfigurationToExportModel(dashboardConfiguration));
    }

    @Test
    void testConstraintToExportModel() {
        assertEquals(constraintExportModel, mapper.constraintToExportModel(constraint));
    }

    @Test
    void testColumnToExportModel() {
        assertEquals(columnExportModel, mapper.columnToExportModel(column));
    }

    @Test
    void testTabToExportModel() {
        assertEquals(tabExportModel, mapper.tabToExportModel(tab));
    }

    @Test
    void testTransactionDefinitionSetToExportModel() {
        transactionDefinitionSet.setKey(null);
        assertEquals(
                exportModel,
                mapper.transactionDefinitionSetToExportModel(transactionDefinitionSet));
    }

    @Test
    void transactionDefinitionSetToResponseModel() {
        assertEquals(
                transactionDefinitionSetResponseModel,
                mapper.transactionDefinitionSetToResponseModel(transactionDefinitionSet));
    }

    @Test
    void updateModelToTransactionDefinitionSet() {
        transactionDefinitionSet.setKey(null);
        transactionDefinitionSet.getDashboardConfiguration().setTransactionDefinitionSet(null);
        assertEquals(
                transactionDefinitionSet,
                mapper.updateModelToTransactionDefinitionSet(transactionDefinitionSetUpdateModel));
    }

    @Test
    void dashboardToDashboardModel() {
        assertEquals(
                transactionDefinitionSetDashboardResultModel,
                mapper.dashboardToDashboardModel(dashboardConfiguration));
    }
}
