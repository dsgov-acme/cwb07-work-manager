package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.generated.models.TransactionCountByStatusModel;
import io.nuvalence.workmanager.service.models.TransactionStatusCount;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Houses concrete Transaction repository method implementations.
 */
@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    private static final String PRIORITY = "priority";
    private static final String STATUS = "status";
    @PersistenceContext private EntityManager entityManager;

    @Override
    public List<TransactionCountByStatusModel> getTransactionCountsByStatus(
            Specification<Transaction> specifications) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TransactionStatusCount> query = cb.createQuery(TransactionStatusCount.class);
        Root<Transaction> root = query.from(Transaction.class);

        query.multiselect(root.get(STATUS), cb.count(root));
        query.select(cb.construct(TransactionStatusCount.class, root.get(STATUS), cb.count(root)));
        query.where(specifications.toPredicate(root, query, cb));
        query.groupBy(root.get(STATUS));
        return entityManager.createQuery(query).getResultList().stream()
                .map(
                        c -> {
                            TransactionCountByStatusModel count =
                                    new TransactionCountByStatusModel();
                            count.setCount(c.getCount().intValue());
                            count.setStatus(c.getStatus());
                            return count;
                        })
                .toList();
    }

    @Override
    public Map<String, Long> getTransactionCountsByStatusSimplified(
            Set<String> statuses, List<String> transactionDefinitionKeys) {
        Map<String, Long> resultMap = new HashMap<>();
        for (String status : statuses) {
            resultMap.put(status, 0L);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Transaction> root = query.from(Transaction.class);

        Predicate statusCondition = root.get(STATUS).in(statuses);
        Predicate transactionKeyCondition =
                root.get("transactionDefinitionKey").in(transactionDefinitionKeys);

        query.multiselect(root.get(STATUS), cb.count(root));
        query.where(cb.and(statusCondition, transactionKeyCondition));
        query.groupBy(root.get(STATUS));

        List<Tuple> results = entityManager.createQuery(query).getResultList();
        for (Tuple tuple : results) {
            String status = tuple.get(0, String.class);
            Long count = tuple.get(1, Long.class);
            resultMap.put(status, count);
        }

        return resultMap;
    }

    @Override
    public Map<String, Long> getTransactionCountsByPrioritySimplified(
            Set<String> priorities, List<String> transactionDefinitionKeys) {
        Set<TransactionPriority> enumPriorities =
                priorities.stream()
                        .map(TransactionPriority::fromStringValue)
                        .collect(Collectors.toSet());

        Map<String, Long> resultMap = new HashMap<>();
        for (TransactionPriority priority : enumPriorities) {
            resultMap.put(priority.toString(), 0L);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Transaction> root = query.from(Transaction.class);

        query.multiselect(root.get(PRIORITY), cb.count(root));
        Predicate priorityCondition = root.get(PRIORITY).in(enumPriorities);
        Predicate transactionKeyCondition =
                root.get("transactionDefinitionKey").in(transactionDefinitionKeys);
        query.where(cb.and(priorityCondition, transactionKeyCondition));
        query.groupBy(root.get(PRIORITY));

        List<Tuple> results = entityManager.createQuery(query).getResultList();
        for (Tuple tuple : results) {
            TransactionPriority priority = tuple.get(0, TransactionPriority.class);
            Long count = tuple.get(1, Long.class);
            resultMap.put(priority.toString(), count);
        }

        return resultMap;
    }
}
