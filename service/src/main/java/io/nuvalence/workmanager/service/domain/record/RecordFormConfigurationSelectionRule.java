package io.nuvalence.workmanager.service.domain.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
 * Defines the structure and behavior of a form config for records.
 */
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "record_form_configuration_selection_rule")
public class RecordFormConfigurationSelectionRule {

    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "viewer", nullable = false)
    private String viewer;

    @Column(name = "context", nullable = false)
    private String context;

    @Column(name = "form_configuration_key", nullable = false)
    private String formConfigurationKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_definition_id", referencedColumnName = "id")
    private RecordDefinition recordDefinition;
}
