package io.nuvalence.workmanager.service.domain.record;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entity for a record type.
 */
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@AccessResource("record_definition")
@Table(name = "record_definition")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class RecordDefinition implements UpdateTrackedEntity, Serializable {

    private static final long serialVersionUID = -498409885201L;

    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "record_definition_key", length = 255, nullable = false)
    private String key;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "schema_key", length = 255, nullable = false)
    private String schemaKey;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "expiration_duration", nullable = true)
    private Period expirationDuration;

    @Column(name = "created_by", updatable = false, length = 36, nullable = false)
    private String createdBy;

    @Column(name = "created_timestamp", updatable = false, nullable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    @OneToMany(
            mappedBy = "recordDefinition",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<FormConfiguration> formConfigurations;

    @OneToMany(
            mappedBy = "recordDefinition",
            orphanRemoval = true,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER)
    private List<RecordFormConfigurationSelectionRule> recordFormConfigurationSelectionRules;

    @Override
    public int hashCode() {
        return Objects.hash(
                key,
                name,
                description,
                schemaKey,
                expirationDuration,
                createdBy,
                createdTimestamp,
                lastUpdatedBy,
                lastUpdatedTimestamp,
                formConfigurations,
                recordFormConfigurationSelectionRules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }

        RecordDefinition that = (RecordDefinition) o;

        return Objects.equals(key, that.key)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(schemaKey, that.schemaKey)
                && Objects.equals(expirationDuration, that.expirationDuration)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(createdTimestamp, that.createdTimestamp)
                && Objects.equals(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equals(lastUpdatedTimestamp, that.lastUpdatedTimestamp)
                && Objects.equals(formConfigurations, that.formConfigurations)
                && Objects.equals(
                        recordFormConfigurationSelectionRules,
                        that.recordFormConfigurationSelectionRules);
    }
}
