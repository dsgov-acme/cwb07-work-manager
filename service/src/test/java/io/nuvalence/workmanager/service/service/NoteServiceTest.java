package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.config.exceptions.ApiException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final String TRANSACTION_NOT_FOUND = "Transaction not found";

    @Mock private TransactionService transactionService;

    @Mock private TransactionNoteRepository noteRepository;

    @Mock private AuditEventService transactionAuditEventService;

    @Mock private NoteTypeRepository noteTypeRepository;

    @Mock private RequestContextTimestamp requestContextTimestamp;

    private NoteService noteService;

    @BeforeEach
    public void setUp() {
        noteService =
                spy(
                        new NoteService(
                                transactionService,
                                noteRepository,
                                transactionAuditEventService,
                                noteTypeRepository,
                                requestContextTimestamp));
    }

    @Test
    void testGetNoteById() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote note = new TransactionNote();
        List<UUID> documentList = new ArrayList<>();
        note.setId(noteId);
        note.setTransactionId(transactionId);
        note.setTitle("title");
        note.setBody("body");
        note.setCreatedBy(UUID.randomUUID().toString());
        note.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        note.setDocuments(documentList);
        Transaction transaction = new Transaction();

        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(noteRepository.findByTransactionIdAndId(transactionId, noteId))
                .thenReturn(Optional.of(note));

        Note returnedNote = noteService.getByTransactionIdAndId(transactionId, noteId);

        assertEquals(note.getId(), returnedNote.getId());
    }

    @Test
    void testGetNotesByTransactionId() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote note = new TransactionNote();
        List<UUID> documentList = new ArrayList<>();
        note.setId(noteId);
        note.setTransactionId(transactionId);
        note.setTitle("title");
        note.setBody("body");
        note.setCreatedBy(UUID.randomUUID().toString());
        note.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        note.setDocuments(documentList);
        Transaction transaction = new Transaction();

        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(noteRepository.findByTransactionId(transactionId)).thenReturn(List.of(note));

        List<TransactionNote> returnedNotes = noteService.getNotesByTransactionId(transactionId);

        assertEquals(1L, returnedNotes.size());
        assertEquals(note.getId(), returnedNotes.get(0).getId());
    }

    @Test
    void testCreateTransactionNote() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote note = new TransactionNote();
        List<UUID> documentList = new ArrayList<>();
        note.setId(noteId);
        note.setTransactionId(transactionId);
        note.setTitle("title");
        note.setBody("body");
        note.setCreatedBy(UUID.randomUUID().toString());
        note.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        note.setDocuments(documentList);
        Transaction transaction = new Transaction();
        String testName = "testName";
        NoteType noteType = new NoteType(UUID.randomUUID(), testName);

        when(noteTypeRepository.findById(noteId)).thenReturn(Optional.of(noteType));
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(noteRepository.save(any())).thenReturn(note);

        Note returnedNote = noteService.createTransactionNote(transactionId, note, noteId);

        assertEquals(note.getId(), returnedNote.getId());
    }

    @Test
    void testCreateTransactionNoteNoteTypeNotFound() {
        UUID transactionId = UUID.randomUUID();

        TransactionNote note = new TransactionNote();
        UUID noteId = UUID.randomUUID();
        note.setId(noteId);

        when(noteTypeRepository.findById(noteId)).thenReturn(Optional.empty());

        BusinessLogicException thrown =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            noteService.createTransactionNote(transactionId, note, noteId);
                        });

        assertEquals("Note type does not exist", thrown.getMessage());
    }

    @Test
    void testPostAuditEventForTransactionNote() throws Exception {
        postUpdateEventForTransactionNote(AuditActivityType.NOTE_ADDED);
        Mockito.reset(transactionAuditEventService);
        postUpdateEventForTransactionNote(AuditActivityType.NOTE_DELETED);
    }

    private void postUpdateEventForTransactionNote(AuditActivityType activityType)
            throws ApiException {

        final UUID transactionId = UUID.randomUUID();
        final UUID noteId = UUID.randomUUID();
        Note note = new Note();
        note.setId(noteId);
        note.setTitle("Test Note");
        note.setCreatedBy(UUID.randomUUID().toString());
        note.setCreatedTimestamp(OffsetDateTime.now());

        NoteAddedAuditEventDto noteInfo =
                new NoteAddedAuditEventDto(
                        note.getCreatedBy(), note.getId().toString(), note.getTitle());

        final String summary = "Transaction " + activityType.getValue().replace("_", " ") + ".";
        UUID expectedEventId = UUID.randomUUID();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(note.getCreatedBy())
                        .userId(note.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(transactionId)
                        .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                        .data(noteInfo.toJson(), activityType)
                        .build();

        when(transactionAuditEventService.sendAuditEvent(any(AuditEventRequestObjectDto.class)))
                .thenReturn(expectedEventId);

        UUID resultEventId =
                noteService.postAuditEventForTransactionNote(transactionId, note, activityType);

        assertEquals(expectedEventId, resultEventId);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void testUpdateTransactionNote_success() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote existingNote = new TransactionNote();
        List<UUID> documentList = new ArrayList<>();
        existingNote.setId(noteId);
        existingNote.setTransactionId(transactionId);
        existingNote.setTitle("title");
        existingNote.setBody("body");
        existingNote.setCreatedBy(UUID.randomUUID().toString());
        existingNote.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        existingNote.setDocuments(documentList);

        TransactionNote note = new TransactionNote();
        note.setBody("otherbody");

        String noteTypeName = "testName";
        NoteType noteType = new NoteType(UUID.randomUUID(), noteTypeName);

        when(noteTypeRepository.findById(noteId)).thenReturn(Optional.of(noteType));
        when(noteRepository.save(any())).thenReturn(existingNote);

        Note returnedNote = noteService.updateTransactionNote(existingNote, note, noteId);

        assertEquals(existingNote.getId(), returnedNote.getId());
        assertEquals(note.getBody(), returnedNote.getBody());
    }

    @Test
    void testUpdateTransactionNote_noteTypeNotFound() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote existingNote = new TransactionNote();
        List<UUID> documentList = new ArrayList<>();
        existingNote.setId(noteId);
        existingNote.setTransactionId(transactionId);
        existingNote.setTitle("title");
        existingNote.setBody("body");
        existingNote.setCreatedBy(UUID.randomUUID().toString());
        existingNote.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        existingNote.setDocuments(documentList);

        TransactionNote note = new TransactionNote();
        note.setBody("otherbody");

        when(noteTypeRepository.findById(noteId)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> {
                    noteService.updateTransactionNote(existingNote, note, noteId);
                });
    }

    @Test
    void testUpdateTransactionNote_UpdateAttachments() {

        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote existingNote = new TransactionNote();
        existingNote.setId(noteId);
        existingNote.setTransactionId(transactionId);
        existingNote.setTitle("title");
        existingNote.setBody("body");
        existingNote.setCreatedBy(UUID.randomUUID().toString());
        existingNote.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        List<UUID> documentList = List.of(UUID.randomUUID(), UUID.randomUUID());
        existingNote.setDocuments(documentList);

        TransactionNote note = new TransactionNote();
        note.setBody("otherbody");
        UUID documentWanted = UUID.randomUUID();
        note.setDocuments(List.of(documentWanted, documentWanted));

        String noteTypeName = "testName";
        NoteType noteType = new NoteType(UUID.randomUUID(), noteTypeName);

        when(noteTypeRepository.findById(noteId)).thenReturn(Optional.of(noteType));
        when(noteRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        Note returnedNote = noteService.updateTransactionNote(existingNote, note, noteId);

        assertEquals(existingNote.getId(), returnedNote.getId());
        assertEquals(note.getBody(), returnedNote.getBody());
        assertEquals(1, returnedNote.getDocuments().size());
        assertEquals(documentWanted, returnedNote.getDocuments().get(0));
    }

    @Test
    void getFilteredNotes() {
        final UUID transactionId = UUID.randomUUID();

        final Note note1 = new Note();
        note1.setType(new NoteType(UUID.randomUUID(), "test-type-one"));
        note1.setCreatedTimestamp(OffsetDateTime.now());
        final TransactionNote transactionNote1 = new TransactionNote(UUID.randomUUID(), note1);
        transactionNote1.setTransactionId(transactionId);

        final Note note2 = new Note();
        note2.setType(new NoteType(UUID.randomUUID(), "test-type-two"));
        note2.setCreatedTimestamp(OffsetDateTime.now());
        final TransactionNote transactionNote2 = new TransactionNote(transactionId, note2);
        transactionNote2.setTransactionId(transactionId);

        final TransactionNoteFilters searchTransactionsFilters =
                TransactionNoteFilters.builder()
                        .transactionId(UUID.randomUUID())
                        .sortBy("createdTimestamp")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(2)
                        .build();

        final Page<TransactionNote> pagedResults =
                new PageImpl<>(List.of(transactionNote1, transactionNote2));

        when(noteRepository.findAll((Specification<TransactionNote>) any(), (Pageable) any()))
                .thenReturn(pagedResults);

        assertEquals(
                pagedResults, noteService.getFilteredTransactionNotes(searchTransactionsFilters));
    }

    @Test
    void testCreateTransactionNoteValidateCreatedByNull() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        List<UUID> documentList = new ArrayList<>();
        note.setId(noteId);
        note.setTransactionId(transactionId);
        note.setTitle("title");
        note.setBody("body");
        note.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        note.setDocuments(documentList);
        Transaction transaction = new Transaction();

        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(noteRepository.save(any())).thenReturn(note);

        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        UUID noteTypeId = UUID.randomUUID();
        when(noteTypeRepository.findById(noteTypeId)).thenReturn(Optional.of(new NoteType()));

        Note returnedNote = noteService.createTransactionNote(transactionId, note, noteTypeId);
        assertNull(returnedNote.getCreatedBy());
    }

    @Test
    void testCreateTransactionNoteValidateCreatedBy() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        String createdBy = "EXT000123";
        TransactionNote note = new TransactionNote();
        List<UUID> documentList = new ArrayList<>();
        note.setId(noteId);
        note.setTransactionId(transactionId);
        note.setTitle("title");
        note.setBody("body");
        note.setCreatedBy(createdBy);
        note.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        note.setDocuments(documentList);
        Transaction transaction = new Transaction();

        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(noteRepository.save(any())).thenReturn(note);
        UUID noteTypeId = UUID.randomUUID();
        when(noteTypeRepository.findById(noteTypeId)).thenReturn(Optional.of(new NoteType()));
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        Note returnedNote = noteService.createTransactionNote(transactionId, note, noteTypeId);
        assertEquals(returnedNote.getCreatedBy(), createdBy);
    }

    @Test
    void testGetNotesByTransactionIdTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());
        Exception exception =
                assertThrows(
                        NotFoundException.class,
                        () -> noteService.getNotesByTransactionId(transactionId));

        assertEquals(TRANSACTION_NOT_FOUND, exception.getMessage());
    }

    @Test
    void testGetNoteByIdTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

        Exception exception =
                assertThrows(
                        NotFoundException.class,
                        () -> noteService.getByTransactionIdAndId(transactionId, noteId));

        assertEquals(TRANSACTION_NOT_FOUND, exception.getMessage());
    }

    @Test
    void testGetNoteByIdNoteNotFound() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        Transaction transaction = new Transaction();

        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(noteRepository.findByTransactionIdAndId(transactionId, noteId))
                .thenReturn(Optional.empty());

        Exception exception =
                assertThrows(
                        NotFoundException.class,
                        () -> noteService.getByTransactionIdAndId(transactionId, noteId));

        assertEquals("Note not found", exception.getMessage());
    }

    @Test
    void testCreateTransactionNoteTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        UUID noteTypeId = UUID.randomUUID();
        when(noteTypeRepository.findById(noteTypeId)).thenReturn(Optional.of(new NoteType()));
        when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());
        Exception exception =
                assertThrows(
                        NotFoundException.class,
                        () -> noteService.createTransactionNote(transactionId, note, noteTypeId));

        assertEquals(TRANSACTION_NOT_FOUND, exception.getMessage());
    }

    @Test
    void testCreateTransactionNoteWithNoDocuments() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(transactionId);
        note.setTitle("title");
        note.setBody("body");
        note.setDocuments(null);
        note.setCreatedBy(UUID.randomUUID().toString());
        note.setCreatedTimestamp(OffsetDateTime.now().minusDays(1));
        Transaction transaction = new Transaction();
        UUID noteTypeId = UUID.randomUUID();
        when(noteTypeRepository.findById(noteTypeId)).thenReturn(Optional.of(new NoteType()));
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(noteRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        Note returnedNote = noteService.createTransactionNote(transactionId, note, noteTypeId);

        assertEquals(returnedNote.getDocuments(), new ArrayList<>());
    }

    @Test
    void softDeleteTransactionNoteSuccess() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        Note note = new Note();
        note.setId(noteId);

        TransactionNote transactionNote = new TransactionNote(transactionId, note);
        transactionNote.setDeleted(false);
        Optional<TransactionNote> optionalTransactionNote = Optional.of(transactionNote);

        when(noteRepository.findByTransactionIdAndId(transactionId, noteId))
                .thenReturn(optionalTransactionNote);

        OffsetDateTime contextTimestamp = OffsetDateTime.now();
        when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(contextTimestamp);

        noteService.softDeleteTransactionNote(transactionId, noteId);

        verify(noteRepository).save(any());
    }

    @Test
    void softDeleteTransactionNoteNotFound() {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(noteRepository.findByTransactionIdAndId(transactionId, noteId))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> noteService.softDeleteTransactionNote(transactionId, noteId));
    }
}
