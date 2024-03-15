package io.nuvalence.workmanager.service.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing a dashboard tab configuration.
 */
@Data
@Entity
@Table(name = "dashboard_tab_configuration")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTabConfiguration {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "tab_label")
    private String tabLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tab_filter", columnDefinition = "json")
    private Map<String, Object> filter;
}
