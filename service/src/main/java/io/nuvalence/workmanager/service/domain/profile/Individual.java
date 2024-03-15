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

import java.time.OffsetDateTime;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@AccessResource("individual_profile")
@Table(name = "individual_profile")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class Individual implements Profile, UpdateTrackedEntity {

    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "ssn", nullable = false)
    private String ssn;

    @OneToOne(
            mappedBy = "individualForMailing",
            orphanRemoval = true,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER)
    private Address mailingAddress;

    @OneToOne(
            mappedBy = "individualForAddress",
            orphanRemoval = true,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER)
    private Address primaryAddress;

    @OneToMany(mappedBy = "profile", fetch = FetchType.EAGER)
    private List<IndividualUserLink> userLinks;

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
        Individual that = (Individual) o;
        return Objects.equal(id, that.id)
                && Objects.equal(ownerUserId, that.ownerUserId)
                && Objects.equal(ssn, that.ssn)
                && Objects.equal(mailingAddress, that.mailingAddress)
                && Objects.equal(primaryAddress, that.primaryAddress)
                && Objects.equal(createdBy, that.createdBy)
                && Objects.equal(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equal(createdTimestamp, that.createdTimestamp)
                && Objects.equal(lastUpdatedTimestamp, that.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                id,
                ownerUserId,
                ssn,
                createdBy,
                lastUpdatedBy,
                createdTimestamp,
                lastUpdatedTimestamp);
    }
}
