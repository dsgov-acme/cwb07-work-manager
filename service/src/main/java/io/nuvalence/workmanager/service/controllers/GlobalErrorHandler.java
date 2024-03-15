package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.workmanager.service.config.exceptions.ApiException;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.ConflictException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.config.exceptions.RecordLinkerException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import io.nuvalence.workmanager.service.config.exceptions.model.RecordLinkerExceptionMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Global Error Handler Class.
 */
@ControllerAdvice
@Slf4j
public class GlobalErrorHandler {

    private String openApiValidationErrorPattern = "^([a-zA-Z]+\\.)+([a-zA-Z]+):\\s(.+)$";
    private String multipleStringValidationPattern = "^(.*)must match \"\\^\\((.*)\\)\\$\"$";

    /**
     * Handles all other exceptions.
     * 
     * @param e Exception to capture.
     * @return Internal server error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("An unexpected error occurred: ", e);

        return ResponseEntity.internalServerError()
                .body(
                        new ErrorResponse(
                                "Internal server error. Please contact the system administrator."));
    }

    /*
     * Error response object.
     * */
    /**
     * Error Response Class.
     */
    @AllArgsConstructor
    @Getter
    public class ErrorResponse {
        private List<String> messages;

        public ErrorResponse(String message) {
            this.messages = Collections.singletonList(message);
        }
    }

    /**
     * Handles business logic.
     *
     * @param e Business logic exception.
     * @return Forbidden request.
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleException(BusinessLogicException e) {
        log.warn("A business logic error has occurred: ", e);

        return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * Handles missing request header.
     *
     * @param e Missing request header exception.
     * @return Bad request with a readable error message.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> missingRequestHeader(MissingRequestHeaderException e) {
        log.warn(e.getMessage());

        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    /**
     * Handles record linker exceptions.
     *
     * @param e Record linker exception.
     * @return Bad request with a readable error message.
     */
    @ExceptionHandler(RecordLinkerException.class)
    public ResponseEntity<RecordLinkerExceptionMessage> handleRecordLinkerException(
            RecordLinkerException e) {
        log.warn("A record linker configuration error: ", e);

        return ResponseEntity.status(400).body(e.getErrorContents());
    }

    /**
     * Return a forbidden request if a ForbiddenException is thrown.
     *
     * @param e Forbidden exception.
     * @return Forbidden request.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleException(ForbiddenException e) {
        log.warn("User does not have permission: ", e);

        return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * Return a not found request if a NotFoundException is thrown.
     *
     * @param e NotFoundException exception.
     * @return NotFound request.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(NotFoundException e) {
        log.warn("Resource not found: {}", e);

        return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * Return a bad request if a ConstraintViolationException is thrown.
     *
     * @param e ConstraintViolationException exception.
     * @return Bad request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleException(ConstraintViolationException e) {
        log.warn("Bad request: ", e);

        String errorMessage =
                Arrays.stream(e.getMessage().split(", "))
                        .map(
                                message -> {
                                    if (message.matches(openApiValidationErrorPattern)) {
                                        return getOpenApiValidationMessage(message);
                                    } else {
                                        return message;
                                    }
                                })
                        .collect(Collectors.joining(", "));

        return ResponseEntity.status(400).body(new ErrorResponse(errorMessage));
    }

    /**
     * Return a bad request if a MethodArgumentNotValidException is thrown.
     *
     * @param e MethodArgumentNotValidException exception.
     * @return Bad request with a readable error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("Not valid argument ", e);

        String errorMessage = e.getMessage();

        String field = null;
        String validation = null;

        String fieldPrefix = "on field '";
        String validationPrefix = "must match \"";

        int fieldStart = errorMessage.indexOf(fieldPrefix);
        int validationStart = errorMessage.indexOf(validationPrefix);

        if (fieldStart != -1 && validationStart != -1) {
            fieldStart += fieldPrefix.length();
            int fieldEnd = errorMessage.indexOf("'", fieldStart);
            field = errorMessage.substring(fieldStart, fieldEnd);

            validationStart += validationPrefix.length();
            int validationEnd = errorMessage.indexOf("\"", validationStart);
            validation = errorMessage.substring(validationStart, validationEnd);

            if (validation.equals("^(?!\\\\s*$).+")) {
                validation = "not empty";
            } else if (validation.equals("^[a-zA-Z0-9]+$")) {
                validation = "not empty with no special characters";
            }

            errorMessage =
                    "Field "
                            + field
                            + " is invalid. Validation pattern that should be followed: "
                            + validation;
        } else {
            errorMessage = "Invalid argument format";
        }

        return ResponseEntity.status(400).body(new ErrorResponse(errorMessage));
    }

    /**
     * Gives a friendly format to open api validation messages.
     *
     * @param errorMessage message to format
     * @return formatted message
     */
    private String getOpenApiValidationMessage(String errorMessage) {
        String message = errorMessage.replaceAll(openApiValidationErrorPattern, "$2: $3");

        Pattern pattern = Pattern.compile(multipleStringValidationPattern);
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String[] values = matcher.group(2).split("\\|");

            message =
                    message.replaceAll(
                            "must match \"[^\"]*+\"",
                            "must be either " + String.join(" or ", values));
        }

        return message;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        log.warn("ApiException: ", e);
        return ResponseEntity.status(502).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ProvidedDataException.class)
    public ResponseEntity<ErrorResponse> handleProvidedDataException(ProvidedDataException e) {
        log.warn("ProvidedDataException: ", e);
        return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NuvalenceFormioValidationException.class)
    public ResponseEntity<NuvalenceFormioValidationExMessage>
            handleNuvalenceFormioValidationException(NuvalenceFormioValidationException e) {
        log.warn("NuvalenceFormioValidationException: ", e.getErrorMessages());
        return ResponseEntity.status(400).body(e.getErrorMessages());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException e) {
        log.warn("ConflictException: ", e);
        return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * Manages HttpMessageNotReadableException exceptions, to provided easily readable response messages.
     *
     * @param e exception to be managed.
     * @return Response to be sent to the user.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: ", e.getMessage());
        return ResponseEntity.status(400)
                .body(
                        new ErrorResponse(
                                "An error in the request deserialization occurred: "
                                        + e.getMessage()));
    }

    /**
     * Manages DataIntegrityViolationException exceptions, to provide easily readable response messages.
     *
     * @param e exception to be managed.
     * @return Response to be sent to the user.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException: ", e.getMessage());
        var response =
                ResponseEntity.status(500)
                        .body(new ErrorResponse("Data integrity violation. Not saved."));

        try {
            Throwable rootCause = e.getRootCause();
            if (rootCause != null
                    && rootCause.getMessage().contains("key value violates unique constraint")) {
                response =
                        ResponseEntity.status(409)
                                .body(
                                        new ErrorResponse(
                                                "Case-insensitive key already exists for this"
                                                        + " type."));
            }
        } catch (Exception ex) {
            return response;
        }
        return response;
    }
}
