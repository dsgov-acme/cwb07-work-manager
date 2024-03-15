package io.nuvalence.workmanager.service.domain.transaction;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entity representing a set of transaction definitions.
 */
@AccessResource("transaction_definition_set")
@Data
@EntityListeners(UpdateTrackedEntityEventListener.class)
@Entity
@Table(name = "transaction_definition_set")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class TransactionDefinitionSet implements UpdateTrackedEntity {

    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "transaction_definition_set_key")
    private String key;

    @Column(name = "workflow")
    private String workflow;

    @Fetch(FetchMode.SELECT)
    @OneToOne(
            mappedBy = "transactionDefinitionSet",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonManagedReference
    private DashboardConfiguration dashboardConfiguration;

    @Fetch(FetchMode.SELECT)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_definition_set_id", nullable = false)
    private List<TransactionDefinitionSetDataRequirement> constraints = new ArrayList<>();

    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Column(name = "created_timestamp")
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp")
    private OffsetDateTime lastUpdatedTimestamp;

    @Formula(
            "(select t.sort_order from transaction_definition_set_order t where"
                    + " t.transaction_definition_set = id)")
    private Integer sortOrder;
}
