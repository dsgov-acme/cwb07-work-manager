package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entity to represent documents provided by a customer for a transaction.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_provided_document")
@AccessResource(
        value = "customer_provided_document",
        translator = CustomerProvidedDocumentTranslator.class)
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class CustomerProvidedDocument implements UpdateTrackedEntity {
    @Id
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @Setter
    @Column(name = "review_status", length = 255, nullable = false)
    @Convert(converter = ReviewStatusConverter.class)
    private ReviewStatus reviewStatus;

    @OneToMany(
            mappedBy = "customerProvidedDocument",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<RejectionReason> rejectionReasons = new ArrayList<>();

    @Setter
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "transaction_id", nullable = false)
    @Convert(disableConversion = true)
    private UUID transactionId;

    @Setter
    @Column(name = "data_path", length = 255, nullable = false)
    private String dataPath;

    @Setter
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Setter
    @Column(name = "classifier", length = 255, nullable = false)
    private String classifier;

    @Setter
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Setter
    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Setter
    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Setter
    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    @Setter
    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Setter
    @Column(name = "reviewed_on")
    private OffsetDateTime reviewedOn;

    /**
     * Setter for rejected reasons, which adds the current instance of the customerProvidedDocument.
     *
     * @param rejectionReasons list of rejected reasons to add
     */
    public void setRejectionReasons(List<RejectionReason> rejectionReasons) {
        this.rejectionReasons = rejectionReasons;
        this.rejectionReasons.stream()
                .forEach(rejectedReason -> rejectedReason.setCustomerProvidedDocument(this));
    }
}
