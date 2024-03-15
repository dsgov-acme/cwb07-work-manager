package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests for {@link TransactionDefinitionSetService}.
 */
@ExtendWith(MockitoExtension.class)
class TransactionDefinitionSetServiceTest {
    @Mock private TransactionDefinitionSetRepository repository;

    private TransactionDefinitionSetService service;

    @BeforeEach
    void setup() {
        this.service = new TransactionDefinitionSetService(repository);
    }

    @Test
    void testSaveNewTransactionDefinitionSet() {
        String key = "key";

        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().id(UUID.randomUUID()).build();
        TransactionDefinitionSet newTransactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .dashboardConfiguration(dashboardConfiguration)
                        .build();
        newTransactionDefinitionSet.setKey(key);

        when(repository.findByKey(key)).thenReturn(new ArrayList<>());
        when(repository.save(any(TransactionDefinitionSet.class)))
                .thenReturn(newTransactionDefinitionSet);

        TransactionDefinitionSet savedTransactionDefinitionSet =
                service.save(key, newTransactionDefinitionSet);

        assertNotNull(savedTransactionDefinitionSet);
        assertEquals(key, savedTransactionDefinitionSet.getKey());

        verify(repository, times(1)).findByKey(key);
        verify(repository, times(1)).save(newTransactionDefinitionSet);
    }

    @Test
    void testSaveUpdateTransactionDefinitionSet() {
        String key = "key";
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder().key(key).constraints(new ArrayList<>()).build();

        transactionDefinitionSet.setKey(key);

        when(repository.findByKey(key)).thenReturn(List.of(transactionDefinitionSet));

        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().id(UUID.randomUUID()).build();

        TransactionDefinitionSet newTransactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .key(key)
                        .workflow("workflow")
                        .dashboardConfiguration(dashboardConfiguration)
                        .constraints(new ArrayList<>())
                        .build();

        when(repository.save(any(TransactionDefinitionSet.class)))
                .thenReturn(newTransactionDefinitionSet);

        TransactionDefinitionSet savedTransactionDefinitionSet =
                service.save(key, newTransactionDefinitionSet);

        assertNotNull(savedTransactionDefinitionSet);
        assertEquals(key, savedTransactionDefinitionSet.getKey());

        verify(repository, times(1)).findByKey(key);
        verify(repository, times(1)).save(newTransactionDefinitionSet);
    }

    @Test
    void testDeleteTransactionDefinitionSet() {
        // Create a TransactionDefinitionSet for testing
        String key = "key";
        DashboardConfiguration dashboardConfiguration =
                DashboardConfiguration.builder().id(UUID.randomUUID()).build();

        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .key(key)
                        .workflow("workflow")
                        .dashboardConfiguration(dashboardConfiguration)
                        .constraints(new ArrayList<>())
                        .build();

        // Mock the repository behavior when deleting the transactionDefinitionSet
        doNothing().when(repository).delete(transactionDefinitionSet);

        // Call the method to be tested
        service.deleteTransactionDefinitionSet(transactionDefinitionSet);

        // Verify that the delete method was called with the correct argument
        verify(repository, times(1)).delete(transactionDefinitionSet);
    }
}
