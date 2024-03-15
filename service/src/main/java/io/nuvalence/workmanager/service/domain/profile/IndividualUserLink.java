package io.nuvalence.workmanager.service.domain.profile;

import com.google.common.base.Objects;
import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@AccessResource("individual_user_link")
@Table(name = "individual_user_link")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class IndividualUserLink implements ProfileUserLink, UpdateTrackedEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private Individual profile;

    @Column(name = "user_id", nullable = false, columnDefinition = "pg-uuid")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID userId;

    @Column(name = "access", nullable = false)
    @Convert(converter = ProfileAccessLevelConverter.class)
    private ProfileAccessLevel accessLevel;

    @Column(name = "created_by", length = 36, nullable = false)
    protected String createdBy;

    @Column(name = "last_updated_by", length = 36, nullable = false)
    protected String lastUpdatedBy;

    @Column(name = "created_timestamp", nullable = false)
    protected OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndividualUserLink that = (IndividualUserLink) o;
        return Objects.equal(id, that.id)
                && Objects.equal(profile, that.profile)
                && accessLevel == that.accessLevel
                && Objects.equal(createdBy, that.createdBy)
                && Objects.equal(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equal(createdTimestamp, that.createdTimestamp)
                && Objects.equal(lastUpdatedTimestamp, that.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                id,
                profile,
                accessLevel,
                createdBy,
                lastUpdatedBy,
                createdTimestamp,
                lastUpdatedTimestamp);
    }
}
