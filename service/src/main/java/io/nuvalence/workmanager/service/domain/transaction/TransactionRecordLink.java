package io.nuvalence.workmanager.service.domain.transaction;

import io.nuvalence.workmanager.service.domain.TransactionRecordLinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Relationship and configuration between a transaction definition and a record definition.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaction_record_link")
@ToString
@EqualsAndHashCode(exclude = "id")
public class TransactionRecordLink {

    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id", nullable = false)
    private Transaction transaction;

    @Column(name = "record_link_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionRecordLinkType recordLinkType;
}
