package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardTabConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.repository.DashboardConfigurationRepository;
import io.nuvalence.workmanager.service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class DashboardConfigurationServiceTest {

    @Mock private DashboardConfigurationRepository repository;

    @Mock private TransactionDefinitionSetOrderService transactionDefinitionSetOrderService;

    @Mock private TransactionRepository transactionRepository;

    @Mock private TransactionDefinitionService transactionDefinitionService;

    private DashboardConfigurationService service;

    @BeforeEach
    public void setUp() {
        service =
                new DashboardConfigurationService(
                        repository,
                        transactionDefinitionSetOrderService,
                        transactionRepository,
                        transactionDefinitionService);
    }

    @Test
    void testGetAllDashboards() {
        UUID idOne = UUID.randomUUID();
        DashboardConfiguration dashboardConfigurationOne =
                DashboardConfiguration.builder()
                        .id(idOne)
                        .transactionDefinitionSet(
                                TransactionDefinitionSet.builder().key("keyOne").build())
                        .build();

        when(transactionDefinitionSetOrderService.getTransactionDefinitionSetOrder())
                .thenReturn(
                        List.of(
                                TransactionDefinitionSet.builder()
                                        .key("keyOne")
                                        .dashboardConfiguration(
                                                DashboardConfiguration.builder().id(idOne).build())
                                        .sortOrder(1)
                                        .build()));

        List<DashboardConfiguration> dashboardConfigurations = service.getAllDashboards();

        assertEquals(1, dashboardConfigurations.size());
        assertEquals(idOne, dashboardConfigurations.get(0).getId());
    }

    @Test
    void testGetDashboardByTransactionSetKeySucceeds() {
        String transactionSetKey = "transactionSetKey";

        DashboardConfiguration dashboardConfiguration = DashboardConfiguration.builder().build();
        when(repository.findByTransactionDefinitionSetKey(transactionSetKey))
                .thenReturn(Optional.of(dashboardConfiguration));

        DashboardConfiguration result = service.getDashboardByTransactionSetKey(transactionSetKey);

        assertEquals(dashboardConfiguration, result);
    }

    @Test
    void testGetDashboardByTransactionSetKeyNotFound() {
        String transactionSetKey = "transactionSetKey";

        DashboardConfiguration dashboardConfiguration = DashboardConfiguration.builder().build();

        when(repository.findByTransactionDefinitionSetKey(transactionSetKey))
                .thenThrow(new NotFoundException("Dashboard Configuration not found"));

        assertThrows(
                NotFoundException.class,
                () -> {
                    service.getDashboardByTransactionSetKey(transactionSetKey);
                });
    }

    @Test
    void testCountTabsForDashboardSimpleStatus() {
        DashboardTabConfiguration tabConfiguration =
                DashboardTabConfiguration.builder()
                        .tabLabel("one")
                        .filter(Map.of("status", "Draft"))
                        .build();
        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().tabs(List.of(tabConfiguration)).build();

        when(repository.findByTransactionDefinitionSetKey("key"))
                .thenReturn(Optional.of(dashboardConfiguration));

        List<String> transactionDefinitionKeys = List.of("key");
        when(transactionDefinitionService.createTransactionDefinitionKeysList(any(), any()))
                .thenReturn(transactionDefinitionKeys);

        when(transactionRepository.getTransactionCountsByStatusSimplified(
                        Set.of("Draft"), transactionDefinitionKeys))
                .thenReturn(Map.of("Draft", 1L));

        Map<String, Long> result = service.countTabsForDashboard("key");

        assertEquals(1, result.size());
        assertTrue(result.keySet().contains("one"));
        assertEquals(1L, result.get("one"));
    }

    @Test
    void testCountTabsForDashboardSimplePriority() {
        DashboardTabConfiguration tabConfiguration =
                DashboardTabConfiguration.builder()
                        .tabLabel("one")
                        .filter(Map.of("priority", "LOW"))
                        .build();
        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().tabs(List.of(tabConfiguration)).build();

        when(repository.findByTransactionDefinitionSetKey("key"))
                .thenReturn(Optional.of(dashboardConfiguration));

        List<String> transactionDefinitionKeys = List.of("key");
        when(transactionDefinitionService.createTransactionDefinitionKeysList(any(), any()))
                .thenReturn(transactionDefinitionKeys);

        when(transactionRepository.getTransactionCountsByPrioritySimplified(
                        Set.of("LOW"), transactionDefinitionKeys))
                .thenReturn(Map.of("LOW", 1L));

        Map<String, Long> result = service.countTabsForDashboard("key");

        assertEquals(1, result.size());
        assertTrue(result.keySet().contains("one"));
        assertEquals(1L, result.get("one"));
    }

    @Test
    void testCountTabsForDashboardSimpleAssignedToMe() {
        DashboardTabConfiguration tabConfiguration =
                DashboardTabConfiguration.builder()
                        .tabLabel("one")
                        .filter(Map.of("assignedToMe", true))
                        .build();
        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().tabs(List.of(tabConfiguration)).build();

        when(repository.findByTransactionDefinitionSetKey("key"))
                .thenReturn(Optional.of(dashboardConfiguration));
        when(transactionRepository.count(any())).thenReturn(1L);

        Map<String, Long> result = service.countTabsForDashboard("key");

        assertEquals(1, result.size());
        assertTrue(result.keySet().contains("one"));
        assertEquals(1L, result.get("one"));
    }

    @Test
    void testCountTabsForDashboardList() {
        DashboardTabConfiguration tabConfiguration =
                DashboardTabConfiguration.builder()
                        .tabLabel("one")
                        .filter(Map.of("status", List.of("Draft", "Review")))
                        .build();
        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().tabs(List.of(tabConfiguration)).build();

        when(repository.findByTransactionDefinitionSetKey("key"))
                .thenReturn(Optional.of(dashboardConfiguration));
        when(transactionRepository.getTransactionCountsByStatusSimplified(any(), any()))
                .thenReturn(Map.of("Draft", 3L, "Review", 2L));

        Map<String, Long> result = service.countTabsForDashboard("key");

        assertEquals(1, result.size());
        assertTrue(result.keySet().contains("one"));
        assertEquals(5L, result.get("one"));
    }

    @Test
    void testCountTabsForComplex() {
        DashboardTabConfiguration tabConfiguration =
                DashboardTabConfiguration.builder()
                        .tabLabel("one")
                        .filter(Map.of("status", "Draft", "priority", "LOW"))
                        .build();
        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().tabs(List.of(tabConfiguration)).build();

        when(repository.findByTransactionDefinitionSetKey("key"))
                .thenReturn(Optional.of(dashboardConfiguration));
        when(transactionRepository.count(any())).thenReturn(4L);

        Map<String, Long> result = service.countTabsForDashboard("key");

        assertEquals(1, result.size());
        assertTrue(result.keySet().contains("one"));
        assertEquals(4L, result.get("one"));
    }
}
