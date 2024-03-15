package io.nuvalence.workmanager.service.domain.transaction;

import io.nuvalence.auth.access.AccessResource;
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
 * Link type that is allowed for a transaction definition.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@AccessResource("allowed_link")
@Table(name = "allowed_link")
public class AllowedLink {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "transaction_definition_key")
    private String transactionDefinitionKey;

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

        AllowedLink that = (AllowedLink) o;
        return Objects.equals(id, that.id)
                && Objects.equals(transactionDefinitionKey, that.transactionDefinitionKey)
                && Objects.equals(transactionLinkType, that.transactionLinkType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionDefinitionKey, transactionLinkType);
    }
}
