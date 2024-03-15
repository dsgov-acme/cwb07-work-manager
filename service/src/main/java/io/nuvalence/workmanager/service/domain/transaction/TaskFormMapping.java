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
import jakarta.persistence.Table;

/**
 * Maps form configuration to workflow task definition, optionally for a specific user role.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaction_task_form_mapping")
public class TaskFormMapping {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "task_definition_id", length = 64, nullable = false)
    private String taskDefinitionId;

    @Column(name = "form_id", length = 36, nullable = false)
    private UUID formId;

    @Column(name = "role", length = 1024, nullable = true)
    private String role;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TaskFormMapping that = (TaskFormMapping) o;
        return Objects.equals(taskDefinitionId, that.taskDefinitionId)
                && Objects.equals(formId, that.formId)
                && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskDefinitionId, formId, role);
    }
}
