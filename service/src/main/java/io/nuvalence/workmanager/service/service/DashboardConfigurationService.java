package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardTabConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.models.SearchTransactionsFilters;
import io.nuvalence.workmanager.service.models.TransactionFilters;
import io.nuvalence.workmanager.service.repository.DashboardConfigurationRepository;
import io.nuvalence.workmanager.service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.NotFoundException;

/**
 * Service for dashboard configuration.
 **/
@Component
@RequiredArgsConstructor
public class DashboardConfigurationService {

    private static final String STATUS = "status";
    private static final String PRIORITY = "priority";
    private static final String ASSIGNED_TO_ME = "assignedToMe";

    private static final int SINGLE_FILTER_SIZE = 1;

    private final DashboardConfigurationRepository dashboardConfigurationRepository;
    private final TransactionDefinitionSetOrderService transactionDefinitionSetOrderService;
    private final TransactionRepository transactionRepository;
    private final TransactionDefinitionService transactionDefinitionService;

    /**
     * Gets all dashboards that are expected to be listed.
     *
     * @return list of dashboards.
     */
    public List<DashboardConfiguration> getAllDashboards() {
        List<DashboardConfiguration> listOfOrderedAndFilteredDashboards = new ArrayList<>();
        transactionDefinitionSetOrderService.getTransactionDefinitionSetOrder().stream()
                .forEach(
                        transactionDefinitionSet -> {
                            if (transactionDefinitionSet.getSortOrder() != null) {
                                listOfOrderedAndFilteredDashboards.add(
                                        transactionDefinitionSet.getDashboardConfiguration());
                            }
                        });

        return listOfOrderedAndFilteredDashboards;
    }

    /**
     * Finds a dashboard by transaction set key.
     *
     * @param transactionSetKey transaction set key.
     *
     * @return dashboard configuration.
     */
    public DashboardConfiguration getDashboardByTransactionSetKey(String transactionSetKey) {
        return dashboardConfigurationRepository
                .findByTransactionDefinitionSetKey(transactionSetKey)
                .orElseThrow(() -> new NotFoundException("Dashboard not found"));
    }

    /**
     * Counts the number of transactions for each tab in a dashboard.
     *
     * @param transactionSetKey transaction set key.
     *
     * @return map of tab label to count.
     */
    public Map<String, Long> countTabsForDashboard(String transactionSetKey) {
        DashboardConfiguration dashboardConfiguration =
                dashboardConfigurationRepository
                        .findByTransactionDefinitionSetKey(transactionSetKey)
                        .orElseThrow(() -> new NotFoundException("Dashboard not found"));

        Set<String> statusToCount = new HashSet<>();
        Set<String> prioritiesToCount = new HashSet<>();

        Map<String, Long> countResults = new HashMap<>();

        List<String> transactionDefinitionKeys =
                transactionDefinitionService.createTransactionDefinitionKeysList(
                        null, transactionSetKey);

        dashboardConfiguration.getTabs().stream()
                .forEach(
                        tab ->
                                processDashboardTab(
                                        tab,
                                        statusToCount,
                                        prioritiesToCount,
                                        countResults,
                                        transactionDefinitionKeys));

        Map<String, Long> statusCounts =
                transactionRepository.getTransactionCountsByStatusSimplified(
                        statusToCount, transactionDefinitionKeys);
        Map<String, Long> priorityCounts =
                transactionRepository.getTransactionCountsByPrioritySimplified(
                        prioritiesToCount, transactionDefinitionKeys);

        integrateUnifiedCountsToFinalCountResults(
                dashboardConfiguration, countResults, statusCounts, priorityCounts);

        return countResults;
    }

    private void processDashboardTab(
            DashboardTabConfiguration dashboardTab,
            Set<String> statusToCount,
            Set<String> prioritiesToCount,
            Map<String, Long> countResults,
            List<String> transactionDefinitionKeys) {
        if (dashboardTab.getFilter().size() == SINGLE_FILTER_SIZE) {
            String firstKey =
                    dashboardTab.getFilter().keySet().stream()
                            .findFirst()
                            .orElseThrow(() -> new BusinessLogicException("Invalid filter keys"));
            if (firstKey.equals(STATUS)) {
                Object status = dashboardTab.getFilter().get(firstKey);
                addObjectsToCount(statusToCount, status);
            } else if (firstKey.equals(PRIORITY)) {
                Object priorities = dashboardTab.getFilter().get(firstKey);
                addObjectsToCount(prioritiesToCount, priorities);
            } else if (firstKey.equals(ASSIGNED_TO_ME)) {
                TransactionFilters filters =
                        SearchTransactionsFilters.builder()
                                .transactionDefinitionKeys(transactionDefinitionKeys)
                                .assignedToMe((Boolean) dashboardTab.getFilter().get(firstKey))
                                .build();

                countResults.put(
                        dashboardTab.getTabLabel(),
                        transactionRepository.count(filters.getTransactionSpecifications()));
            }
        } else {
            handleComplexFilters(countResults, dashboardTab, transactionDefinitionKeys);
        }
    }

    private void integrateUnifiedCountsToFinalCountResults(
            DashboardConfiguration dashboardConfiguration,
            Map<String, Long> countResults,
            Map<String, Long> statusCounts,
            Map<String, Long> priorityCounts) {
        dashboardConfiguration.getTabs().stream()
                .forEach(
                        dashboardTab -> {
                            if (dashboardTab.getFilter().size() == SINGLE_FILTER_SIZE) {
                                String firstKey =
                                        dashboardTab.getFilter().keySet().stream()
                                                .findFirst()
                                                .get();
                                if (firstKey.equals(STATUS)) {
                                    List<String> values =
                                            getValuesFromFilter(dashboardTab, firstKey);
                                    Long count = getCountForGivenTab(statusCounts, values);
                                    countResults.put(dashboardTab.getTabLabel(), count);
                                } else if (firstKey.equals(PRIORITY)) {
                                    List<String> values =
                                            getValuesFromFilter(dashboardTab, firstKey);
                                    Long count = getCountForGivenTab(priorityCounts, values);
                                    countResults.put(dashboardTab.getTabLabel(), count);
                                }
                            }
                        });
    }

    private void handleComplexFilters(
            Map<String, Long> countResults,
            DashboardTabConfiguration dashboardTab,
            List<String> transactionDefinitionKeys) {
        if (dashboardTab.getFilter().isEmpty()) {
            countResults.put(dashboardTab.getTabLabel(), 0L);
            return;
        }

        List<String> priorityStrings = getValuesFromFilter(dashboardTab, PRIORITY);

        List<TransactionPriority> priorities = null;
        if (priorityStrings != null) {
            priorities =
                    priorityStrings.stream().map(TransactionPriority::fromStringValue).toList();
        }

        List<String> status = getValuesFromFilter(dashboardTab, STATUS);

        Boolean assignedToMe = (Boolean) dashboardTab.getFilter().get(ASSIGNED_TO_ME);

        TransactionFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(transactionDefinitionKeys)
                        .priority(priorities)
                        .status(status)
                        .assignedToMe(assignedToMe)
                        .build();

        countResults.put(
                dashboardTab.getTabLabel(),
                transactionRepository.count(filters.getTransactionSpecifications()));
    }

    private void addObjectsToCount(Set<String> toCount, Object values) {
        try {
            if (values instanceof List) {
                List<String> valueStrings = (List<String>) values;
                toCount.addAll(valueStrings);

            } else {
                toCount.add(values.toString());
            }
        } catch (Exception e) {
            throw new BusinessLogicException("Invalid filter value");
        }
    }

    private List<String> getValuesFromFilter(
            DashboardTabConfiguration dashboardTab, String firstKey) {
        List<String> values = new ArrayList<>();
        if (dashboardTab.getFilter().get(firstKey) instanceof List) {
            values = (List<String>) dashboardTab.getFilter().get(firstKey);
        } else {
            values.add(dashboardTab.getFilter().get(firstKey).toString());
        }
        return values;
    }

    private Long getCountForGivenTab(Map<String, Long> counts, List<String> values) {
        Long count = 0L;
        if (values == null || values.isEmpty()) {
            return count;
        }

        if (values.size() == SINGLE_FILTER_SIZE) {
            return counts.get(values.get(0));
        }

        for (String value : values) {
            if (counts.containsKey(value)) {
                count += counts.get(value);
            }
        }

        return count;
    }
}
