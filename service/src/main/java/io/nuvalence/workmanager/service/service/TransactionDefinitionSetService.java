package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.models.TransactionDefinitionSetFilter;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;

import jakarta.transaction.Transactional;

/**
 * Service layer to manage transaction definition  sets.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class TransactionDefinitionSetService {

    private final TransactionDefinitionSetRepository repository;

    /**
     * Fetches all transaction definition sets from the database.
     *
     * @param filters transaction definition set filters
     * @return paged transaction definition set object
     */
    public Page<TransactionDefinitionSet> getAllTransactionDefinitionSets(
            final TransactionDefinitionSetFilter filters) {
        return repository.findAll(filters.getPageRequest());
    }

    /**
     * Fetches a transaction definition set from the database by key.
     *
     * @param key transaction definition set key to fetch
     * @return transaction definition set object
     */
    public Optional<TransactionDefinitionSet> getTransactionDefinitionSet(final String key) {
        // TODO: when versioning is implemented, this will need to select for the newest version
        return repository.findByKey(key).stream().findFirst();
    }

    /**
     * Saves a transaction definition set to the database.
     *
     * @param key transaction definition set key to save
     * @param transactionDefinitionSet transaction definition set object to save
     * @return transaction definition set object
     */
    public TransactionDefinitionSet save(
            final String key, final TransactionDefinitionSet transactionDefinitionSet) {
        Optional<TransactionDefinitionSet> existingTransactonDefinitionSetOptional =
                getTransactionDefinitionSet(key);
        if (existingTransactonDefinitionSetOptional.isPresent()) {
            TransactionDefinitionSet existingTransactionDefinitionSet =
                    existingTransactonDefinitionSetOptional.get();
            existingTransactionDefinitionSet.setWorkflow(transactionDefinitionSet.getWorkflow());
            existingTransactionDefinitionSet.setDashboardConfiguration(
                    transactionDefinitionSet.getDashboardConfiguration());
            if (existingTransactionDefinitionSet.getDashboardConfiguration() != null) {
                existingTransactionDefinitionSet
                        .getDashboardConfiguration()
                        .setTransactionDefinitionSet(existingTransactionDefinitionSet);
            }
            existingTransactionDefinitionSet.getConstraints().clear();
            existingTransactionDefinitionSet
                    .getConstraints()
                    .addAll(transactionDefinitionSet.getConstraints());
            return repository.save(existingTransactionDefinitionSet);
        }

        if (transactionDefinitionSet.getDashboardConfiguration() != null) {
            transactionDefinitionSet
                    .getDashboardConfiguration()
                    .setTransactionDefinitionSet(transactionDefinitionSet);
        }
        transactionDefinitionSet.setKey(key);
        return repository.save(transactionDefinitionSet);
    }

    public void deleteTransactionDefinitionSet(TransactionDefinitionSet transactionDefinitionSet) {
        repository.delete(transactionDefinitionSet);
    }
}
