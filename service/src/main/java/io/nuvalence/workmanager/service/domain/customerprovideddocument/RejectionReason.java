package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Possible causes for a customer provided document to be rejected.
 */
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rejection_reason")
@Builder
@IdClass(RejectionReasonId.class)
public class RejectionReason {
    @Id
    @Column(name = "customer_provided_document_id", nullable = false)
    private UUID customerProvidedDocumentId;

    @Id
    @Getter
    @Setter
    @Column(name = "rejection_reason", nullable = false)
    @Convert(converter = RejectionReasonConverter.class)
    @Enumerated(EnumType.STRING)
    private RejectionReasonType rejectionReasonValue;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_provided_document_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false)
    private CustomerProvidedDocument customerProvidedDocument;

    public void setCustomerProvidedDocument(CustomerProvidedDocument customerProvidedDocument) {
        this.customerProvidedDocument = customerProvidedDocument;
        this.customerProvidedDocumentId = customerProvidedDocument.getId();
    }
}
