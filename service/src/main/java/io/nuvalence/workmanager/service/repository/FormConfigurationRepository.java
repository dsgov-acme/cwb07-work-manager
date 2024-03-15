package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Form Configurations.
 */
public interface FormConfigurationRepository extends CrudRepository<FormConfiguration, UUID> {

    List<FormConfiguration> findByTransactionDefinitionKey(String transactionDefinitionKey);

    List<FormConfiguration> findByRecordDefinitionKey(String recordDefinitionKey);

    List<FormConfiguration> findByKey(String formConfigurationKey);

    List<FormConfiguration> findByKeyAndRecordDefinitionKey(String key, String recordDefinitionKey);

    @Query(
            "SELECT fc FROM FormConfiguration fc WHERE fc.transactionDefinitionKey ="
                    + " :transactionDefinitionKey AND fc.key = :key")
    List<FormConfiguration> searchByKeys(
            @Param("transactionDefinitionKey") String transactionDefinitionKey,
            @Param("key") String key);
}
