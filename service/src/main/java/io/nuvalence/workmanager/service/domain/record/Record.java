package io.nuvalence.workmanager.service.domain.record;

import com.google.common.base.Objects;
import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.RecordAccessResourceTranslator;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntityContainer;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntityContainerEventListener;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Defines the structure and behavior of a record.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "record")
@AccessResource(value = "record", translator = RecordAccessResourceTranslator.class)
@ToString(exclude = {"data"})
@EntityListeners({
    DynamicEntityContainerEventListener.class,
    UpdateTrackedEntityEventListener.class
})
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class Record implements DynamicEntityContainer, UpdateTrackedEntity {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(name = "external_id")
    private String externalId;

    @Column(name = "record_definition_key", nullable = false)
    private String recordDefinitionKey;

    @ManyToOne
    @JoinColumn(name = "record_definition_id", referencedColumnName = "id")
    private RecordDefinition recordDefinition;

    @Setter
    @Column(name = "status", nullable = false)
    private String status;

    @Setter
    @Column(name = "expires", nullable = false)
    private OffsetDateTime expires;

    @Setter
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Setter
    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Setter
    @ManyToOne
    @JoinColumn(name = "created_from", referencedColumnName = "id", updatable = false)
    private Transaction createdFrom;

    @Setter
    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Setter
    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    @Setter
    @ManyToOne
    @JoinColumn(name = "last_updated_from", referencedColumnName = "id")
    private Transaction lastUpdatedFrom;

    @Setter @Embedded private DynamicEntity data;

    /**
     * Creates a new instance of a Record.
     *
     * @param id the transaction id
     * @param externalId the external id
     * @param recordDefinitionKey the record definition key
     * @param recordDefinition the record definition
     * @param status the status
     * @param expires the expiration date
     * @param createdBy the user who created the record
     * @param lastUpdatedBy the user who last updated the record
     * @param createdFrom the transaction that created this record
     * @param createdTimestamp the timestamp when the record was created
     * @param lastUpdatedTimestamp the timestamp when the record was last updated
     * @param data the record data
     */
    @SuppressWarnings("java:S107")
    @Builder(toBuilder = true)
    public Record(
            UUID id,
            String externalId,
            String recordDefinitionKey,
            RecordDefinition recordDefinition,
            String status,
            OffsetDateTime expires,
            String createdBy,
            String lastUpdatedBy,
            Transaction createdFrom,
            Transaction lastUpdatedFrom,
            OffsetDateTime createdTimestamp,
            OffsetDateTime lastUpdatedTimestamp,
            DynamicEntity data) {
        this.id = id;
        this.externalId = externalId;
        this.recordDefinitionKey = recordDefinitionKey;
        this.recordDefinition = recordDefinition;
        this.status = status;
        this.expires = expires;
        this.createdBy = createdBy;
        this.lastUpdatedBy = lastUpdatedBy;
        this.createdFrom = createdFrom;
        this.lastUpdatedFrom = lastUpdatedFrom;
        this.createdTimestamp = createdTimestamp;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Record that = (Record) o;
        return Objects.equal(id, that.id)
                && Objects.equal(externalId, that.externalId)
                && Objects.equal(recordDefinitionKey, that.recordDefinitionKey)
                && Objects.equal(recordDefinition, that.recordDefinition)
                && Objects.equal(status, that.status)
                && Objects.equal(expires, that.expires)
                && Objects.equal(createdBy, that.createdBy)
                && Objects.equal(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equal(createdFrom, that.createdFrom)
                && Objects.equal(createdTimestamp, that.createdTimestamp)
                && Objects.equal(lastUpdatedTimestamp, that.lastUpdatedTimestamp)
                && Objects.equal(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                id,
                externalId,
                recordDefinitionKey,
                recordDefinition,
                status,
                expires,
                createdBy,
                lastUpdatedBy,
                createdFrom,
                createdTimestamp,
                lastUpdatedTimestamp,
                data);
    }

    @PrePersist
    @PreUpdate
    public void recordPrePersistAndUpdate() {
        this.externalId = this.getExternalId().toUpperCase(Locale.ROOT);
    }
}
