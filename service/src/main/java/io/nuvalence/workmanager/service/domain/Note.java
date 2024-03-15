package io.nuvalence.workmanager.service.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Base class for all notes.
 */
@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@Table(name = "note")
@EntityListeners(UpdateTrackedEntityEventListener.class)
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class Note implements UpdateTrackedEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    protected UUID id;

    @Column(name = "title")
    protected String title;

    @Column(name = "body")
    protected String body;

    @Convert(disableConversion = true)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "note_document",
            joinColumns = @JoinColumn(name = "note_id", nullable = false))
    @Column(name = "document_id")
    protected List<UUID> documents = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "note_type_id", nullable = false)
    protected NoteType type;

    @Column(name = "created_by", length = 36, nullable = false)
    protected String createdBy;

    @Column(name = "last_updated_by", length = 36, nullable = false)
    protected String lastUpdatedBy;

    @Column(name = "created_timestamp", nullable = false)
    protected OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    @Getter
    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @Getter
    @Column(name = "deleted_On")
    private OffsetDateTime deletedOn;
}
