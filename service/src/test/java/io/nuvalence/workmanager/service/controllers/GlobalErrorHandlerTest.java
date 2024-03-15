package io.nuvalence.workmanager.service.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class GlobalErrorHandlerTest {

    @InjectMocks private GlobalErrorHandler globalErrorHandler;

    @Test
    void testHandleConstraintViolationException_pattern() {
        ConstraintViolationException exception =
                new ConstraintViolationException(
                        "getTransactionsForAuthenticatedUser.sortBy: must match"
                                + " \"^(id|priority)$\"",
                        null);
        ResponseEntity<GlobalErrorHandler.ErrorResponse> response =
                globalErrorHandler.handleException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        assertEquals(
                "sortBy: must be either id or priority", response.getBody().getMessages().get(0));
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

        when(ex.getMessage())
                .thenReturn(
                        "Validation failed for argument [0] in public default"
                            + " org.springframework.http.ResponseEntity<SchemaModel>"
                            + " io.nuvalence.workmanager.servics.AdminApi.createSchema(SchemaCreateModel):"
                            + " [Field error in object 'schemaCreateModel' on field 'name':"
                            + " rejected value []; codes"
                            + " [Pattern.schemaCreateModel.name,Pattern.name,Pattern.java.lang.String,Pattern];"
                            + " arguments"
                            + " [org.springframework.context.support.DefaultMessageSourceResolvable:"
                            + " codes [schemaCreateModel.name,name]; arguments []; default message"
                            + " [name],[Ljakarta.validation.constraints.Pattern$Flag;@fd232ca,^(?!\\\\s*$).+];"
                            + " default message [must match \"^(?!\\\\s*$).+\"]]");

        ResponseEntity<GlobalErrorHandler.ErrorResponse> response =
                globalErrorHandler.handleMethodArgumentNotValidException(ex);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getMessages().size());
        assertEquals(
                "Field name is invalid. Validation pattern that should be followed: not empty",
                response.getBody().getMessages().get(0));
    }
}
