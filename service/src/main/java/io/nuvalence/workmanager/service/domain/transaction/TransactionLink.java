package io.nuvalence.workmanager.service.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Describes the linked relationship between two transactions based on direction (to/from).
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaction_link")
public class TransactionLink {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "from_transaction_id")
    private UUID fromTransactionId;

    @Column(name = "to_transaction_id")
    private UUID toTransactionId;

    @OneToOne
    @JoinColumn(name = "transaction_link_type_id", nullable = false)
    @Builder.Default
    private TransactionLinkType transactionLinkType = null;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionLink that = (TransactionLink) o;
        return Objects.equals(id, that.id)
                && Objects.equals(fromTransactionId, that.fromTransactionId)
                && Objects.equals(toTransactionId, that.toTransactionId)
                && Objects.equals(transactionLinkType, that.transactionLinkType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromTransactionId, toTransactionId, transactionLinkType);
    }
}
