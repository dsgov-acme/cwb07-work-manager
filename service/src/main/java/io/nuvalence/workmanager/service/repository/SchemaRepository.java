package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Schemas.
 */
public interface SchemaRepository
        extends JpaRepository<SchemaRow, UUID>, JpaSpecificationExecutor<SchemaRow> {

    Optional<SchemaRow> findByKey(String key);

    @Query(
            nativeQuery = true,
            value =
                    "WITH RECURSIVE nested_schemas AS ("
                            + "  SELECT ds1.* "
                            + "  FROM dynamic_schema ds1 "
                            + "  JOIN parent_child_schema pcs ON ds1.id = pcs.child_id "
                            + "  WHERE ds1.key = ?1 "
                            + "  UNION ALL "
                            + "  SELECT ds2.* "
                            + "  FROM dynamic_schema ds2 "
                            + "  JOIN parent_child_schema pcs ON ds2.id = pcs.parent_id "
                            + "  JOIN nested_schemas ns ON pcs.child_id = ns.id "
                            + ") "
                            + "SELECT DISTINCT * "
                            + "FROM nested_schemas "
                            + "WHERE key <> ?1")
    List<SchemaRow> getSchemaParents(String childKey);
}
