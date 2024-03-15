package io.nuvalence.workmanager.service.domain.transaction;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.Note;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Note associated with a transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AccessResource("transaction-note")
@Entity
@DiscriminatorValue("TRANSACTION")
@Table(name = "transaction_note")
public class TransactionNote extends Note {

    @Convert(disableConversion = true)
    @Column(name = "transaction_id", length = 36, nullable = false)
    private UUID transactionId;

    /**
     * Creates a new transaction note.
     *
     * @param transactionId transaction ID
     * @param note          note
     */
    public TransactionNote(UUID transactionId, Note note) {
        this.id = note.getId();
        this.title = note.getTitle();
        this.body = note.getBody();
        this.transactionId = transactionId;
        this.type = note.getType();
    }
}
