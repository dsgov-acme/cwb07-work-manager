package io.nuvalence.workmanager.service.domain.transaction;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.nuvalence.auth.access.AccessResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entity representing a dashboard configuration.
 */
@Data
@Entity
@Table(name = "dashboard_configuration")
@AccessResource(value = "dashboard_configuration")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardConfiguration {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "dashboard_label")
    private String dashboardLabel;

    @Column(name = "menu_icon")
    private String menuIcon;

    @Fetch(FetchMode.SELECT)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "dashboard_configuration_id")
    private List<DashboardColumnConfiguration> columns;

    @Fetch(FetchMode.SELECT)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "dashboard_configuration_id")
    private List<DashboardTabConfiguration> tabs;

    @Fetch(FetchMode.SELECT)
    @OneToOne
    @JoinColumn(name = "transaction_definition_set_id", nullable = false)
    @JsonBackReference
    private TransactionDefinitionSet transactionDefinitionSet;
}
