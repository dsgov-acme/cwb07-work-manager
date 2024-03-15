package io.nuvalence.workmanager.service.domain.profile;

import com.google.common.base.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "profile_invitation")
public class ProfileInvitation {

    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false, columnDefinition = "uuid")
    private UUID profileId;

    @Column(name = "profile_type")
    @Convert(converter = ProfileTypeConverter.class)
    private ProfileType type;

    @Column(name = "profile_access_level", nullable = false)
    @Convert(converter = ProfileAccessLevelConverter.class)
    private ProfileAccessLevel accessLevel;

    @Column(name = "expires")
    private OffsetDateTime expires;

    @Column(name = "claimed", nullable = false)
    private Boolean claimed;

    @Column(name = "email")
    private String email;

    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "claimed_timestamp")
    private OffsetDateTime claimedTimestamp;

    @PrePersist
    protected void onCreate() {
        createdTimestamp = OffsetDateTime.now();
        expires = createdTimestamp.plusDays(7);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileInvitation that = (ProfileInvitation) o;
        return Objects.equal(profileId, that.profileId)
                && type == that.type
                && accessLevel == that.accessLevel
                && Objects.equal(expires, that.expires)
                && Objects.equal(claimed, that.claimed)
                && Objects.equal(email, that.email)
                && Objects.equal(createdTimestamp, that.createdTimestamp)
                && Objects.equal(claimedTimestamp, that.claimedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                profileId,
                type,
                accessLevel,
                expires,
                claimed,
                email,
                createdTimestamp,
                claimedTimestamp);
    }
}
