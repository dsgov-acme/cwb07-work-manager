package io.nuvalence.workmanager.service.controllers;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.Note;
import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
import io.nuvalence.workmanager.service.generated.models.NoteCreationModelRequest;
import io.nuvalence.workmanager.service.generated.models.NoteModelResponse;
import io.nuvalence.workmanager.service.generated.models.NoteTypeModel;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"wm:transaction-admin", "wm:transaction-config-admin"})
@ExtendWith(OutputCaptureExtension.class)
class NoteApiDelegateImplTest {

    private static final String NOTE_TYPE_INVALID_MESSAGE =
            "{\"messages\":[\"Field type.name is invalid. Validation pattern that should be"
                    + " followed: not empty\"]}";

    @Autowired private MockMvc mockMvc;

    @MockBean private NoteService noteService;
    @MockBean private AuthorizationHandler authorizationHandler;

    @BeforeEach
    void setup() {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication())
                .thenReturn(
                        UserToken.builder()
                                .providerUserId("EXT000123")
                                .roles(
                                        List.of(
                                                "wm:transaction-admin",
                                                "wm:transaction-config-admin"))
                                .build());

        // Ensure that all authorization checks pass.
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
    }

    @Test
    void testGetTransactionNote() throws Exception {
        UUID id = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(id);
        when(noteService.getByTransactionIdAndId(id, noteId)).thenReturn(note);

        mockMvc.perform(get("/api/v1/transactions/" + id + "/notes/" + noteId))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTransactionNoteWithoutPermissions() throws Exception {
        UUID noteId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(id);
        when(noteService.getByTransactionIdAndId(id, noteId)).thenReturn(note);

        when(authorizationHandler.isAllowed("view", TransactionNote.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/transactions/" + id + "/notes/" + noteId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetTransactionNotes() throws Exception {
        UUID id = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(id);

        TransactionNote transactionNote = new TransactionNote(UUID.randomUUID(), note);
        final Page<TransactionNote> pagedResults = new PageImpl<>(List.of(transactionNote));
        when(noteService.getFilteredTransactionNotes(any())).thenReturn(pagedResults);

        NoteModelResponse noteModelResponse = new NoteModelResponse();
        noteModelResponse.setId(noteId);

        mockMvc.perform(get("/api/v1/transactions/" + id + "/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber", comparesEqualTo(0)))
                .andExpect(jsonPath("$.pagingMetadata.pageSize", comparesEqualTo(1)))
                .andExpect(jsonPath("$.pagingMetadata.totalCount", comparesEqualTo(1)))
                .andExpect(
                        jsonPath(
                                "$.items[0].id",
                                comparesEqualTo(noteModelResponse.getId().toString())));
    }

    @Test
    void testGetTransactionNotesWithoutPermissions() throws Exception {
        UUID id = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(id);
        when(noteService.getNotesByTransactionId(id)).thenReturn(List.of(note));

        when(authorizationHandler.isAllowed("view", TransactionNote.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/transactions/" + id + "/notes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPostTransactionNote() throws Exception {

        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setTitle("title");
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteModelRequest.setType(noteTypeModel);

        UUID id = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(id);
        String noteTypeName = "test-name";
        NoteType noteType = new NoteType(noteTypeId, noteTypeName);
        note.setType(noteType);

        when(noteService.createTransactionNote(eq(id), any(), eq(noteTypeId))).thenReturn(note);

        mockMvc.perform(
                        post("/api/v1/transactions/" + id + "/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isOk())
                .andReturn();

        verify(noteService, times(1))
                .postAuditEventForTransactionNote(id, note, AuditActivityType.NOTE_ADDED);
    }

    @Test
    void testPostTransactionNoteWithoutPermissions() throws Exception {

        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setTitle("title");
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteModelRequest.setType(noteTypeModel);

        UUID id = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(id);
        String noteTypeName = "test-name";
        NoteType noteType = new NoteType(noteTypeId, noteTypeName);
        note.setType(noteType);

        when(noteService.createTransactionNote(eq(id), any(), eq(noteTypeId))).thenReturn(note);
        when(authorizationHandler.isAllowed("create", TransactionNote.class)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/transactions/" + id + "/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPostTransactionNote_ExceptionPath(CapturedOutput output) throws Exception {

        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setTitle("title");
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteModelRequest.setType(noteTypeModel);

        UUID id = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(id);
        String noteTypeName = "test-name";
        NoteType noteType = new NoteType(noteTypeId, noteTypeName);
        note.setType(noteType);

        when(noteService.createTransactionNote(eq(id), any(), eq(noteTypeId))).thenReturn(note);

        doThrow(new RuntimeException("Test Exception"))
                .when(noteService)
                .postAuditEventForTransactionNote(any(), any(), any());

        mockMvc.perform(
                        post("/api/v1/transactions/" + id + "/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(
                output.getOut()
                        .contains(
                                "An error has occurred when recording an audit event for"
                                        + " note creation with id "
                                        + noteId
                                        + " for transaction with id "
                                        + id));
    }

    @Test
    void postTransactionNote_InvalidTypeName() throws Exception {
        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setTitle("title");
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteTypeModel.setName("");
        noteModelRequest.setType(noteTypeModel);

        UUID id = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/transactions/" + id + "/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(NOTE_TYPE_INVALID_MESSAGE)));
    }

    @Test
    void testUpdateTransactionNote_success() throws Exception {

        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setTitle("title");
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteModelRequest.setType(noteTypeModel);

        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        TransactionNote note = new TransactionNote();
        note.setId(noteId);
        note.setTransactionId(transactionId);
        String noteTypeName = "test-name";
        NoteType noteType = new NoteType(noteTypeId, noteTypeName);
        note.setType(noteType);

        when(noteService.getByTransactionIdAndId(transactionId, noteId)).thenReturn(note);
        when(noteService.updateTransactionNote(any(TransactionNote.class), any(), eq(noteTypeId)))
                .thenReturn(note);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId + "/notes/" + noteId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(note.getId().toString()));
    }

    @Test
    void testUpdateTransactionNote_forbidden() throws Exception {
        String noteTypeName = "test-name";

        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setTitle("title");
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteModelRequest.setType(noteTypeModel);

        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(authorizationHandler.isAllowed(eq("update"), (Class<TransactionNote>) any()))
                .thenReturn(false);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId + "/notes/" + noteId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateTransactionNote_NotFound() throws Exception {
        String noteTypeName = "test-name";

        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setTitle("title");
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteModelRequest.setType(noteTypeModel);

        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(noteService.getByTransactionIdAndId(transactionId, noteId))
                .thenThrow(new NotFoundException());

        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId + "/notes/" + noteId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateTransactionNote_InvalidDocumentIds() throws Exception {
        NoteCreationModelRequest noteModelRequest = new NoteCreationModelRequest();
        noteModelRequest.setBody("body");
        UUID noteTypeId = UUID.randomUUID();
        NoteTypeModel noteTypeModel = new NoteTypeModel();
        noteTypeModel.setId(noteTypeId);
        noteModelRequest.setType(noteTypeModel);
        noteModelRequest.setTitle("title");
        List<UUID> list = new ArrayList<>();
        list.add(null);
        noteModelRequest.setDocuments(list);
        UUID transactionId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/transactions/" + transactionId + "/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(noteModelRequest))
                                .characterEncoding("utf-8"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Provided document ids could not be mapped to"
                                                        + " UUIDs")));
    }

    @Test
    void softDeleteTransactionNote() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        TransactionNote note = mock(TransactionNote.class);
        when(noteService.softDeleteTransactionNote(transactionId, noteId)).thenReturn(note);

        mockMvc.perform(
                        delete(
                                        "/api/v1/transactions/"
                                                + transactionId
                                                + "/notes/"
                                                + noteId) // Use 'delete' method
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .characterEncoding("utf-8"))
                .andExpect(status().isOk());

        verify(noteService, times(1))
                .postAuditEventForTransactionNote(
                        transactionId, note, AuditActivityType.NOTE_DELETED);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void softDeleteTransactionNoteAuditEventError(CapturedOutput output) throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        // Mocking the necessary objects
        when(authorizationHandler.isAllowed("delete", TransactionNote.class)).thenReturn(true);
        when(noteService.softDeleteTransactionNote(transactionId, noteId))
                .thenReturn(mock(TransactionNote.class));
        doThrow(new RuntimeException("Simulated audit event error"))
                .when(noteService)
                .postAuditEventForTransactionNote(
                        eq(transactionId), any(Note.class), eq(AuditActivityType.NOTE_DELETED));

        mockMvc.perform(
                        delete("/api/v1/transactions/" + transactionId + "/notes/" + noteId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .characterEncoding("utf-8"))
                .andExpect(status().isOk());

        // Verify that the error message is logged
        assertTrue(
                output.getOut()
                        .contains(
                                String.format(
                                        "An error has occurred when recording an audit event for"
                                            + " note deletion with id %s for transaction with id"
                                            + " %s.",
                                        noteId, transactionId)));
    }

    @Test
    void softDeleteTransactionNote_forbidden() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(authorizationHandler.isAllowed("delete", TransactionNote.class)).thenReturn(false);

        mockMvc.perform(
                        delete(
                                        "/api/v1/transactions/"
                                                + transactionId
                                                + "/notes/"
                                                + noteId) // Use 'delete' method
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .characterEncoding("utf-8"))
                .andExpect(status().isForbidden());
    }
}
