package io.nuvalence.workmanager.service.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entity representing a set of transaction definitions.
 */
@Entity
@Table(name = "transaction_definition_set_order")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDefinitionSetOrder {
    @Id
    @Column(name = "sort_order", unique = true, nullable = false)
    private Integer sortOrder;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_definition_set", referencedColumnName = "id")
    private TransactionDefinitionSet transactionDefinitionSet;
}
