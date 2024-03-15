package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.record.Record;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * Repository for Records.
 */
public interface RecordRepository
        extends CrudRepository<Record, UUID>, JpaSpecificationExecutor<Record> {

    @Query(value = "SELECT nextval('record_sequence')", nativeQuery = true)
    Long getNextTransactionSequenceValue();

    @Modifying
    @Query(
            "UPDATE Record r SET r.status = 'Expired' WHERE r.expires < CURRENT_TIMESTAMP AND"
                    + " r.status <> 'Expired'")
    int updateStatusForExpiredRecords();
}
