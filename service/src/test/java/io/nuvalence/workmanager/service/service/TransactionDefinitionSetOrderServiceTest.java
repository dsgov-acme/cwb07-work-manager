package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetOrder;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionSetOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

/**
 * Tests for service to manage transaction definition set order.
 */
@ExtendWith(MockitoExtension.class)
class TransactionDefinitionSetOrderServiceTest {

    @Mock private TransactionDefinitionSetOrderRepository repository;
    @Mock private TransactionDefinitionSetService transactionDefinitionSetService;

    private TransactionDefinitionSetOrderService service;

    @BeforeEach
    void init() {
        service =
                new TransactionDefinitionSetOrderService(
                        repository, transactionDefinitionSetService);
    }

    @Test
    void testUpdateTransactionSetKeyOrder() {
        TransactionDefinitionSet transactionDefinitionSetA =
                TransactionDefinitionSet.builder().key("A").build();
        when(transactionDefinitionSetService.getTransactionDefinitionSet("A"))
                .thenReturn(Optional.ofNullable(transactionDefinitionSetA));

        List<String> newOrder = List.of("A", "B");

        TransactionDefinitionSet transactionDefinitionSetB =
                TransactionDefinitionSet.builder().key("B").build();
        when(transactionDefinitionSetService.getTransactionDefinitionSet("B"))
                .thenReturn(Optional.ofNullable(transactionDefinitionSetB));

        when(repository.save(any())).thenReturn(null);

        service.updateTransactionSetKeyOrder(newOrder);

        verify(repository, times(1)).deleteAll();
        verify(repository, times(2)).save(any());
    }

    @Test
    void testGetTransactionDefinitionSetOrder() {
        TransactionDefinitionSetOrder transactionDefinitionSetOrderA =
                TransactionDefinitionSetOrder.builder()
                        .sortOrder(2)
                        .transactionDefinitionSet(
                                TransactionDefinitionSet.builder().key("A").build())
                        .build();
        TransactionDefinitionSetOrder transactionDefinitionSetOrderB =
                TransactionDefinitionSetOrder.builder()
                        .sortOrder(1)
                        .transactionDefinitionSet(
                                TransactionDefinitionSet.builder().key("B").build())
                        .build();
        when(repository.findAll())
                .thenReturn(
                        List.of(transactionDefinitionSetOrderA, transactionDefinitionSetOrderB));

        List<String> transactionDefinitionSetOrder =
                service.getTransactionDefinitionSetOrderAsString();

        assertEquals(2, transactionDefinitionSetOrder.size());
        assertEquals("B", transactionDefinitionSetOrder.get(0));
        assertEquals("A", transactionDefinitionSetOrder.get(1));
    }
}
