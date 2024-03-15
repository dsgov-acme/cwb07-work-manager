package io.nuvalence.workmanager.service.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.generated.models.RecordCreationRequest;
import io.nuvalence.workmanager.service.generated.models.RecordUpdateRequest;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.mapper.RecordMapper;
import io.nuvalence.workmanager.service.models.RecordFilters;
import io.nuvalence.workmanager.service.service.RecordDefinitionService;
import io.nuvalence.workmanager.service.service.RecordService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import io.nuvalence.workmanager.service.utils.testutils.StringDateUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(
        authorities = {
            "wm:transaction-submitter",
            "wm:transaction-admin",
            "wm:transaction-config-admin"
        })
@ExtendWith(OutputCaptureExtension.class)
class RecordsApiDelegateImplTest {

    @MockBean private RecordDefinitionService recordDefinitionService;
    @MockBean private TransactionService transactionService;
    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private RecordService recordService;

    @Autowired private EntityMapper entityMapper;

    @Autowired private RecordMapper recordMapper;
    private Clock clock;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        recordMapper.setEntityMapper(entityMapper);
        when(securityContext.getAuthentication())
                .thenReturn(
                        UserToken.builder()
                                .providerUserId("EXT000123")
                                .applicationUserId("APP000123")
                                .roles(
                                        List.of(
                                                "wm:transaction-admin",
                                                "wm:transaction-config-admin"))
                                .build());

        // Ensure that all authorization checks pass.
        Mockito.when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        Mockito.when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
    }

    @Test
    void postRecord() throws Exception {
        User testUser = createUser();

        // Arrange
        final RecordDefinition recordDefinition = createRecordDefinition();

        Mockito.when(recordDefinitionService.getRecordDefinitionByKey("key"))
                .thenReturn(Optional.of(recordDefinition));

        final Transaction transaction = createTransaction(testUser);

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        Record record = createRecord(recordDefinition, transaction, testUser);

        Mockito.when(recordService.createRecord(recordDefinition, transaction)).thenReturn(record);

        final RecordCreationRequest request =
                new RecordCreationRequest()
                        .recordDefinitionKey("key")
                        .transactionId(UUID.fromString(transaction.getId().toString()));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/records")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId().toString()));
    }

    @Test
    void postRecordUnAuthorize() throws Exception {
        final RecordCreationRequest request =
                new RecordCreationRequest()
                        .recordDefinitionKey("key")
                        .transactionId(UUID.randomUUID());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(authorizationHandler.isAllowed("create", Record.class)).thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/records")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void postRecordMissingRecordDefinitionException() throws Exception {
        final RecordCreationRequest request =
                new RecordCreationRequest()
                        .recordDefinitionKey("key")
                        .transactionId(UUID.randomUUID());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(recordDefinitionService.getRecordDefinitionByKey("key")).thenReturn(Optional.empty());

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/records")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void postRecordMissingTransactionException() throws Exception {

        final RecordDefinition recordDefinition = createRecordDefinition();
        Mockito.when(recordDefinitionService.getRecordDefinitionByKey("key"))
                .thenReturn(Optional.of(recordDefinition));

        final RecordCreationRequest request =
                new RecordCreationRequest()
                        .recordDefinitionKey("key")
                        .transactionId(UUID.randomUUID());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionService.getTransactionById(UUID.randomUUID())).thenReturn(Optional.empty());

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/records")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void postRecordForbiddenTransactionUpdateException() throws Exception {

        // Arrange
        final RecordDefinition recordDefinition = createRecordDefinition();

        when(recordDefinitionService.getRecordDefinitionByKey("key"))
                .thenReturn(Optional.of(recordDefinition));

        final Transaction transaction = createTransaction(createUser());

        when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        final RecordCreationRequest request =
                new RecordCreationRequest()
                        .recordDefinitionKey("key")
                        .transactionId(UUID.fromString(transaction.getId().toString()));

        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(authorizationHandler.isAllowedForInstance(eq("view"), any(Transaction.class)))
                .thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(eq("update"), any(Transaction.class)))
                .thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/records")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                Matchers.containsString(
                                        "Action forbidden on the required transaction.")));
    }

    @Test
    void postRecordMissingSchema() throws Exception {
        User testUser = createUser();

        final RecordDefinition recordDefinition = createRecordDefinition();

        Mockito.when(recordDefinitionService.getRecordDefinitionByKey("key"))
                .thenReturn(Optional.of(recordDefinition));

        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("low")
                        .createdBy(testUser.getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(new DynamicEntity(Schema.builder().build()))
                        .build();

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        Mockito.when(recordService.createRecord(recordDefinition, transaction))
                .thenThrow(MissingSchemaException.class);
        final RecordCreationRequest request =
                new RecordCreationRequest()
                        .recordDefinitionKey("key")
                        .transactionId(UUID.fromString(transaction.getId().toString()));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/records")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void getRecord() throws Exception {
        RecordDefinition recordDefinition = createRecordDefinition();
        Transaction transaction = createTransaction(createUser());
        User testUser = createUser();

        Record record = createRecord(recordDefinition, transaction, testUser);

        when(recordService.getRecordById(any())).thenReturn(Optional.ofNullable(record));

        assert record != null;
        mockMvc.perform(get("/api/v1/records/" + record.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getRecordUnAuthorize() throws Exception {
        UUID id = UUID.randomUUID();
        when(authorizationHandler.isAllowed("view", Record.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/records/" + id)).andExpect(status().isForbidden());
    }

    @Test
    void getRecordNotFound() throws Exception {
        when(recordService.getRecordById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/records/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecordDefinitions() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        RecordDefinition recordDefinition = createRecordDefinition();
        Transaction transaction = createTransaction(createUser());
        User testUser = createUser();

        List<Record> records =
                List.of(
                        Record.builder()
                                .id(uuid1)
                                .recordDefinitionKey("key")
                                .externalId("externalId")
                                .recordDefinition(recordDefinition)
                                .status("status")
                                .expires(
                                        OffsetDateTime.now(clock)
                                                .plus(recordDefinition.getExpirationDuration()))
                                .createdBy(testUser.getId().toString())
                                .createdTimestamp(OffsetDateTime.now(clock))
                                .createdFrom(transaction)
                                .lastUpdatedFrom(transaction)
                                .lastUpdatedBy(testUser.getId().toString())
                                .lastUpdatedTimestamp(OffsetDateTime.now(clock))
                                .data(new DynamicEntity(Schema.builder().build()))
                                .build(),
                        Record.builder()
                                .id(uuid2)
                                .recordDefinitionKey("key2")
                                .externalId("externalId2")
                                .recordDefinition(recordDefinition)
                                .status("status2")
                                .expires(
                                        OffsetDateTime.now(clock)
                                                .plus(recordDefinition.getExpirationDuration()))
                                .createdBy(testUser.getId().toString())
                                .createdTimestamp(OffsetDateTime.now(clock))
                                .createdFrom(transaction)
                                .lastUpdatedFrom(transaction)
                                .lastUpdatedBy(testUser.getId().toString())
                                .lastUpdatedTimestamp(OffsetDateTime.now(clock))
                                .data(new DynamicEntity(Schema.builder().build()))
                                .build());

        when(recordService.getRecordsByFilters(any())).thenReturn(new PageImpl<>(records));

        // act and assert
        mockMvc.perform(get("/api/v1/records?sortOrder=ASC&pageNumber=0&pageSize=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(uuid1.toString()))
                .andExpect(jsonPath("$.items[0].recordDefinitionKey").value("key"))
                .andExpect(jsonPath("$.items[0].externalId").value("externalId"))
                .andExpect(jsonPath("$.items[0].status").value("status"))
                .andExpect(
                        jsonPath("$.items[0].expires")
                                .value(
                                        StringDateUtils.areStringDateAndOffsetDateTimeEqual(
                                                OffsetDateTime.now(clock)
                                                        .plus(
                                                                recordDefinition
                                                                        .getExpirationDuration()))))
                .andExpect(jsonPath("$.items[0].createdBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.items[0].createdTimestamp").exists())
                .andExpect(jsonPath("$.items[0].createdFrom").value(transaction.getId().toString()))
                .andExpect(
                        jsonPath("$.items[0].lastUpdatedFrom")
                                .value(transaction.getId().toString()))
                .andExpect(jsonPath("$.items[0].lastUpdatedBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.items[0].lastUpdatedTimestamp").exists())
                .andExpect(jsonPath("$.items[0].data").exists())
                .andExpect(jsonPath("$.items[1].id").value(uuid2.toString()))
                .andExpect(jsonPath("$.items[1].recordDefinitionKey").value("key2"))
                .andExpect(jsonPath("$.items[1].externalId").value("externalId2"))
                .andExpect(jsonPath("$.items[1].status").value("status2"))
                .andExpect(jsonPath("$.items[1].createdBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.items[1].createdTimestamp").exists())
                .andExpect(jsonPath("$.items[1].createdFrom").value(transaction.getId().toString()))
                .andExpect(
                        jsonPath("$.items[1].lastUpdatedFrom")
                                .value(transaction.getId().toString()))
                .andExpect(jsonPath("$.items[1].lastUpdatedBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.items[1].lastUpdatedTimestamp").exists())
                .andExpect(jsonPath("$.items[1].data").exists());

        verify(recordService, times(1)).getRecordsByFilters(any());
        verifyNoMoreInteractions(recordService);
    }

    @Test
    void getRecords_Unauthorized() throws Exception {

        when(authorizationHandler.isAllowed("view", Record.class)).thenReturn(false);

        // act and assert
        mockMvc.perform(get("/api/v1/records?name=a")).andExpect(status().isForbidden());

        verifyNoMoreInteractions(recordService);
    }

    @Test
    void getRecords_Defaults() throws Exception {

        when(recordService.getRecordsByFilters(any())).thenReturn(new PageImpl<>(List.of()));

        // act and assert
        mockMvc.perform(get("/api/v1/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void getRecordByExternalId() throws Exception {
        Record record =
                createRecord(
                        createRecordDefinition(), createTransaction(createUser()), createUser());

        final RecordFilters filters =
                new RecordFilters(null, null, "externalId", null, "ASC", 0, 2);

        final Page<Record> pagedResults = new PageImpl<>(List.of(record));

        Mockito.when(recordService.getRecordsByFilters(any())).thenReturn(pagedResults);

        // Act and Assert
        mockMvc.perform(get("/api/v1/records?" + "externalid=" + filters.getExternalId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$..externalId").value(filters.getExternalId()));
    }

    @Test
    void updateRecordSuccess() throws Exception {
        try (MockedStatic<CurrentUserUtility> mock = Mockito.mockStatic(CurrentUserUtility.class)) {
            mock.when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(
                            Optional.of(
                                    UserToken.builder()
                                            .userType("agency")
                                            .roles(Collections.emptyList())
                                            .build()));
            Record record =
                    createRecord(
                            createRecordDefinition(),
                            createTransaction(createUser()),
                            createUser());

            RecordUpdateRequest updateRequest = new RecordUpdateRequest();
            updateRequest.setStatus("NEW_STATUS");
            updateRequest.setExpires(OffsetDateTime.now(clock));
            Map<String, Object> dataRequested = Map.of("key", "value");
            updateRequest.setData(dataRequested);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            final String postBody = objectMapper.writeValueAsString(updateRequest);

            when(recordService.updateRecord(any(), any(), eq(true))).thenReturn(record);

            mockMvc.perform(
                            put("/api/v1/records/" + record.getId().toString())
                                    .content(postBody)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(record.getId().toString()));
        }
    }

    @Test
    void updateRecord_NotFound() throws Exception {
        try (MockedStatic<CurrentUserUtility> mock = Mockito.mockStatic(CurrentUserUtility.class)) {
            UUID recordId = UUID.randomUUID();

            mock.when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(
                            Optional.of(
                                    UserToken.builder()
                                            .userType("agency")
                                            .roles(Collections.emptyList())
                                            .build()));

            RecordUpdateRequest updateRequest = new RecordUpdateRequest();
            updateRequest.setStatus("NEW_STATUS");

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            final String postBody = objectMapper.writeValueAsString(updateRequest);

            doThrow(new NotFoundException("Record not found"))
                    .when(recordService)
                    .updateRecord(updateRequest, recordId, true);

            mockMvc.perform(
                            put("/api/v1/records/" + recordId)
                                    .content(postBody)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.messages[0]", Matchers.containsString("Record not found")));

            verify(recordService, times(1)).updateRecord(updateRequest, recordId, true);
        }
    }

    @Test
    void updateRecord_IsNotAdminNullProfileType() throws Exception {
        try (MockedStatic<CurrentUserUtility> mock = Mockito.mockStatic(CurrentUserUtility.class)) {
            UUID recordId = UUID.randomUUID();

            mock.when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(
                            Optional.of(
                                    UserToken.builder()
                                            .userType(null)
                                            .roles(Collections.emptyList())
                                            .build()));

            RecordUpdateRequest updateRequest = new RecordUpdateRequest();
            updateRequest.setStatus("NEW_STATUS");

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            final String postBody = objectMapper.writeValueAsString(updateRequest);

            doThrow(new ForbiddenException("User is not authorized to update admin data"))
                    .when(recordService)
                    .updateRecord(updateRequest, recordId, false);

            mockMvc.perform(
                            put("/api/v1/records/" + recordId)
                                    .content(postBody)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(
                            jsonPath(
                                    "$.messages[0]",
                                    Matchers.containsString(
                                            "User is not authorized to update admin data")));

            verify(recordService, times(1)).updateRecord(updateRequest, recordId, false);
        }
    }

    @Test
    void updateRecord_IsNotAdminWithUserType() throws Exception {
        try (MockedStatic<CurrentUserUtility> mock = Mockito.mockStatic(CurrentUserUtility.class)) {
            UUID recordId = UUID.randomUUID();

            mock.when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(
                            Optional.of(
                                    UserToken.builder()
                                            .userType("public")
                                            .roles(Collections.emptyList())
                                            .build()));

            when(authorizationHandler.isAllowed("update-admin-data", Record.class))
                    .thenReturn(false);

            RecordUpdateRequest updateRequest = new RecordUpdateRequest();
            updateRequest.setStatus("NEW_STATUS");

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            final String postBody = objectMapper.writeValueAsString(updateRequest);

            doThrow(new ForbiddenException("User is not authorized to update admin data"))
                    .when(recordService)
                    .updateRecord(updateRequest, recordId, false);

            mockMvc.perform(
                            put("/api/v1/records/" + recordId)
                                    .content(postBody)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(
                            jsonPath(
                                    "$.messages[0]",
                                    Matchers.containsString(
                                            "User is not authorized to update admin data")));

            verify(recordService, times(1)).updateRecord(updateRequest, recordId, false);
        }
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .externalId("EXT000123")
                .email("someEmail@something.com")
                .build();
    }

    private RecordDefinition createRecordDefinition() {
        return RecordDefinition.builder()
                .id(UUID.randomUUID())
                .name("Test Record Definition")
                .key("key")
                .schemaKey("schemaKey")
                .expirationDuration(Period.ofDays(20))
                .build();
    }

    private Record createRecord(
            RecordDefinition recordDefinition, Transaction transaction, User testUser) {
        return Record.builder()
                .id(UUID.randomUUID())
                .recordDefinitionKey("key")
                .externalId("externalId")
                .recordDefinition(recordDefinition)
                .status("status")
                .expires(OffsetDateTime.now().plus(recordDefinition.getExpirationDuration()))
                .createdBy(testUser.getId().toString())
                .createdTimestamp(OffsetDateTime.now())
                .createdFrom(transaction)
                .lastUpdatedFrom(transaction)
                .lastUpdatedBy(testUser.getId().toString())
                .lastUpdatedTimestamp(OffsetDateTime.now())
                .data(new DynamicEntity(Schema.builder().build()))
                .build();
    }

    private Transaction createTransaction(User testUser) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .transactionDefinitionId(UUID.randomUUID())
                .transactionDefinitionKey("Dummy user test")
                .processInstanceId("Dummy user test")
                .status("low")
                .createdBy(testUser.getId().toString())
                .createdTimestamp(OffsetDateTime.now())
                .lastUpdatedTimestamp(OffsetDateTime.now())
                .data(new DynamicEntity(Schema.builder().build()))
                .build();
    }
}
