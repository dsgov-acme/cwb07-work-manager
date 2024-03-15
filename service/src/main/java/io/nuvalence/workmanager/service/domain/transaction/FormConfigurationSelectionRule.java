package io.nuvalence.workmanager.service.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * Configures selection rules for determining request form configuration.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaction_form_selection_rule")
public class FormConfigurationSelectionRule {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "task", length = 1024, nullable = true)
    private String task;

    @Column(name = "viewer", length = 1024, nullable = true)
    private String viewer;

    @Column(name = "context", length = 1024, nullable = true)
    private String context;

    @Column(name = "form_configuration_key", length = 1024, nullable = false)
    private String formConfigurationKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_definition_id", referencedColumnName = "id")
    private TransactionDefinition transactionDefinition;

    /**
     * Returns true if this rule matches inputs.
     *
     * @param task task name the form applies to
     * @param viewer view type the form applies to
     * @param context context the form applies to
     * @return true if rule matches input
     */
    public boolean matches(final String task, final String viewer, final String context) {
        return (this.task == null || this.task.equals(task))
                && (this.viewer == null || this.viewer.equals(viewer))
                && (this.context == null || this.context.equals(context));
    }
}
