package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.domain.Note;
import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
import io.nuvalence.workmanager.service.models.TransactionNoteFilters;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.models.auditevents.NoteAddedAuditEventDto;
import io.nuvalence.workmanager.service.repository.NoteTypeRepository;
import io.nuvalence.workmanager.service.repository.TransactionNoteRepository;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;

/**
 * Service for creating and retrieving notes.
 */
@RequiredArgsConstructor
@Service
@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class NoteService {
    private static final String TRANSACTION_NOT_FOUND = "Transaction not found";

    private final TransactionService transactionService;
    private final TransactionNoteRepository noteRepository;
    private final AuditEventService transactionAuditEventService;
    private final NoteTypeRepository noteTypeRepository;
    private final RequestContextTimestamp requestContextTimestamp;

    /**
     * Get Note By Id.
     *
     * @param transactionId transactionId
     * @param noteId noteId id
     *
     * @return note
     *
     * @throws NotFoundException if the transaction is not found.
     */
    public TransactionNote getByTransactionIdAndId(final UUID transactionId, final UUID noteId) {
        Optional<Transaction> optionalTransaction = getTransactionById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new NotFoundException(TRANSACTION_NOT_FOUND);
        }

        return noteRepository
                .findByTransactionIdAndId(transactionId, noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
    }

    /**
     * Get transaction notes.
     *
     * @param transactionId transaction identifier
     * @return notes
     *
     * @throws NotFoundException if the transaction is not found.
     */
    public List<TransactionNote> getNotesByTransactionId(final UUID transactionId) {
        Optional<Transaction> optionalTransaction = getTransactionById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new NotFoundException(TRANSACTION_NOT_FOUND);
        }

        return noteRepository.findByTransactionId(transactionId);
    }

    /**
     * Create a transaction note.
     *
     * @param transactionId transaction identifier
     * @param note note
     * @param noteTypeId id for note type.
     * @return note
     *
     * @throws NotFoundException if the transaction is not found.
     * @throws BusinessLogicException if the note type is not found.
     */
    public TransactionNote createTransactionNote(
            UUID transactionId, final Note note, final UUID noteTypeId) {
        Optional<NoteType> optionalNoteType = noteTypeRepository.findById(noteTypeId);
        if (optionalNoteType.isEmpty()) {
            throw new BusinessLogicException("Note type does not exist");
        }

        NoteType type = optionalNoteType.get();
        note.setType(type);

        Optional<Transaction> optionalTransaction = getTransactionById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new NotFoundException(TRANSACTION_NOT_FOUND);
        }

        TransactionNote transactionNote = new TransactionNote(transactionId, note);
        if (note.getDocuments() != null) {
            transactionNote.getDocuments().addAll(note.getDocuments());
        }

        transactionNote.setDeleted(false);

        return noteRepository.save(transactionNote);
    }

    /**
     * Update a transaction note.
     *
     * @param existingNote existing transaction to modify
     * @param requestNote note object with the info to update
     * @param noteTypeId id for note type .
     * @return updated note
     */
    public TransactionNote updateTransactionNote(
            TransactionNote existingNote, Note requestNote, UUID noteTypeId) {

        if (noteTypeId == null) {
            noteTypeId = existingNote.getType().getId();
        }

        NoteType type =
                noteTypeRepository
                        .findById(noteTypeId)
                        .orElseThrow(() -> new NotFoundException("Note type does not exist"));

        existingNote.setTitle(requestNote.getTitle());
        existingNote.setBody(requestNote.getBody());
        existingNote.setType(type);

        Set<UUID> uniqueDocuments = requestNote.getDocuments().stream().collect(Collectors.toSet());
        existingNote.setDocuments(new ArrayList<>(uniqueDocuments));

        return noteRepository.save(existingNote);
    }

    public List<NoteType> getAllNoteTypes() {
        return (List<NoteType>) noteTypeRepository.findAll();
    }

    /**
     * Posts an audit event for transaction notes.
     *
     * @param transactionId transaction identifier.
     * @param note related note.
     * @param activityType type of activity.
     * @return id of posted audit event.
     */
    public UUID postAuditEventForTransactionNote(
            UUID transactionId, Note note, @NotNull AuditActivityType activityType) {

        NoteAddedAuditEventDto noteInfo =
                new NoteAddedAuditEventDto(
                        note.getCreatedBy(), note.getId().toString(), note.getTitle());

        final String summary = "Transaction " + activityType.getValue().replace("_", " ") + ".";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(note.getCreatedBy())
                        .userId(note.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(transactionId)
                        .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                        .data(noteInfo.toJson(), activityType)
                        .build();

        return transactionAuditEventService.sendAuditEvent(auditEvent);
    }

    public Page<TransactionNote> getFilteredTransactionNotes(final TransactionNoteFilters filters) {
        return noteRepository.findAll(
                filters.getTransactionSpecifications(), filters.getPageRequest());
    }

    /**
     * Marks a transaction note as deleted.
     *
     * @param transactionId the transaction id to which the note is associated.
     * @param noteId the note id.
     *
     * @return deleted note.
     *
     * @throws NotFoundException if the note is not found or already deleted.
     */
    public TransactionNote softDeleteTransactionNote(UUID transactionId, UUID noteId) {
        Optional<TransactionNote> optionalNote =
                noteRepository.findByTransactionIdAndId(transactionId, noteId);

        if (optionalNote.isEmpty() || optionalNote.map(TransactionNote::getDeleted).orElse(true)) {
            throw new NotFoundException("Transaction note not found or deleted.");
        }

        TransactionNote transactionNote = optionalNote.get();
        transactionNote.setDeleted(true);
        transactionNote.setDeletedOn(requestContextTimestamp.getCurrentTimestamp());

        return noteRepository.save(transactionNote);
    }

    private Optional<Transaction> getTransactionById(UUID transactionId) {
        return transactionService.getTransactionById(transactionId);
    }
}
