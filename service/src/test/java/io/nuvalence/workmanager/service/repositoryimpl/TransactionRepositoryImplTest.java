package io.nuvalence.workmanager.service.repositoryimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.generated.models.TransactionCountByStatusModel;
import io.nuvalence.workmanager.service.models.TransactionStatusCount;
import io.nuvalence.workmanager.service.repository.TransactionRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidAccessibilityAlteration")
class TransactionRepositoryImplTest {

    @Mock private EntityManager entityManager;

    @Mock private CriteriaBuilder criteriaBuilder;

    @Mock private CriteriaQuery<TransactionStatusCount> criteriaQuery;

    @Mock private CriteriaQuery<Tuple> criteriaTupleQuery;

    @Mock private Root<Transaction> root;

    @Mock private TypedQuery<TransactionStatusCount> typedQuery;

    @Mock private TypedQuery<Tuple> typedQueryTuple;

    private TransactionRepositoryImpl repository;

    @BeforeEach
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        repository = new TransactionRepositoryImpl();

        // Inject the mocked EntityManager using reflection
        Field entityManagerField =
                TransactionRepositoryImpl.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        entityManagerField.set(repository, entityManager);
    }

    @Test
    void testGetTransactionCountsByStatus() {
        // Mock data

        List<TransactionStatusCount> result = new ArrayList<>();
        result.add(new TransactionStatusCount("SUCCESS", 10L));
        result.add(new TransactionStatusCount("FAILED", 5L));

        // Mock Criteria API
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(TransactionStatusCount.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(Transaction.class)).thenReturn(root);
        when(criteriaBuilder.construct(
                        TransactionStatusCount.class,
                        root.get("status"),
                        criteriaBuilder.count(root)))
                .thenReturn((mock(CompoundSelection.class)));
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(result);
        Specification<Transaction> specifications = mock(Specification.class);
        // Invoke the method

        List<TransactionCountByStatusModel> transactionCounts =
                repository.getTransactionCountsByStatus(specifications);
        assertEquals(2, transactionCounts.size());
        assertEquals("SUCCESS", transactionCounts.get(0).getStatus());
        assertEquals(10, transactionCounts.get(0).getCount());
        assertEquals("FAILED", transactionCounts.get(1).getStatus());
        assertEquals(5, transactionCounts.get(1).getCount());

        // Verify the interactions
        verify(criteriaQuery)
                .select(
                        criteriaBuilder.construct(
                                TransactionStatusCount.class,
                                root.get("status"),
                                criteriaBuilder.count(root)));
        verify(criteriaQuery)
                .where(specifications.toPredicate(root, criteriaQuery, criteriaBuilder));
        verify(criteriaQuery).groupBy(root.get("status"));
        verify(typedQuery).getResultList();
    }

    @Test
    void testGetTransactionCountsByStatusSimplified() {
        Tuple draftTuple = mock(Tuple.class);
        when(draftTuple.get(0, String.class)).thenReturn("Draft");
        when(draftTuple.get(1, Long.class)).thenReturn(10L);

        Tuple reviewTuple = mock(Tuple.class);
        when(reviewTuple.get(0, String.class)).thenReturn("Review");
        when(reviewTuple.get(1, Long.class)).thenReturn(5L);

        List<Tuple> result = new ArrayList<>();
        result.add(draftTuple);
        result.add(reviewTuple);

        Path<String> statusPath = mock(Path.class);
        Path<String> transactionDefinitionKeyPath = mock(Path.class);
        when(root.<String>get("status")).thenReturn(statusPath);
        when(root.<String>get("transactionDefinitionKey")).thenReturn(transactionDefinitionKeyPath);

        CriteriaBuilder.In<String> inStatus = criteriaBuilder.in(statusPath);
        CriteriaBuilder.In<String> inTransactionKey =
                criteriaBuilder.in(transactionDefinitionKeyPath);

        when(statusPath.in(anySet())).thenReturn(inStatus);
        when(transactionDefinitionKeyPath.in(anyList())).thenReturn(inTransactionKey);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createTupleQuery()).thenReturn(criteriaTupleQuery);
        when(criteriaTupleQuery.from(Transaction.class)).thenReturn(root);

        when(entityManager.createQuery(criteriaTupleQuery)).thenReturn(typedQueryTuple);
        when(typedQueryTuple.getResultList()).thenReturn(result);

        List<String> keys = Collections.singletonList("keys");
        Map<String, Long> transactionCounts =
                repository.getTransactionCountsByStatusSimplified(Set.of("Draft", "Review"), keys);

        assertEquals(2, transactionCounts.size());
        assertEquals(10L, transactionCounts.get("Draft"));
        assertEquals(5L, transactionCounts.get("Review"));
    }

    @Test
    void testGetTransactionCountsByPrioritySimplified() {
        Tuple lowTuple = mock(Tuple.class);
        when(lowTuple.get(0, TransactionPriority.class)).thenReturn(TransactionPriority.LOW);
        when(lowTuple.get(1, Long.class)).thenReturn(3L);

        Tuple mediumTuple = mock(Tuple.class);
        when(mediumTuple.get(0, TransactionPriority.class)).thenReturn(TransactionPriority.MEDIUM);
        when(mediumTuple.get(1, Long.class)).thenReturn(8L);

        List<Tuple> result = new ArrayList<>();
        result.add(lowTuple);
        result.add(mediumTuple);

        Path<String> priorityPath = mock(Path.class);
        Path<String> transactionDefinitionKeyPath = mock(Path.class);
        when(root.<String>get("priority")).thenReturn(priorityPath);
        when(root.<String>get("transactionDefinitionKey")).thenReturn(transactionDefinitionKeyPath);

        CriteriaBuilder.In<String> inPriority = criteriaBuilder.in(priorityPath);
        CriteriaBuilder.In<String> inTransactionKey =
                criteriaBuilder.in(transactionDefinitionKeyPath);

        when(priorityPath.in(anySet())).thenReturn(inPriority);
        when(transactionDefinitionKeyPath.in(anyList())).thenReturn(inTransactionKey);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createTupleQuery()).thenReturn(criteriaTupleQuery);
        when(criteriaTupleQuery.from(Transaction.class)).thenReturn(root);

        when(entityManager.createQuery(criteriaTupleQuery)).thenReturn(typedQueryTuple);
        when(typedQueryTuple.getResultList()).thenReturn(result);

        List<String> keys = Collections.singletonList("keys");
        Map<String, Long> transactionCounts =
                repository.getTransactionCountsByPrioritySimplified(
                        Set.of(
                                TransactionPriority.LOW.toString(),
                                TransactionPriority.MEDIUM.toString()),
                        keys);

        assertEquals(2, transactionCounts.size());
        assertEquals(3L, transactionCounts.get(TransactionPriority.LOW.toString()));
        assertEquals(8L, transactionCounts.get(TransactionPriority.MEDIUM.toString()));
    }
}
