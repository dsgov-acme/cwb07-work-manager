package io.nuvalence.workmanager.service.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaction_record_linker")
public class TransactionRecordLinker implements Serializable {

    private static final long serialVersionUID = 987654321L;

    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "record_definition_key", nullable = false)
    private String recordDefinitionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mappings", nullable = false, columnDefinition = "json")
    private Map<String, String> fieldMappings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_definition_id", referencedColumnName = "id")
    private TransactionDefinition transactionDefinition;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionRecordLinker that = (TransactionRecordLinker) o;

        UUID transactionDefinitionId =
                this.transactionDefinition != null ? this.transactionDefinition.getId() : null;

        UUID thatTransactionDefinitionId =
                that.getTransactionDefinition() != null
                        ? that.getTransactionDefinition().getId()
                        : null;

        return Objects.equals(id, that.id)
                && Objects.equals(recordDefinitionKey, that.recordDefinitionKey)
                && Objects.equals(fieldMappings, that.fieldMappings)
                && Objects.equals(transactionDefinitionId, thatTransactionDefinitionId);
    }

    @Override
    public int hashCode() {

        UUID transactionDefinitionId =
                this.transactionDefinition != null ? this.transactionDefinition.getId() : null;
        return Objects.hash(id, recordDefinitionKey, fieldMappings, transactionDefinitionId);
    }
}
