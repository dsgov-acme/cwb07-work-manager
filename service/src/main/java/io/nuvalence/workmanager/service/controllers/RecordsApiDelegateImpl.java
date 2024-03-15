package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.record.MissingRecordDefinitionException;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.controllers.RecordsApiDelegate;
import io.nuvalence.workmanager.service.generated.models.PagedRecordResponseModel;
import io.nuvalence.workmanager.service.generated.models.RecordCreationRequest;
import io.nuvalence.workmanager.service.generated.models.RecordResponseModel;
import io.nuvalence.workmanager.service.generated.models.RecordUpdateRequest;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.mapper.RecordMapper;
import io.nuvalence.workmanager.service.models.RecordFilters;
import io.nuvalence.workmanager.service.service.RecordDefinitionService;
import io.nuvalence.workmanager.service.service.RecordService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;

/**
 * Controller layer for Records.
 */
@Service
@AllArgsConstructor
@Slf4j
public class RecordsApiDelegateImpl implements RecordsApiDelegate {

    private final AuthorizationHandler authorizationHandler;
    private final RecordDefinitionService recordDefinitionService;
    private final RecordService recordService;
    private final TransactionService transactionService;
    private final RecordMapper mapper;
    private final PagingMetadataMapper pagingMetadataMapper;

    /**
     * Constructor.
     *
     * @param authorizationHandler authorization handler
     * @param recordDefinitionService record definition service
     * @param recordService record service
     * @param transactionService transaction service
     * @param mapper record mapper
     * @param entityMapper entity mapper
     * @param pagingMetadataMapper paging metadata mapper
     */
    @Autowired
    public RecordsApiDelegateImpl(
            AuthorizationHandler authorizationHandler,
            RecordDefinitionService recordDefinitionService,
            RecordService recordService,
            TransactionService transactionService,
            RecordMapper mapper,
            EntityMapper entityMapper,
            PagingMetadataMapper pagingMetadataMapper) {
        this.authorizationHandler = authorizationHandler;
        this.recordDefinitionService = recordDefinitionService;
        this.recordService = recordService;
        this.transactionService = transactionService;
        this.mapper = mapper;
        mapper.setEntityMapper(entityMapper);
        this.pagingMetadataMapper = pagingMetadataMapper;
    }

    @Override
    public ResponseEntity<RecordResponseModel> postRecord(
            RecordCreationRequest request, String authorization) {

        if (!authorizationHandler.isAllowed("create", Record.class)) {
            throw new ForbiddenException();
        }

        try {
            final RecordDefinition definition =
                    recordDefinitionService
                            .getRecordDefinitionByKey(request.getRecordDefinitionKey())
                            .orElseThrow(
                                    () ->
                                            new MissingRecordDefinitionException(
                                                    request.getRecordDefinitionKey()));
            final Transaction transaction =
                    transactionService
                            .getTransactionById(request.getTransactionId())
                            .filter(t -> authorizationHandler.isAllowedForInstance("view", t))
                            .orElseThrow(
                                    () ->
                                            new MissingTransactionException(
                                                    request.getTransactionId()));

            if (!authorizationHandler.isAllowedForInstance("update", transaction)) {
                throw new ForbiddenException("Action forbidden on the required transaction.");
            }

            final Record transactionRecord = recordService.createRecord(definition, transaction);

            return ResponseEntity.ok(mapper.recordToRecordResponseModel(transactionRecord));

        } catch (MissingSchemaException e) {
            log.error(
                    String.format(
                            "transaction definition [%s] references missing schema.",
                            request.getRecordDefinitionKey()),
                    e);
            return ResponseEntity.status(424).build();
        } catch (MissingRecordDefinitionException | MissingTransactionException e) {
            log.error(
                    String.format(
                            "ID [%s] references missing record definition.",
                            request.getRecordDefinitionKey()),
                    e);
            return ResponseEntity.status(424).build();
        }
    }

    @Override
    public ResponseEntity<RecordResponseModel> getRecord(UUID id) {
        if (!authorizationHandler.isAllowed("view", Record.class)) {
            throw new ForbiddenException();
        }

        return recordService
                .getRecordById(id)
                .map(
                        existingRecord ->
                                ResponseEntity.ok(
                                        mapper.recordToRecordResponseModel(existingRecord)))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<PagedRecordResponseModel> getRecords(
            String recordDefinitionKey,
            List<String> status,
            String externalId,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        if (!authorizationHandler.isAllowed("view", Record.class)) {
            throw new ForbiddenException();
        }

        RecordFilters filters =
                new RecordFilters(
                        recordDefinitionKey,
                        status,
                        externalId,
                        sortBy,
                        sortOrder,
                        pageNumber,
                        pageSize);

        Page<RecordResponseModel> results =
                recordService.getRecordsByFilters(filters).map(mapper::recordToRecordResponseModel);

        PagedRecordResponseModel response = new PagedRecordResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<RecordResponseModel> updateRecord(
            UUID id, RecordUpdateRequest recordUpdateRequest) {
        if (!authorizationHandler.isAllowed("update", Record.class)) {
            throw new ForbiddenException();
        }

        final String userType =
                CurrentUserUtility.getCurrentUser().map(UserToken::getUserType).orElse(null);

        return ResponseEntity.ok(
                mapper.recordToRecordResponseModel(
                        recordService.updateRecord(
                                recordUpdateRequest, id, isAdminForData(userType))));
    }

    private boolean isAdminForData(String userType) {
        return userType != null
                && (userType.equals(UserType.AGENCY.getValue())
                        || authorizationHandler.isAllowed("update-admin-data", Record.class));
    }
}
