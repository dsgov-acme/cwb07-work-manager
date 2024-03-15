package io.nuvalence.workmanager.service.domain.profile;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@AccessResource("employer_profile")
@Table(name = "employer_profile")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class Employer implements Profile, UpdateTrackedEntity {

    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fein", nullable = false)
    private String fein;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "employer_profile_other_names",
            joinColumns = @JoinColumn(name = "employer_profile_id"))
    @Column(name = "other_names", nullable = false)
    private List<String> otherNames = new ArrayList<>();

    @Column(name = "business_type", nullable = false)
    private String type;

    @Column(name = "industry", nullable = false)
    private String industry;

    @Column(name = "summary_of_business", nullable = false)
    private String summaryOfBusiness;

    @Column(name = "business_phone", nullable = false)
    private String businessPhone;

    @OneToOne(
            mappedBy = "employerForMailing",
            orphanRemoval = true,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER)
    private Address mailingAddress;

    @OneToMany(
            mappedBy = "employerForLocations",
            orphanRemoval = true,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER)
    private List<Address> locations;

    @OneToMany(mappedBy = "profile", fetch = FetchType.EAGER)
    private List<EmployerUserLink> userLinks;

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
        if (!(o instanceof Employer employer)) return false;
        return Objects.equals(id, employer.id)
                && Objects.equals(fein, employer.fein)
                && Objects.equals(legalName, employer.legalName)
                && Objects.equals(otherNames, employer.otherNames)
                && Objects.equals(type, employer.type)
                && Objects.equals(industry, employer.industry)
                && Objects.equals(summaryOfBusiness, employer.summaryOfBusiness)
                && Objects.equals(businessPhone, employer.businessPhone)
                && Objects.equals(mailingAddress, employer.mailingAddress)
                && Objects.equals(locations, employer.locations)
                && Objects.equals(createdBy, employer.createdBy)
                && Objects.equals(lastUpdatedBy, employer.lastUpdatedBy)
                && Objects.equals(createdTimestamp, employer.createdTimestamp)
                && Objects.equals(lastUpdatedTimestamp, employer.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                fein,
                legalName,
                otherNames,
                type,
                industry,
                summaryOfBusiness,
                businessPhone,
                createdBy,
                lastUpdatedBy,
                createdTimestamp,
                lastUpdatedTimestamp);
    }
}
