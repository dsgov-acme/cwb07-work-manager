package io.nuvalence.workmanager.service.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing a dashboard column configuration.
 */
@Data
@Entity
@Table(name = "dashboard_column_configuration")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardColumnConfiguration {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "column_label")
    private String columnLabel;

    @Column(name = "attribute_path")
    private String attributePath;

    @Column(name = "sortable")
    private Boolean sortable;

    @Column(name = "display_format")
    @Enumerated(EnumType.STRING)
    private DisplayFormat displayFormat;
}
