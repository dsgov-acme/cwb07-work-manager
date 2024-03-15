package io.nuvalence.workmanager.service.domain.profile;

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
import java.util.Objects;
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
@AccessResource("employer_user_link")
@Table(name = "employer_user_link")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class EmployerUserLink implements ProfileUserLink, UpdateTrackedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne()
    @JoinColumn(name = "profile", nullable = false)
    private Employer profile;

    @Column(name = "user_id", nullable = false, columnDefinition = "pg-uuid")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID userId;

    @Column(name = "profile_access_level", nullable = false)
    @Convert(converter = ProfileAccessLevelConverter.class)
    private ProfileAccessLevel profileAccessLevel;

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
        if (!(o instanceof EmployerUserLink link)) return false;
        return Objects.equals(id, link.id)
                && Objects.equals(profile, link.profile)
                && profileAccessLevel == link.profileAccessLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, profile, profileAccessLevel);
    }
}
