package io.nuvalence.workmanager.service.domain.securemessaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Possible types for notes.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "message")
@Builder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Message {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "message_sender_id", nullable = false, referencedColumnName = "id")
    private MessageSender sender;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "body", nullable = false)
    private String body;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_attachment", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "attachment_id")
    private List<UUID> attachments = new ArrayList<>();

    @Column(name = "original_message", nullable = false)
    private boolean originalMessage;

    @PrePersist
    public void setTimestamp() {
        this.timestamp = OffsetDateTime.now();
    }
}
