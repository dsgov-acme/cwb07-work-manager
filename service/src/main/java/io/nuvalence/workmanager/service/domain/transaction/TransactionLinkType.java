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
import jakarta.persistence.Table;

/**
 * A type of link between two transaction definitions.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@AccessResource("transaction_link_type")
@Table(name = "transaction_link_type")
public class TransactionLinkType {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "from_description")
    private String fromDescription;

    @Column(name = "to_description")
    private String toDescription;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionLinkType that = (TransactionLinkType) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(fromDescription, that.fromDescription)
                && Objects.equals(toDescription, that.toDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, fromDescription, toDescription);
    }
}
