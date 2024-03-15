package io.nuvalence.workmanager.service.controllers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.AllowedLink;
import io.nuvalence.workmanager.service.domain.transaction.DashboardColumnConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardTabConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DisplayFormat;
import io.nuvalence.workmanager.service.domain.transaction.FormConfigurationSelectionRule;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkType;
import io.nuvalence.workmanager.service.generated.models.AllowedLinkCreationRequest;
import io.nuvalence.workmanager.service.generated.models.DashboardColumnModel;
import io.nuvalence.workmanager.service.generated.models.DashboardTabModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationCreateModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationResponseModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationSelectionRuleModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationUpdateModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.RecordDefinitionUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardRequestModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkTypeModel;
import io.nuvalence.workmanager.service.mapper.AllowedLinkMapper;
import io.nuvalence.workmanager.service.mapper.AttributeConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.RecordDefinitionMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionSetMapper;
import io.nuvalence.workmanager.service.models.SchemaFilters;
import io.nuvalence.workmanager.service.repository.RecordDefinitionRepository;
import io.nuvalence.workmanager.service.service.AllowedLinkService;
import io.nuvalence.workmanager.service.service.DashboardConfigurationService;
import io.nuvalence.workmanager.service.service.FormConfigurationService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionSetOrderService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionSetService;
import io.nuvalence.workmanager.service.service.TransactionLinkTypeService;
import io.nuvalence.workmanager.service.service.WorkflowTasksService;
import io.nuvalence.workmanager.service.utils.ConfigurationUtility;
import io.nuvalence.workmanager.service.utils.camunda.ConsistencyChecker;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"wm:transaction-admin", "wm:transaction-config-admin"})
class AdminApiDelegateImplTest {

    private static final String KEY_VALIDATION_USER_MESSAGE =
            "{\"messages\":[\"Field key is invalid. Validation"
                    + " pattern that should be followed: not empty"
                    + " with no special characters\"]}";
    private static final String KEY_VALIDATION_USER_MESSAGE_CONSTRAINT =
            "{\"messages\":[\"key: must match \\\"^[a-zA-Z0-9]+$\\\"\"]}";
    private static final String TRANSACTION_DEFINITION_KEY_USER_MESSAGE_CONSTRAINT =
            "{\"messages\":[\"transactionDefinitionKey: must match \\\"^[a-zA-Z0-9]+$\\\"\"]}";
    private static final String NAME_VALIDATION_USER_MESSAGE =
            "{\"messages\":[\"Field name is invalid. Validation"
                    + " pattern that should be followed: not"
                    + " empty\"]}";
    private static final String NAME_VALIDATION_USER_MESSAGE_CONSTRAINT =
            "{\"messages\":[\"name: must match \\\"^(?!\\\\\\\\s*$).+\\\"\"]}";
    private static final String SCHEMA_KEY_INVALID_MESSAGE =
            "{\"messages\":[\"Field schemaKey is invalid. Validation pattern that should be"
                    + " followed: not empty with no special characters\"]}";
    private static final String TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE =
            "{\"messages\":[\"Field transactionDefinitionSetKey is invalid. Validation pattern that"
                    + " should be followed: not empty with no special characters\"]}";
    private static final String TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE_CONSTRAINT =
            "{\"messages\":[\"transactionSetKey: must match \\\"^[a-zA-Z0-9]+$\\\"\"]}";
    private static final String TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE =
            "{\"messages\":[\"Field transactionDefinitionKey is invalid. Validation pattern that"
                    + " should be followed: not empty with no special characters\"]}";
    private static final String TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE_CONSTRAINT =
            "{\"messages\":[\"transactionDefinitionKey: must match \\\"^[a-zA-Z0-9]+$\\\"\"]}";

    @Autowired private MockMvc mockMvc;

    @MockBean private SchemaService schemaService;

    @MockBean private TransactionDefinitionService transactionDefinitionService;

    @MockBean private FormConfigurationService formConfigurationService;

    @MockBean private ConsistencyChecker consistencyChecker;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private ConfigurationUtility configurationUtility;

    @MockBean private TransactionLinkTypeService transactionLinkTypeService;

    @MockBean private AllowedLinkService allowedLinkService;

    @MockBean private WorkflowTasksService workflowTasksService;

    @MockBean private DashboardConfigurationService dashboardConfigurationService;

    @MockBean private TransactionDefinitionSetOrderService transactionDefinitionSetOrderService;

    @MockBean private TransactionDefinitionSetService transactionDefinitionSetService;

    @MockBean private RecordDefinitionRepository recordDefRepository;

    // @MockBean private RecordDefinitionService recordDefinitionService;

    private DynamicSchemaMapper dynamicSchemaMapper;
    private RecordDefinitionMapper recordDefinitionMapper = RecordDefinitionMapper.INSTANCE;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        this.objectMapper = SpringConfig.getMapper();
        this.dynamicSchemaMapper = Mappers.getMapper(DynamicSchemaMapper.class);
        this.dynamicSchemaMapper.setObjectMapper(this.objectMapper);
        this.dynamicSchemaMapper.setAttributeConfigurationMapper(
                Mappers.getMapper(AttributeConfigurationMapper.class));

        // Ensure that all authorization checks pass.
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
    }

    @Test
    void globalErrorHandler() throws Exception {
        // Arrange
        when(authorizationHandler.isAllowed("view", Schema.class))
                .thenThrow(new RuntimeException("something failed"));

        // Act and Assert
        MvcResult mvcResult =
                mockMvc.perform(get("/api/v1/admin/schemas?name=test"))
                        .andExpect(status().isInternalServerError())
                        .andReturn();

        String actualResponseBody = mvcResult.getResponse().getContentAsString();

        Map<String, Object> responseMap =
                new ObjectMapper().readValue(actualResponseBody, Map.class);

        // make sure only plain response is provided
        assertEquals(1, responseMap.size());
        var messages = (List<String>) responseMap.get("messages");
        assertEquals(1, messages.size());
        assertEquals(
                "Internal server error. Please contact the system administrator.", messages.get(0));

        verifyNoInteractions(schemaService);
    }

    @Test
    void getSchema() throws Exception {
        // Arrange
        final Schema schema = createMockSchema();
        when(schemaService.getSchemaByKey("testschema")).thenReturn(Optional.of(schema));

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/schemas/testschema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testschema"));
    }

    @Test
    void getSchema404() throws Exception {
        // Arrange
        when(schemaService.getSchemaByKey("testschema")).thenReturn(Optional.empty());

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/schemas/testschema")).andExpect(status().isNotFound());
    }

    @Test
    void getSchemas() throws Exception {
        // Arrange
        final Schema schema1 =
                Schema.builder()
                        .key("testschemaKey")
                        .name("testschema")
                        .property("attribute", String.class)
                        .build();
        final Schema schema2 =
                Schema.builder()
                        .key("mytestKey")
                        .name("mytest")
                        .property("attribute", String.class)
                        .build();

        ArgumentCaptor<SchemaFilters> schemaFiltersCaptor =
                ArgumentCaptor.forClass(SchemaFilters.class);

        when(schemaService.getSchemasByFilters(any(SchemaFilters.class)))
                .thenReturn(new PageImpl<>(List.of(schema1, schema2)));

        // Act and Assert
        mockMvc.perform(
                        get(
                                "/api/v1/admin/schemas?name=test&pageSize=3&pageNumber=1&sortBy=key&sortOrder=DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("testschema"))
                .andExpect(jsonPath("$.items[1].name").value("mytest"));

        verify(schemaService).getSchemasByFilters(schemaFiltersCaptor.capture());

        var passedSchemaFilters = schemaFiltersCaptor.getValue();
        assertEquals("test", passedSchemaFilters.getName());
        assertEquals("key", passedSchemaFilters.getSortBy());
        assertEquals("DESC", passedSchemaFilters.getSortOrder());
        assertEquals(1, passedSchemaFilters.getPageNumber());
        assertEquals(3, passedSchemaFilters.getPageSize());
    }

    @Test
    void getSchemasDefaultFilters() throws Exception {

        ArgumentCaptor<SchemaFilters> schemaFiltersCaptor =
                ArgumentCaptor.forClass(SchemaFilters.class);

        when(schemaService.getSchemasByFilters(any(SchemaFilters.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/schemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        verify(schemaService).getSchemasByFilters(schemaFiltersCaptor.capture());

        var passedSchemaFilters = schemaFiltersCaptor.getValue();
        assertEquals(null, passedSchemaFilters.getName());
        assertEquals("name", passedSchemaFilters.getSortBy());
        assertEquals("ASC", passedSchemaFilters.getSortOrder());
        assertEquals(0, passedSchemaFilters.getPageNumber());
        assertEquals(50, passedSchemaFilters.getPageSize());
    }

    @Test
    void getSchemasForbidden() throws Exception {
        // Arrange
        when(authorizationHandler.isAllowed("view", Schema.class)).thenReturn(false);

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/schemas?name=test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.items").doesNotExist());

        verifyNoInteractions(schemaService);
    }

    @Test
    void getSchemaParents_Success() throws Exception {
        // Arrange
        final String schemaKey = "testSchemaKey";
        final Schema schema = Schema.builder().key(schemaKey).name("testSchema").build();
        final List<Schema> parents =
                List.of(
                        Schema.builder().key("parent1").name("Parent 1").build(),
                        Schema.builder().key("parent2").name("Parent 2").build());

        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));
        when(schemaService.getSchemaParents(schemaKey)).thenReturn(parents);

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/schemas/" + schemaKey + "/parents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentSchemas", hasSize(2)))
                .andExpect(jsonPath("$.parentSchemas[0]").value("parent1"))
                .andExpect(jsonPath("$.parentSchemas[1]").value("parent2"));

        verify(schemaService).getSchemaByKey(schemaKey);
        verify(schemaService).getSchemaParents(schemaKey);
    }

    @Test
    void getSchemaParents_SchemaNotFound() throws Exception {
        // Arrange
        final String schemaKey = "nonexistentSchemaKey";

        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.empty());

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/schemas/" + schemaKey + "/parents"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Schema not found: " + schemaKey));

        verify(schemaService).getSchemaByKey(schemaKey);
        verify(schemaService, never()).getSchemaParents(any());
    }

    @Test
    void getSchemaParents_AuthorizationFailed() throws Exception {
        // Arrange
        final String schemaKey = "testSchemaKey";
        final Schema schema = Schema.builder().key(schemaKey).name("testSchema").build();

        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));
        when(authorizationHandler.isAllowed("view", Schema.class)).thenReturn(false);

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/schemas/" + schemaKey + "/parents"))
                .andExpect(status().isForbidden());

        verify(authorizationHandler).isAllowed("view", Schema.class);
        verify(schemaService, never()).getSchemaByKey(schemaKey);
        verify(schemaService, never()).getSchemaParents(any());
    }

    @Test
    void updateSchemaNotFound() throws Exception {
        // Arrange
        final Schema schema = createMockSchema();

        final String body =
                new ObjectMapper()
                        .writeValueAsString(
                                this.dynamicSchemaMapper.schemaToSchemaUpdateModel(schema));

        when(schemaService.getSchemaByKey(schema.getKey())).thenReturn(Optional.empty());
        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/admin/schemas/testschemaKey")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(schemaService).getSchemaByKey(schema.getKey());
        verifyNoMoreInteractions(schemaService);
    }

    @Test
    void updateSchemaExisting() throws Exception {
        // Arrange
        final Schema schema = createMockSchema();

        when(schemaService.getSchemaByKey(schema.getKey())).thenReturn(Optional.of(schema));

        when(schemaService.saveSchema(any(Schema.class))).thenReturn(schema);

        final String body =
                new ObjectMapper()
                        .writeValueAsString(
                                this.dynamicSchemaMapper.schemaToSchemaUpdateModel(schema));

        var schemaCaptor = ArgumentCaptor.forClass(Schema.class);

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/admin/schemas/testschemaKey")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schema.getId().toString()));

        verify(schemaService).saveSchema(schemaCaptor.capture());
        // should pass schema with existing id
        assertEquals(schemaCaptor.getValue().getId(), schema.getId());
    }

    @Test
    void updateSchemaForbidden() throws Exception {

        // Arrange
        final Schema schema = createMockSchema();

        final String body =
                new ObjectMapper()
                        .writeValueAsString(
                                this.dynamicSchemaMapper.schemaToSchemaUpdateModel(schema));

        when(authorizationHandler.isAllowed("update", Schema.class)).thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/admin/schemas/testschemaKey")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.id").doesNotExist());

        verifyNoInteractions(schemaService);
    }

    @Test
    void updateSchema_InvalidSchemaName() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/schemas/testschemaKey",
                NAME_VALIDATION_USER_MESSAGE,
                customSchemaUpdateBodyToString(""));
    }

    @Test
    void createSchemaExisting() throws Exception {
        // Arrange
        final Schema schema = createMockSchema();

        final String body =
                new ObjectMapper()
                        .writeValueAsString(
                                this.dynamicSchemaMapper.schemaToSchemaCreateModel(schema));

        when(schemaService.getSchemaByKey(schema.getKey())).thenReturn(Optional.of(schema));

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/schemas")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        verify(schemaService).getSchemaByKey(schema.getKey());
        verifyNoMoreInteractions(schemaService);
    }

    @Test
    void createSchemaInvalidInvalidKeyEmpty() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/schemas",
                KEY_VALIDATION_USER_MESSAGE,
                customSchemaCreateBodyToString("Name", ""));
    }

    @Test
    void createSchemaInvalidInvalidKeySpecialCharacter() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/schemas",
                KEY_VALIDATION_USER_MESSAGE,
                customSchemaCreateBodyToString("Name", "Invalid Key"));
    }

    @Test
    void createSchema() throws Exception {
        // Arrange
        final Schema schema = createMockSchema();

        when(schemaService.getSchemaByKey(schema.getKey()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(schema));

        when(schemaService.saveSchema(any(Schema.class))).thenReturn(schema);

        final String body =
                new ObjectMapper()
                        .writeValueAsString(
                                this.dynamicSchemaMapper.schemaToSchemaCreateModel(schema));

        var schemaCaptor = ArgumentCaptor.forClass(Schema.class);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/schemas")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schema.getId().toString()))
                .andExpect(jsonPath("$.key").value(schema.getKey()));

        verify(schemaService).saveSchema(schemaCaptor.capture());
        // should pass schema with no id
        assertNull(schemaCaptor.getValue().getId());
    }

    @Test
    void createSchemaKeyUniquenessConstraint() throws Exception {
        // Arrange
        final Schema schema = createMockSchema();

        when(schemaService.getSchemaByKey(schema.getKey()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(schema));

        var exception = mock(DataIntegrityViolationException.class);
        when(exception.getRootCause())
                .thenReturn(new RuntimeException("key value violates unique constraint"));
        when(schemaService.saveSchema(any(Schema.class))).thenThrow(exception);

        final String body =
                new ObjectMapper()
                        .writeValueAsString(
                                this.dynamicSchemaMapper.schemaToSchemaCreateModel(schema));
        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/schemas")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value("Case-insensitive key already exists for this type."));
    }

    @Test
    void createSchemaForbidden() throws Exception {

        // Arrange
        final Schema schema = createMockSchema();

        final String postBody =
                new ObjectMapper()
                        .writeValueAsString(
                                this.dynamicSchemaMapper.schemaToSchemaCreateModel(schema));

        when(authorizationHandler.isAllowed("create", Schema.class)).thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/schemas")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.id").doesNotExist());

        verifyNoInteractions(schemaService);
    }

    @Test
    void deleteSchema() throws Exception {
        // Mock SchemaService to avoid actual deletion
        Mockito.doNothing().when(schemaService).deleteSchema(Mockito.any());
        when(schemaService.getSchemaByKey("testschemaKey"))
                .thenReturn(Optional.of(createMockSchema()));
        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/admin/schemas/testschemaKey")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteSchemaConflictException() throws Exception {

        when(schemaService.getSchemaByKey("testschemaKey"))
                .thenReturn(Optional.of(createMockSchema()));

        // Mock SchemaService to throw a RuntimeException
        doThrow(new RuntimeException("Schema is being used and cannot be deleted"))
                .when(schemaService)
                .deleteSchema(Mockito.any());

        mockMvc.perform(
                        delete("/api/v1/admin/schemas/testschemaKey")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteSchemaForbidden() throws Exception {
        when(authorizationHandler.isAllowed("delete", Schema.class)).thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/admin/schemas/testschemaKey")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(schemaService);
    }

    @Test
    void deleteSchema404() throws Exception {
        when(schemaService.getSchemaByKey("testschemaKey")).thenReturn(Optional.empty());
        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/admin/schemas/testschemaKey")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionDefinition() throws Exception {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test")
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("test-schema")
                        .defaultStatus("new")
                        .build();
        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.of(transactionDefinition));

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/transactions/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test transaction"));
    }

    @Test
    void getTransactionDefinition404() throws Exception {
        // Arrange
        final UUID id = UUID.randomUUID();
        when(transactionDefinitionService.getTransactionDefinitionById(id))
                .thenReturn(Optional.empty());

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/transactions/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void getTransactionDefinitions() throws Exception {
        // Arrange
        final TransactionDefinition transactionDefinition1 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test-1")
                        .name("test 1")
                        .category("cat")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("test-schema")
                        .defaultStatus("new")
                        .build();
        final TransactionDefinition transactionDefinition2 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test-2")
                        .name("test 2")
                        .category("cat")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("test-schema")
                        .defaultStatus("new")
                        .build();

        Page<TransactionDefinition> pagedResult =
                new PageImpl<>(List.of(transactionDefinition1, transactionDefinition2));

        when(transactionDefinitionService.getTransactionDefinitionsByFilters(any()))
                .thenReturn(pagedResult);

        // Act and Assert
        mockMvc.perform(get("/api/v1/admin/transactions?name=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("test 1"))
                .andExpect(jsonPath("$.items[1].name").value("test 2"));
    }

    @Test
    void postRecordDefinition() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(recordDefRepository.save(any(RecordDefinition.class)))
                .thenAnswer(
                        invocation -> {
                            RecordDefinition recordDefinition = invocation.getArgument(0);
                            recordDefinition.setId(uuid);
                            return recordDefinition;
                        });

        when(schemaService.getSchemaByKey(anyString())).thenReturn(Optional.of(createMockSchema()));

        // create model
        RecordDefinitionCreateModel request = new RecordDefinitionCreateModel();
        request.setKey("rdKey");
        request.setName("rdName");
        request.setDescription("rdDescription");
        request.setSchemaKey("rdSchemaKey");
        request.setExpirationDuration("p1m");

        // act and assert
        mockMvc.perform(
                        post("/api/v1/admin/records")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()))
                .andExpect(jsonPath("$.name").value("rdName"))
                .andExpect(jsonPath("$.key").value("rdKey"))
                .andExpect(jsonPath("$.description").value("rdDescription"))
                .andExpect(jsonPath("$.schemaKey").value("rdSchemaKey"))
                .andExpect(jsonPath("$.expirationDuration").value("P1M"));

        verify(recordDefRepository, times(1)).save(any(RecordDefinition.class));
        verify(schemaService, times(1)).getSchemaByKey(request.getSchemaKey());
    }

    @Test
    void postRecordDefinition_Unauthorized() throws Exception {

        when(authorizationHandler.isAllowed("create", RecordDefinition.class)).thenReturn(false);

        // create model
        RecordDefinitionCreateModel request = new RecordDefinitionCreateModel();
        request.setKey("rdKey");
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        // act and assert
        mockMvc.perform(
                        post("/api/v1/admin/records")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recordDefRepository);
    }

    @Test
    void postRecordDefinition_BadRequest() throws Exception {

        // create model
        RecordDefinitionCreateModel request = new RecordDefinitionCreateModel();
        // no key provided
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        // act and assert
        mockMvc.perform(
                        post("/api/v1/admin/records")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recordDefRepository);
        verifyNoInteractions(authorizationHandler);
    }

    @Test
    void postRecordDefinition_Existing() throws Exception {

        when(recordDefRepository.findByKey(anyString()))
                .thenAnswer(
                        invocation -> {
                            RecordDefinition recordDefinition =
                                    RecordDefinition.builder()
                                            .id(UUID.randomUUID())
                                            .key(invocation.getArgument(0))
                                            .name("OldName")
                                            .description("OldDescription")
                                            .schemaKey("OldSchemaKey")
                                            .expirationDuration(Period.parse("P53Y"))
                                            .build();

                            return Optional.of(recordDefinition);
                        });

        // create model
        RecordDefinitionCreateModel request = new RecordDefinitionCreateModel();
        request.setKey("rdKey");
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        // act and assert
        mockMvc.perform(
                        post("/api/v1/admin/records")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[0]").value("Record definition already exists"));

        verify(recordDefRepository, times(1)).findByKey(request.getKey());
    }

    @Test
    void postRecordDefinition_KeyUniquenessConstraint() throws Exception {

        var exception = mock(DataIntegrityViolationException.class);
        when(exception.getRootCause())
                .thenReturn(new RuntimeException("key value violates unique constraint"));

        when(recordDefRepository.save(any(RecordDefinition.class))).thenThrow(exception);

        when(schemaService.getSchemaByKey(anyString())).thenReturn(Optional.of(createMockSchema()));

        // create model
        RecordDefinitionCreateModel request = new RecordDefinitionCreateModel();
        request.setKey("rdKey");
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        // act and assert
        mockMvc.perform(
                        post("/api/v1/admin/records")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value("Case-insensitive key already exists for this type."));

        verify(recordDefRepository, times(1)).findByKey(request.getKey());
        verify(recordDefRepository, times(1)).save(any(RecordDefinition.class));
        verify(schemaService, times(1)).getSchemaByKey(request.getSchemaKey());
        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void postRecordDefinition_SchemaNotFound() throws Exception {

        when(schemaService.getSchemaByKey(anyString())).thenReturn(Optional.empty());

        // create model
        RecordDefinitionCreateModel request = new RecordDefinitionCreateModel();
        request.setKey("rdKey");
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        // act and assert
        mockMvc.perform(
                        post("/api/v1/admin/records")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[0]").value("Schema key not found"));

        verify(recordDefRepository, times(1)).findByKey(request.getKey());
        verify(schemaService, times(1)).getSchemaByKey(request.getSchemaKey());
        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void getRecordDefinitions() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        when(recordDefRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(
                        new PageImpl<RecordDefinition>(
                                List.of(
                                        RecordDefinition.builder()
                                                .id(uuid1)
                                                .name("rdName")
                                                .key("rdKey")
                                                .description("rdDescription")
                                                .schemaKey("rdSchemaKey")
                                                .expirationDuration(Period.parse("P1Y"))
                                                .build(),
                                        RecordDefinition.builder()
                                                .id(uuid2)
                                                .name("rdName2")
                                                .key("rdKey2")
                                                .description("rdDescription2")
                                                .schemaKey("rdSchemaKey2")
                                                .expirationDuration(Period.parse("P1D"))
                                                .build())));

        // act and assert
        mockMvc.perform(
                        get(
                                "/api/v1/admin/records?name=a&sortBy="
                                    + "createdTimestamp&sortOrder=DESC&pageNumber=1&pageSize=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(uuid1.toString()))
                .andExpect(jsonPath("$.items[0].name").value("rdName"))
                .andExpect(jsonPath("$.items[0].expirationDuration").value("P1Y"))
                .andExpect(jsonPath("$.items[1].id").value(uuid2.toString()))
                .andExpect(jsonPath("$.items[1].schemaKey").value("rdSchemaKey2"))
                .andExpect(jsonPath("$.items[1].expirationDuration").value("P1D"));

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);

        verify(recordDefRepository, times(1))
                .findAll(any(Specification.class), pageRequestCaptor.capture());
        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertEquals(
                "Page request [number: 1, size 2, sort: createdTimestamp: DESC]",
                capturedPageRequest.toString());
        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void getRecordDefinitions_Unauthorized() throws Exception {

        when(authorizationHandler.isAllowed("view", RecordDefinition.class)).thenReturn(false);

        // act and assert
        mockMvc.perform(get("/api/v1/admin/records?name=a")).andExpect(status().isForbidden());

        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void getRecordDefinitions_Defaults() throws Exception {

        when(recordDefRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<RecordDefinition>(List.of()));

        // act and assert
        mockMvc.perform(get("/api/v1/admin/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)));

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);

        verify(recordDefRepository, times(1))
                .findAll(any(Specification.class), pageRequestCaptor.capture());
        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertEquals(
                "Page request [number: 0, size 50, sort: name: ASC]",
                capturedPageRequest.toString());
        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void getRecordDefinitionByKey() throws Exception {
        UUID uuid = UUID.randomUUID();
        String key = "TestingKey";

        when(recordDefRepository.findByKey(anyString()))
                .thenAnswer(
                        invocation -> {
                            RecordDefinition recordDefinition =
                                    RecordDefinition.builder()
                                            .id(uuid)
                                            .key(invocation.getArgument(0))
                                            .name("rdName")
                                            .description("rdDescription")
                                            .schemaKey("rdSchemaKey")
                                            .expirationDuration(Period.parse("P1W"))
                                            .build();

                            return Optional.of(recordDefinition);
                        });

        // act and assert
        mockMvc.perform(get("/api/v1/admin/records/" + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()))
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.name").value("rdName"))
                .andExpect(jsonPath("$.description").value("rdDescription"))
                .andExpect(jsonPath("$.schemaKey").value("rdSchemaKey"))
                .andExpect(jsonPath("$.expirationDuration").value("P7D"));

        verify(recordDefRepository, times(1)).findByKey(key);
        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void getRecordDefinitionByKey_Unauthorized() throws Exception {

        when(authorizationHandler.isAllowed("view", RecordDefinition.class)).thenReturn(false);

        String key = "TestingKey";

        // act and assert
        mockMvc.perform(get("/api/v1/admin/records/" + key)).andExpect(status().isForbidden());

        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void getRecordDefinitionByKey_NotFound() throws Exception {
        String key = "TestingKey";

        when(recordDefRepository.findByKey(anyString())).thenReturn(Optional.empty());

        // act and assert
        mockMvc.perform(get("/api/v1/admin/records/" + key)).andExpect(status().isNotFound());

        verify(recordDefRepository, times(1)).findByKey(key);
        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void putRecordDefinition() throws Exception {
        UUID uuid = UUID.randomUUID();
        String key = "TestingKey";

        when(recordDefRepository.findByKey(anyString()))
                .thenAnswer(
                        invocation -> {
                            RecordDefinition recordDefinition =
                                    RecordDefinition.builder()
                                            .id(uuid)
                                            .key(invocation.getArgument(0))
                                            .name("OldName")
                                            .description("OldDescription")
                                            .schemaKey("OldSchemaKey")
                                            .expirationDuration(Period.parse("P53Y"))
                                            .build();

                            return Optional.of(recordDefinition);
                        });

        List<RecordDefinition> recordDefSaver = new ArrayList<>();

        when(recordDefRepository.save(any(RecordDefinition.class)))
                .thenAnswer(
                        invocation -> {
                            RecordDefinition recordDefinition = invocation.getArgument(0);
                            recordDefSaver.add(recordDefinition);

                            return recordDefinition;
                        });

        when(recordDefRepository.findById(any(UUID.class)))
                .thenAnswer(
                        invocation -> {
                            var recordDefinition =
                                    recordDefSaver.stream()
                                            .filter(
                                                    value ->
                                                            value.getId()
                                                                    .equals(
                                                                            invocation.getArgument(
                                                                                    0)))
                                            .findFirst()
                                            .get();

                            return Optional.of(recordDefinition);
                        });

        when(schemaService.getSchemaByKey(anyString())).thenReturn(Optional.of(createMockSchema()));

        // update model
        RecordDefinitionUpdateModel request = new RecordDefinitionUpdateModel();
        request.setName("rdName");
        request.setDescription("rdDescription");
        request.setSchemaKey("rdSchemaKey");
        request.setExpirationDuration("p1m");

        // act and assert
        mockMvc.perform(
                        put("/api/v1/admin/records/" + key)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()))
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.name").value("rdName"))
                .andExpect(jsonPath("$.description").value("rdDescription"))
                .andExpect(jsonPath("$.schemaKey").value("rdSchemaKey"))
                .andExpect(jsonPath("$.expirationDuration").value("P1M"));

        verify(recordDefRepository, times(1)).findByKey(key);
        verify(recordDefRepository, times(1)).save(any(RecordDefinition.class));
        verify(recordDefRepository, times(1)).findById(uuid);
        verify(schemaService, times(1)).getSchemaByKey(request.getSchemaKey());

        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void putRecordDefinition_Unauthorized() throws Exception {

        when(authorizationHandler.isAllowed("update", RecordDefinition.class)).thenReturn(false);

        // update model
        RecordDefinitionUpdateModel request = new RecordDefinitionUpdateModel();
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        // act and assert
        mockMvc.perform(
                        put("/api/v1/admin/records/AKey")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void putRecordDefinition_NotFound() throws Exception {

        when(recordDefRepository.findByKey(anyString())).thenReturn(Optional.empty());

        // update model
        RecordDefinitionUpdateModel request = new RecordDefinitionUpdateModel();
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        // act and assert
        mockMvc.perform(
                        put("/api/v1/admin/records/anotherKey")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[0]").value("Record definition not found"));

        verify(recordDefRepository, times(1)).findByKey("anotherKey");

        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void putRecordDefinition_SchemaNotFound() throws Exception {
        when(recordDefRepository.findByKey(anyString()))
                .thenReturn(Optional.of(mock(RecordDefinition.class)));

        when(schemaService.getSchemaByKey(anyString())).thenReturn(Optional.empty());

        // update model
        RecordDefinitionUpdateModel request = new RecordDefinitionUpdateModel();
        request.setName("rdName");
        request.setSchemaKey("rdSchemaKey");

        String key = "TestingKey";

        // act and assert
        mockMvc.perform(
                        put("/api/v1/admin/records/" + key)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[0]").value("Schema key not found"));

        verify(recordDefRepository, times(1)).findByKey(key);
        verify(schemaService, times(1)).getSchemaByKey(request.getSchemaKey());

        verifyNoMoreInteractions(recordDefRepository);
    }

    @Test
    void postTransactionDefinition() throws Exception {

        FormConfiguration formConfig = createFormConfigurations().get(2);

        // Arrange
        final TransactionDefinition transactionDefinition = createTransactionDefinition();
        final TransactionDefinition savedTransactionDefinition = createTransactionDefinition();
        savedTransactionDefinition.addFormConfiguration(formConfig);

        final TransactionDefinitionCreateModel createModel =
                getBaseTransactionDefinitionCreateModel();

        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.empty());

        when(transactionDefinitionService.saveTransactionDefinition(
                        any(TransactionDefinition.class)))
                .thenReturn(savedTransactionDefinition);

        final String postBody = new ObjectMapper().writeValueAsString(createModel);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/transactions")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test transaction"))
                .andExpect(jsonPath("$.defaultFormConfigurationKey").value(formConfig.getKey()));

        verify(transactionDefinitionService)
                .saveTransactionDefinition(any(TransactionDefinition.class));
    }

    @Test
    void postTransactionDefinitionKeyUniquenessConstraint() throws Exception {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test")
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey("schemaKey")
                        .description("test transaction")
                        .defaultStatus("new")
                        .build();

        final TransactionDefinitionCreateModel createModel =
                getBaseTransactionDefinitionCreateModel();

        FormConfiguration formConfig = createFormConfigurations().get(2);
        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.empty());

        var exception = mock(DataIntegrityViolationException.class);
        when(exception.getRootCause())
                .thenReturn(new RuntimeException("key value violates unique constraint"));

        when(transactionDefinitionService.saveTransactionDefinition(
                        any(TransactionDefinition.class)))
                .thenThrow(exception);

        final String postBody = new ObjectMapper().writeValueAsString(createModel);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/transactions")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value("Case-insensitive key already exists for this type."));
    }

    @Test
    void postTransactionDefinition409() throws Exception {
        // Arrange
        final TransactionDefinition transactionDefinition = createTransactionDefinition();
        final TransactionDefinitionCreateModel createModel =
                getBaseTransactionDefinitionCreateModel();

        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.of(transactionDefinition));

        final String postBody = new ObjectMapper().writeValueAsString(createModel);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/transactions")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void postTransactionDefinitionForbidden() throws Exception {
        final TransactionDefinitionCreateModel createModel =
                getBaseTransactionDefinitionCreateModel();

        when(authorizationHandler.isAllowed("create", TransactionDefinition.class))
                .thenReturn(false);

        final String postBody = new ObjectMapper().writeValueAsString(createModel);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/admin/transactions")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void postTransactionDefinition_InvalidKeyEmpty() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions",
                KEY_VALIDATION_USER_MESSAGE,
                customTransactionDefinitionCreateModel("", "schemaKey", "name", "setKey"));
    }

    @Test
    void postTransactionDefinition_InvalidKeySpecialCharacter() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions",
                KEY_VALIDATION_USER_MESSAGE,
                customTransactionDefinitionCreateModel(
                        "Invalid Key", "schemaKey", "name", "setKey"));
    }

    @Test
    void postTransactionDefinition_InvalidName() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions",
                NAME_VALIDATION_USER_MESSAGE,
                customTransactionDefinitionCreateModel("key", "schemaKey", "", "setKey"));
    }

    @Test
    void postTransactionDefinition_InvalidSchemaKeyEmpty() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions",
                SCHEMA_KEY_INVALID_MESSAGE,
                customTransactionDefinitionCreateModel("key", "", "name", "setKey"));
    }

    @Test
    void postTransactionDefinition_InvalidSchemaKeySpecialCharacter() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions",
                SCHEMA_KEY_INVALID_MESSAGE,
                customTransactionDefinitionCreateModel("key", "Invalid Key", "name", "setKey"));
    }

    @Test
    void postTransactionDefinition_InvalidTransactionDefinitionSetKeyEmpty() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions",
                TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE,
                customTransactionDefinitionCreateModel("key", "schemaKey", "name", ""));
    }

    @Test
    void postTransactionDefinition_InvalidTransactionDefinitionSetKeySpecialCharacter()
            throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions",
                TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE,
                customTransactionDefinitionCreateModel("key", "schemaKey", "name", "Invalid Key"));
    }

    @Test
    void putTransactionDefinition() throws Exception {
        // Arrange

        FormConfiguration formConfig = createFormConfigurations().get(0);

        FormConfigurationSelectionRule rule =
                FormConfigurationSelectionRule.builder()
                        .task("task")
                        .viewer("viewer")
                        .context("context")
                        .formConfigurationKey(formConfig.getKey())
                        .build();

        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test")
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey("schemaKey")
                        .defaultStatus("new")
                        .formConfigurationSelectionRules(List.of(rule))
                        .transactionDefinitionSetKey("transactionDefinitionSetKey")
                        .build();

        FormConfigurationSelectionRuleModel ruleModel = new FormConfigurationSelectionRuleModel();
        ruleModel.setTask("task");
        ruleModel.setViewer("viewer");
        ruleModel.setContext("context");
        ruleModel.setFormConfigurationKey(formConfig.getKey());

        final TransactionDefinitionUpdateModel update =
                new TransactionDefinitionUpdateModel()
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .defaultFormConfigurationKey(formConfig.getKey())
                        .formConfigurationSelectionRules(List.of(ruleModel))
                        .schemaKey("schemaKey")
                        .subjectType("INDIVIDUAL")
                        .allowedRelatedPartyTypes(List.of("INDIVIDUAL"))
                        .defaultStatus("new");

        ArgumentCaptor<TransactionDefinition> transactionDefinitionCaptor =
                ArgumentCaptor.forClass(TransactionDefinition.class);

        when(transactionDefinitionService.saveTransactionDefinition(
                        transactionDefinitionCaptor.capture()))
                .thenReturn(transactionDefinition);

        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.of(transactionDefinition));

        when(formConfigurationService.getFormConfigurationByKey(formConfig.getKey()))
                .thenReturn(Optional.of(formConfig));

        final String postBody = new ObjectMapper().writeValueAsString(update);

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/admin/transactions/test")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test transaction"));

        TransactionDefinition capturedTransactionDefinition =
                transactionDefinitionCaptor.getValue();

        assertEquals(transactionDefinition.getKey(), capturedTransactionDefinition.getKey());
        assertEquals(transactionDefinition.getName(), capturedTransactionDefinition.getName());
        assertEquals(
                transactionDefinition.getCategory(), capturedTransactionDefinition.getCategory());
        assertEquals(
                transactionDefinition.getProcessDefinitionKey(),
                capturedTransactionDefinition.getProcessDefinitionKey());
        assertEquals(
                transactionDefinition.getTransactionDefinitionSetKey(),
                capturedTransactionDefinition.getTransactionDefinitionSetKey());
    }

    @Test
    void putTransactionDefinition_existingWithFormConfig() throws Exception {
        // Arrange
        FormConfiguration formConfig = createFormConfigurations().get(0);

        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test")
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey("schemaKey")
                        .defaultStatus("new")
                        .subjectType(ProfileType.INDIVIDUAL)
                        .allowedRelatedPartyTypes(Set.of(ProfileType.INDIVIDUAL))
                        .build();
        transactionDefinition.addFormConfiguration(formConfig);

        final TransactionDefinitionUpdateModel update =
                new TransactionDefinitionUpdateModel()
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey("schemaKey")
                        .subjectType("INDIVIDUAL")
                        .allowedRelatedPartyTypes(List.of("INDIVIDUAL"))
                        .defaultStatus("new");

        when(transactionDefinitionService.saveTransactionDefinition(
                        any(TransactionDefinition.class)))
                .thenReturn(transactionDefinition);

        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.of(transactionDefinition));

        final String postBody = new ObjectMapper().writeValueAsString(update);

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/admin/transactions/test")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test transaction"));
    }

    @Test
    void putTransactionDefinition404() throws Exception {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test")
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey("schemaKey")
                        .defaultStatus("new")
                        .build();
        final TransactionDefinitionUpdateModel update =
                new TransactionDefinitionUpdateModel()
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey("schemaKey")
                        .defaultStatus("new");

        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.empty());

        final String postBody = new ObjectMapper().writeValueAsString(update);

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/admin/transactions/test")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void putTransactionDefinitionForbidden() throws Exception {
        // Arrange
        final TransactionDefinitionUpdateModel update =
                new TransactionDefinitionUpdateModel()
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey("testSchema")
                        .defaultStatus("new");
        final String postBody = new ObjectMapper().writeValueAsString(update);

        when(authorizationHandler.isAllowed("update", TransactionDefinition.class))
                .thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/admin/transactions/test")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.defaultFormConfigurationKey").doesNotExist());

        verify(transactionDefinitionService, never()).saveTransactionDefinition(any());
        verify(formConfigurationService, never()).getFormConfigurationByKeys(any(), any());
        verify(formConfigurationService, never()).saveFormConfiguration(any());
    }

    @Test
    void putTransactionDefinition_InvalidName() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/key",
                NAME_VALIDATION_USER_MESSAGE,
                customTransactionDefinitionUpdateModel("schemaKey", "", "setKey"));
    }

    @Test
    void putTransactionDefinition_InvalidSchemaKeyEmpty() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/invalid%20key",
                SCHEMA_KEY_INVALID_MESSAGE,
                customTransactionDefinitionUpdateModel("", "name", "setKey"));
    }

    @Test
    void putTransactionDefinition_InvalidSchemaKeySpecialCharacter() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/invalid%20key",
                SCHEMA_KEY_INVALID_MESSAGE,
                customTransactionDefinitionUpdateModel("Invalid Key", "name", "setKey"));
    }

    @Test
    void putTransactionDefinition_InvalidTransactionDefinitionSetKeyEmpty() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/test",
                TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE,
                customTransactionDefinitionUpdateModel("schemaKey", "name", ""));
    }

    @Test
    void putTransactionDefinition_InvalidTransactionDefinitionSetKeySpecialCharacter()
            throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/test",
                TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE,
                customTransactionDefinitionUpdateModel("schemaKey", "name", "Invalid Key"));
    }

    @Test
    void getConsistencyCheck() throws Exception {
        when(consistencyChecker.check()).thenReturn(List.of("ISSUE_1", "ISSUE_2"));

        mockMvc.perform(get("/api/v1/admin/consistency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value("ISSUE_1"))
                .andExpect(jsonPath("$[1]").value("ISSUE_2"));
    }

    @Test
    void getConsistencyCheckForbidden() throws Exception {
        when(authorizationHandler.isAllowed("view", "admin_dashboard")).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/consistency"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$[0]").doesNotExist());

        verify(consistencyChecker, never()).check();
    }

    @Test
    void getListOfFormConfigurationSuccess() throws Exception {
        TransactionDefinition transactionDefinition = createTransactionDefinition();
        List<FormConfiguration> formConfigurations = createFormConfigurations();

        FormConfigurationResponseModel formConfigurationResponseModel1 =
                FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(
                        formConfigurations.get(0));
        FormConfigurationResponseModel formConfigurationResponseModel2 =
                FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(
                        formConfigurations.get(1));

        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.of(transactionDefinition));

        when(formConfigurationService.getFormConfigurationsByTransactionDefinitionKey(
                        transactionDefinition.getKey()))
                .thenReturn(formConfigurations);

        FormConfigurationMapper formConfigurationMapper =
                Mockito.mock(FormConfigurationMapper.class);

        when(formConfigurationMapper.mapFormConfigurationToModel(formConfigurations.get(0)))
                .thenReturn(formConfigurationResponseModel1);
        when(formConfigurationMapper.mapFormConfigurationToModel(formConfigurations.get(1)))
                .thenReturn(formConfigurationResponseModel2);

        mockMvc.perform(get("/api/v1/admin/transactions/test/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(3))
                .andExpect(jsonPath("$[0].name").value(formConfigurationResponseModel1.getName()))
                .andExpect(jsonPath("$[1].name").value(formConfigurationResponseModel2.getName()));
    }

    @Test
    void getListOfFormConfigurationSuccessWithEmptyForms() throws Exception {

        TransactionDefinition transactionDefinition = createTransactionDefinition();

        when(transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinition.getKey()))
                .thenReturn(Optional.of(transactionDefinition));

        when(formConfigurationService.getFormConfigurationsByTransactionDefinitionKey(
                        transactionDefinition.getKey()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/admin/transactions/test/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    void exportConfiguration() throws Exception {

        File tempFile = new File("tmp");
        tempFile.createNewFile();
        when(configurationUtility.getConfiguration()).thenReturn(new FileSystemResource("tmp"));

        mockMvc.perform(get("/api/v1/admin/configuration/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"));

        assert (tempFile.delete());
        verify(configurationUtility, times(1)).getConfiguration();
    }

    @Test
    void exportConfigurationForbidden() throws Exception {

        when(authorizationHandler.isAllowed("export", "configuration")).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/configuration/export"))
                .andExpect(status().isForbidden());

        verify(configurationUtility, never()).getConfiguration();
    }

    @Test
    void exportConfiguration_FileReadException() throws Exception {
        when(configurationUtility.getConfiguration())
                .thenThrow(new IOException("Failed to get Configuration"));

        mockMvc.perform(get("/api/v1/admin/configuration/export"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Failed to get Configuration"));

        verify(configurationUtility, times(1)).getConfiguration();
    }

    @Test
    void getFormConfiguration() throws Exception {

        String formKey = "testForm1";
        FormConfiguration formConfig = createFormConfigurations().get(0);
        String transactionDefKey = formConfig.getTransactionDefinition().getKey();

        when(formConfigurationService.getFormConfigurationByKeys(transactionDefKey, formKey))
                .thenReturn(Optional.of(formConfig));

        mockMvc.perform(
                        get("/api/v1/admin/transactions/" + transactionDefKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is(formKey)))
                .andExpect(jsonPath("$.transactionDefinitionKey", is(transactionDefKey)))
                .andExpect(jsonPath("$.description", is(formConfig.getDescription())))
                .andExpect(jsonPath("$.createdBy", is(formConfig.getCreatedBy())))
                .andExpect(jsonPath("$.lastUpdatedBy", is(formConfig.getLastUpdatedBy())))
                .andExpect(jsonPath("$.createdTimestamp").exists())
                .andExpect(jsonPath("$.lastUpdatedTimestamp").exists());

        verify(formConfigurationService, times(1))
                .getFormConfigurationByKeys(transactionDefKey, formKey);
        verifyNoMoreInteractions(formConfigurationService);
    }

    @Test
    void getFormConfigurationFilteredToEmptyByPermissions() throws Exception {

        String transactionDefKey = "test";
        String formKey = "testForm1";

        FormConfiguration formConfig = createFormConfigurations().get(0);

        when(formConfigurationService.getFormConfigurationByKeys(transactionDefKey, formKey))
                .thenReturn(Optional.of(formConfig));

        when(authorizationHandler.isAllowedForInstance("view", formConfig)).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/admin/transactions/" + transactionDefKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.formConfigurationKey").doesNotExist());

        verify(formConfigurationService, times(1))
                .getFormConfigurationByKeys(transactionDefKey, formKey);
        verifyNoMoreInteractions(formConfigurationService);
    }

    @Test
    void putFormConfiguration() throws Exception {

        FormConfiguration formConfig = createFormConfigurations().get(0);

        FormConfigurationUpdateModel formConfigurationUpdateModel =
                new FormConfigurationUpdateModel();
        formConfigurationUpdateModel.setConfigurationSchema(formConfig.getConfigurationSchema());
        formConfigurationUpdateModel.setName(formConfig.getName());
        formConfigurationUpdateModel.setSchemaKey(formConfig.getSchemaKey());
        formConfigurationUpdateModel.setDescription(formConfig.getDescription());
        formConfigurationUpdateModel.setConfiguration(formConfig.getConfiguration());

        // method works with its own data
        when(formConfigurationService.saveFormConfiguration(any()))
                .then(AdditionalAnswers.returnsFirstArg());
        when(formConfigurationService.getFormConfigurationByKeys(any(), any()))
                .thenReturn(Optional.of(formConfig));
        when(transactionDefinitionService.getTransactionDefinitionByKey(any()))
                .thenReturn(Optional.of(formConfig.getTransactionDefinition()));

        String transactionDefKey = formConfig.getTransactionDefinition().getKey();
        String formKey = formConfig.getKey();
        mockMvc.perform(
                        put("/api/v1/admin/transactions/" + transactionDefKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                formConfigurationUpdateModel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is(formKey)))
                .andExpect(jsonPath("$.transactionDefinitionKey", is(transactionDefKey)))
                .andExpect(jsonPath("$.description", is(formConfig.getDescription())));

        verify(formConfigurationService, times(1)).getFormConfigurationByKeys(any(), any());
        verify(formConfigurationService, times(1)).saveFormConfiguration(any());
        verifyNoMoreInteractions(formConfigurationService);
    }

    @Test
    void putFormConfigurationForbidden() throws Exception {

        FormConfiguration formConfig = createFormConfigurations().get(0);

        FormConfigurationUpdateModel formConfigurationUpdateModel =
                new FormConfigurationUpdateModel();
        formConfigurationUpdateModel.setConfigurationSchema(formConfig.getConfigurationSchema());
        formConfigurationUpdateModel.setName(formConfig.getName());
        formConfigurationUpdateModel.setSchemaKey(formConfig.getSchemaKey());
        formConfigurationUpdateModel.setConfiguration(formConfig.getConfiguration());

        // method works with its own data
        when(formConfigurationService.saveFormConfiguration(any()))
                .then(AdditionalAnswers.returnsFirstArg());

        when(authorizationHandler.isAllowed("update", FormConfiguration.class)).thenReturn(false);

        String transactionDefKey = "defKeyToSet";
        String formKey = "formKeyToSet";
        mockMvc.perform(
                        put("/api/v1/admin/transactions/" + transactionDefKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                formConfigurationUpdateModel)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.formConfigurationKey").doesNotExist());

        verify(formConfigurationService, never()).saveFormConfiguration(any());
    }

    @Test
    void putFormConfiguration404() throws Exception {
        FormConfigurationUpdateModel formConfigurationUpdateModel =
                new FormConfigurationUpdateModel();
        String transactionDefKey = "nonExistentDefKey";
        String formKey = "nonExistentFormKey";
        when(formConfigurationService.getFormConfigurationByKeys(any(), any()))
                .thenReturn(Optional.empty());
        mockMvc.perform(
                        put("/api/v1/admin/transactions/" + transactionDefKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                formConfigurationUpdateModel)))
                .andExpect(status().isNotFound());

        verify(formConfigurationService, times(1)).getFormConfigurationByKeys(any(), any());
        verify(formConfigurationService, never()).saveFormConfiguration(any());
        verifyNoMoreInteractions(formConfigurationService);
    }

    @Test
    void putFormConfiguration_InvalidSchemaKeyEmpty() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/transactionDefKey/forms/formKey",
                SCHEMA_KEY_INVALID_MESSAGE,
                customFormConfigurationUpdateModel(""));
    }

    @Test
    void putFormConfiguration_InvalidSchemaKeySpecialCharacter() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/transactionDefKey/forms/formKey",
                SCHEMA_KEY_INVALID_MESSAGE,
                customFormConfigurationUpdateModel("Invalid Key"));
    }

    @Test
    void postFormConfigurationKeyUniquenessConstraint() throws Exception {

        FormConfiguration formConfig = createFormConfigurations().get(0);

        FormConfigurationCreateModel formConfigurationCreateModel =
                new FormConfigurationCreateModel();
        formConfigurationCreateModel.setConfigurationSchema(formConfig.getConfigurationSchema());
        formConfigurationCreateModel.setName(formConfig.getName());
        formConfigurationCreateModel.setSchemaKey(formConfig.getSchemaKey());
        formConfigurationCreateModel.setDescription(formConfig.getDescription());
        formConfigurationCreateModel.setConfiguration(formConfig.getConfiguration());
        formConfigurationCreateModel.setKey("formKeyToSet");

        var exception = mock(DataIntegrityViolationException.class);
        when(exception.getRootCause())
                .thenReturn(new RuntimeException("key value violates unique constraint"));

        // method works with its own data
        when(formConfigurationService.saveFormConfiguration(any())).thenThrow(exception);
        when(transactionDefinitionService.getTransactionDefinitionByKey(any()))
                .thenReturn(Optional.of(formConfig.getTransactionDefinition()));

        mockMvc.perform(
                        post("/api/v1/admin/transactions/defKeyToSet/forms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                formConfigurationCreateModel)))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value("Case-insensitive key already exists for this type."));
    }

    @Test
    void postFormConfiguration() throws Exception {

        FormConfiguration formConfig = createFormConfigurations().get(0);

        FormConfigurationCreateModel formConfigurationCreateModel =
                new FormConfigurationCreateModel();
        formConfigurationCreateModel.setConfigurationSchema(formConfig.getConfigurationSchema());
        formConfigurationCreateModel.setName(formConfig.getName());
        formConfigurationCreateModel.setSchemaKey(formConfig.getSchemaKey());
        formConfigurationCreateModel.setDescription(formConfig.getDescription());
        formConfigurationCreateModel.setConfiguration(formConfig.getConfiguration());
        formConfigurationCreateModel.setKey(formConfig.getKey());
        // method works with its own data
        when(formConfigurationService.saveFormConfiguration(any())).thenReturn(formConfig);
        when(formConfigurationService.getFormConfigurationByKey(any()))
                .thenReturn(Optional.empty());
        when(transactionDefinitionService.getTransactionDefinitionByKey(any()))
                .thenReturn(Optional.of(formConfig.getTransactionDefinition()));
        String transactionDefKey = formConfig.getTransactionDefinition().getKey();
        mockMvc.perform(
                        post("/api/v1/admin/transactions/" + transactionDefKey + "/forms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                formConfigurationCreateModel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is(formConfig.getKey())))
                .andExpect(jsonPath("$.transactionDefinitionKey", is(transactionDefKey)))
                .andExpect(jsonPath("$.description", is(formConfig.getDescription())));

        verify(formConfigurationService, times(1)).getFormConfigurationByKey(any());
        verify(formConfigurationService, times(1)).saveFormConfiguration(any());
        verifyNoMoreInteractions(formConfigurationService);
    }

    @Test
    void postFormConfigurationExistingForm() throws Exception {

        FormConfiguration formConfig = createFormConfigurations().get(0);

        FormConfigurationCreateModel formConfigurationCreateModel =
                new FormConfigurationCreateModel();
        formConfigurationCreateModel.setConfigurationSchema(formConfig.getConfigurationSchema());
        formConfigurationCreateModel.setName(formConfig.getName());
        formConfigurationCreateModel.setSchemaKey(formConfig.getSchemaKey());
        formConfigurationCreateModel.setDescription(formConfig.getDescription());
        formConfigurationCreateModel.setConfiguration(formConfig.getConfiguration());
        formConfigurationCreateModel.setKey("formKeyToSet");
        // method works with its own data
        when(formConfigurationService.saveFormConfiguration(any()))
                .then(AdditionalAnswers.returnsFirstArg());
        when(formConfigurationService.getFormConfigurationByKey(any()))
                .thenReturn(Optional.of(formConfig));
        String transactionDefKey = "defKeyToSet";
        mockMvc.perform(
                        post("/api/v1/admin/transactions/" + transactionDefKey + "/forms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                formConfigurationCreateModel)))
                .andExpect(status().isConflict());

        verify(formConfigurationService, times(1)).getFormConfigurationByKey(any());
        verify(formConfigurationService, never()).saveFormConfiguration(any());
        verifyNoMoreInteractions(formConfigurationService);
    }

    @Test
    void deleteFormConfiguration() throws Exception {
        Mockito.doNothing().when(formConfigurationService).deleteFormConfiguration(Mockito.any());
        String transactionDefKey = "defKey";
        String formKey = "formKey";
        when(formConfigurationService.getFormConfigurationByKeys(transactionDefKey, formKey))
                .thenReturn(Optional.of(createFormConfigurations().get(0)));
        // Act and Assert
        mockMvc.perform(
                        delete(
                                        "/api/v1/admin/transactions/"
                                                + transactionDefKey
                                                + "/forms/"
                                                + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteFormConfigurationConflictException() throws Exception {

        String transactionDefKey = "defKey";
        String formKey = "formKey";

        when(formConfigurationService.getFormConfigurationByKeys(transactionDefKey, formKey))
                .thenReturn(Optional.of(createFormConfigurations().get(0)));

        when(transactionDefinitionService.isActiveFormConfiguration(transactionDefKey, formKey))
                .thenReturn(true);
        mockMvc.perform(
                        delete(
                                        "/api/v1/admin/transactions/"
                                                + transactionDefKey
                                                + "/forms/"
                                                + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteFormConfigurationForbidden() throws Exception {
        when(authorizationHandler.isAllowed("delete", FormConfiguration.class)).thenReturn(false);

        String transactionDefKey = "defKey";
        String formKey = "formKey";

        Mockito.doThrow(
                        new RuntimeException(
                                "Form Configuration is being used and cannot be deleted"))
                .when(formConfigurationService)
                .deleteFormConfiguration(Mockito.any());

        mockMvc.perform(
                        delete(
                                        "/api/v1/admin/transactions/"
                                                + transactionDefKey
                                                + "/forms/"
                                                + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(schemaService);
    }

    @Test
    void deleteFormConfiguration404() throws Exception {
        Mockito.doNothing().when(formConfigurationService).deleteFormConfiguration(Mockito.any());
        String transactionDefKey = "defKey";
        String formKey = "formKey";
        when(formConfigurationService.getFormConfigurationByKeys(transactionDefKey, formKey))
                .thenReturn(Optional.empty());
        // Act and Assert
        mockMvc.perform(
                        delete(
                                        "/api/v1/admin/transactions/"
                                                + transactionDefKey
                                                + "/forms/"
                                                + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void postFormConfiguration_InvalidTransactionDefinitionKeySpecialCharacter() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/Invalid%20Key/forms",
                TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE_CONSTRAINT,
                customFormConfigurationCreateModel("schemaKey", "formKey"));
    }

    @Test
    void postFormConfiguration_InvalidSchemaKeyEmpty() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/transactionDefKey/forms",
                SCHEMA_KEY_INVALID_MESSAGE,
                customFormConfigurationCreateModel("", "formKey"));
    }

    @Test
    void postFormConfiguration_InvalidSchemaKeySpecialCharacter() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/transactionDefKey/forms",
                SCHEMA_KEY_INVALID_MESSAGE,
                customFormConfigurationCreateModel("Invalid Key", "formKey"));
    }

    @Test
    void postTransactionLinkType() throws Exception {

        String transactionLinkTypeName = "tester";
        TransactionLinkTypeModel requestModel = new TransactionLinkTypeModel();
        requestModel.setName(transactionLinkTypeName);

        // method works with its own data
        when(transactionLinkTypeService.saveTransactionLinkType(any()))
                .then(AdditionalAnswers.returnsFirstArg());

        mockMvc.perform(
                        post("/api/v1/admin/transactions/linktype")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestModel)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(transactionLinkTypeName)));

        verify(transactionLinkTypeService, times(1))
                .saveTransactionLinkType(any(TransactionLinkType.class));
    }

    @Test
    void postTransactionLinkTypeForbidden() throws Exception {

        TransactionLinkTypeModel requestModel = new TransactionLinkTypeModel();

        when(authorizationHandler.isAllowed("create", TransactionLinkType.class)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/admin/transactions/linktype")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestModel)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.name").doesNotExist());

        verify(transactionLinkTypeService, never()).saveTransactionLinkType(any());
    }

    @Test
    void postTransactionLinkType_InvalidName() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/linktype",
                NAME_VALIDATION_USER_MESSAGE,
                customTransactionLinkTypeModel(""));
    }

    @Test
    void getTransactionLinkTypes() throws Exception {

        TransactionLinkType transactionLinkType1 =
                TransactionLinkType.builder().name("test1").build();
        TransactionLinkType transactionLinkType2 =
                TransactionLinkType.builder().name("test2").build();

        when(transactionLinkTypeService.getTransactionLinkTypes())
                .thenReturn(List.of(transactionLinkType1, transactionLinkType2));

        mockMvc.perform(get("/api/v1/admin/transactions/linktypes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(2)))
                .andExpect(jsonPath("$[0].name", is("test1")))
                .andExpect(jsonPath("$[1].name", is("test2")));

        verify(transactionLinkTypeService, times(1)).getTransactionLinkTypes();
    }

    @Test
    void getTransactionLinkTypesFilteredByPermissions() throws Exception {

        TransactionLinkType transactionLinkType1 =
                TransactionLinkType.builder().name("test1").build();

        when(transactionLinkTypeService.getTransactionLinkTypes())
                .thenReturn(List.of(transactionLinkType1));

        when(authorizationHandler.getAuthFilter("view", TransactionLinkType.class))
                .thenReturn(element -> false);

        mockMvc.perform(get("/api/v1/admin/transactions/linktypes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(0)));

        verify(transactionLinkTypeService, times(1)).getTransactionLinkTypes();
    }

    @Test
    void postAllowedLinkToDefinition() throws Exception {

        UUID transactionLinkTypeId = UUID.randomUUID();
        String transactionDefinitionKey = "testerKey";
        AllowedLinkCreationRequest request = new AllowedLinkCreationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);
        request.setTransactionDefinitionKey(transactionDefinitionKey);

        AllowedLink allowedLink =
                AllowedLinkMapper.INSTANCE.allowedLinkRequestToAllowedLink(request);

        // method works with its own data
        when(allowedLinkService.saveAllowedLink(allowedLink, transactionLinkTypeId))
                .thenReturn(allowedLink);

        mockMvc.perform(
                        post("/api/v1/admin/transactions/allowedlink")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionDefinitionKey", is(transactionDefinitionKey)));

        verify(allowedLinkService, times(1)).saveAllowedLink(allowedLink, transactionLinkTypeId);
    }

    @Test
    void postAllowedLinkToDefinitionForbidden() throws Exception {

        AllowedLinkCreationRequest request = new AllowedLinkCreationRequest();

        when(authorizationHandler.isAllowed("create", AllowedLink.class)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/admin/transactions/allowedlink")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.transactionDefinitionKey").doesNotExist());

        verify(allowedLinkService, never()).saveAllowedLink(any(AllowedLink.class), any());
    }

    @Test
    void postAllowedLinkToDefinition_InvalidTransactionDefinitionKeyEmpty() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/allowedlink",
                TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE,
                customAllowedLinkCreationRequest(""));
    }

    @Test
    void postAllowedLinkToDefinition_InvalidTransactionDefinitionKeySpecialCharacter()
            throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transactions/allowedlink",
                TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE,
                customAllowedLinkCreationRequest("Invalid Key"));
    }

    @Test
    void getTransactionDefinitionAllowedLinksByKey() throws Exception {

        String transactionDefinitionKey = "testerKey";
        TransactionDefinition transactionDefinition = createTransactionDefinition();
        transactionDefinition.setKey(transactionDefinitionKey);

        TransactionLinkType transactionLinkType1 =
                TransactionLinkType.builder().name("test1").build();
        TransactionLinkType transactionLinkType2 =
                TransactionLinkType.builder().name("test2").build();

        AllowedLink allowedLink1 =
                AllowedLink.builder().transactionLinkType(transactionLinkType1).build();
        AllowedLink allowedLink2 =
                AllowedLink.builder().transactionLinkType(transactionLinkType2).build();

        when(allowedLinkService.getAllowedLinksByDefinitionKey(transactionDefinitionKey))
                .thenReturn(List.of(allowedLink1, allowedLink2));

        mockMvc.perform(get("/api/v1/admin/transactions/allowedlinks/" + transactionDefinitionKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(2)))
                .andExpect(jsonPath("$[0].transactionLinkType.name", is("test1")))
                .andExpect(jsonPath("$[1].transactionLinkType.name", is("test2")));

        verify(allowedLinkService, times(1))
                .getAllowedLinksByDefinitionKey(transactionDefinitionKey);
    }

    @Test
    void getTransactionDefinitionAllowedLinksByKeyFilteredToEmptyByPermissions() throws Exception {

        String transactionDefinitionKey = "testerKey";
        TransactionDefinition transactionDefinition = createTransactionDefinition();
        transactionDefinition.setKey(transactionDefinitionKey);

        TransactionLinkType transactionLinkType1 =
                TransactionLinkType.builder().name("test1").build();

        AllowedLink allowedLink1 =
                AllowedLink.builder().transactionLinkType(transactionLinkType1).build();

        when(allowedLinkService.getAllowedLinksByDefinitionKey(transactionDefinitionKey))
                .thenReturn(List.of(allowedLink1));

        when(authorizationHandler.getAuthFilter("view", AllowedLink.class))
                .thenReturn(element -> false);

        mockMvc.perform(get("/api/v1/admin/transactions/allowedlinks/" + transactionDefinitionKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(0)))
                .andExpect(jsonPath("$[0].transactionLinkType.name").doesNotExist());

        verify(allowedLinkService).getAllowedLinksByDefinitionKey(any());
    }

    @Test
    void listWorkflowsTest() throws Exception {
        ProcessDefinitionEntity processDefinitionOne = new ProcessDefinitionEntity();
        processDefinitionOne.setId("processDefinitionId1");
        processDefinitionOne.setKey("processDefinitionKey1");
        processDefinitionOne.setVersion(1);
        processDefinitionOne.setName("processDefinitionName1");

        ProcessDefinitionEntity processDefinitionTwo = new ProcessDefinitionEntity();
        processDefinitionTwo.setId("processDefinitionId1");
        processDefinitionTwo.setKey("processDefinitionKey1");
        processDefinitionTwo.setVersion(1);
        processDefinitionTwo.setName("processDefinitionName2");

        List<ProcessDefinition> processDefinitions =
                Arrays.asList(processDefinitionOne, processDefinitionTwo);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessDefinition> page =
                new PageImpl<>(processDefinitions, pageable, processDefinitions.size());

        when(workflowTasksService.getAllWorkflows(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/workflows").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("processDefinitionName1"))
                .andExpect(jsonPath("$.items[1].name").value("processDefinitionName2"));
    }

    @Test
    void listWorkflowsTest_forbidden() throws Exception {
        when(authorizationHandler.isAllowed("export", "configuration")).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/workflows").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getWorkflowByProcessDefinitionKeyTest() throws Exception {
        String processKey = "Key";

        ProcessDefinitionEntity processDefinitionOne = new ProcessDefinitionEntity();
        processDefinitionOne.setId("processDefinitionId1");
        processDefinitionOne.setKey(processKey);
        processDefinitionOne.setVersion(1);
        processDefinitionOne.setName("processDefinitionName1");

        when(workflowTasksService.getSingleWorkflow(processKey)).thenReturn(processDefinitionOne);

        mockMvc.perform(
                        get("/api/v1/admin/workflows/" + processKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processDefinitionKey").value(processKey));
    }

    @Test
    void getWorkflowByProcessDefinitionKeyTest_forbidden() throws Exception {
        when(authorizationHandler.isAllowed("export", "configuration")).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/workflows/key").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersTasksByProcessDefinitionKeyTest() throws Exception {
        String processKey = "Key";

        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
        UserTask taskModelOne = modelInstance.newInstance(UserTask.class);
        taskModelOne.setId("task1");
        taskModelOne.setName("User Task 1");
        when(workflowTasksService.getListOfTasksByProcessDefinitionKey(processKey))
                .thenReturn(List.of(taskModelOne));

        mockMvc.perform(
                        get("/api/v1/admin/workflows/" + processKey + "/tasks")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("User Task 1"));
    }

    @Test
    void getUsersTasksByProcessDefinitionKeyTest_forbidden() throws Exception {
        when(authorizationHandler.isAllowed("export", "configuration")).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/admin/workflows/key/tasks")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPutTransactionSet_query_not_found() throws Exception {
        String key = RandomStringUtils.randomAlphanumeric(10);
        mockMvc.perform(get("/api/v1/admin/transaction-sets/" + key))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDashboards() throws Exception {

        DashboardConfiguration dashboardConfiguration = createMockDashBoardConfiguration();
        UUID transactionDefinitionSetId = UUID.randomUUID();
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .id(transactionDefinitionSetId)
                        .key("testKey")
                        .dashboardConfiguration(dashboardConfiguration)
                        .build();
        dashboardConfiguration.setTransactionDefinitionSet(transactionDefinitionSet);
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionSetKey(transactionDefinitionSet.getKey())
                        .key("transactionDefinitionKey")
                        .build();

        when(dashboardConfigurationService.getAllDashboards())
                .thenReturn(List.of(dashboardConfiguration));
        when(transactionDefinitionService.getTransactionDefinitionsBySetKey(
                        transactionDefinitionSet.getKey()))
                .thenReturn(List.of(transactionDefinition));

        mockMvc.perform(get("/api/v1/admin/dashboards").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionSet", is("testKey")))
                .andExpect(
                        jsonPath(
                                "$[0].transactionDefinitionKeys[0]",
                                is("transactionDefinitionKey")));
    }

    @Test
    void testGetDashboards_forbidden() throws Exception {
        when(authorizationHandler.isAllowed("view", "dashboard_configuration")).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/dashboards").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetDashboardByTransactionSetKey() throws Exception {
        DashboardConfiguration dashboardConfiguration = createMockDashBoardConfiguration();
        UUID transactionDefinitionSetId = UUID.randomUUID();
        String transactionSetKey = "testKey";
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .id(transactionDefinitionSetId)
                        .key(transactionSetKey)
                        .dashboardConfiguration(dashboardConfiguration)
                        .build();
        dashboardConfiguration.setTransactionDefinitionSet(transactionDefinitionSet);
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionSetKey(transactionDefinitionSet.getKey())
                        .key("transactionDefinitionKey")
                        .build();

        when(dashboardConfigurationService.getDashboardByTransactionSetKey(transactionSetKey))
                .thenReturn(dashboardConfiguration);
        when(transactionDefinitionService.getTransactionDefinitionsBySetKey(
                        transactionDefinitionSet.getKey()))
                .thenReturn(List.of(transactionDefinition));

        mockMvc.perform(
                        get("/api/v1/admin/dashboards/" + transactionSetKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("transactionSet", is("testKey")))
                .andExpect(
                        jsonPath("transactionDefinitionKeys[0]", is("transactionDefinitionKey")));
    }

    @Test
    void testGetDashboardByTransactionSetKey_forbidden() throws Exception {
        when(authorizationHandler.isAllowed("view", "dashboard_configuration")).thenReturn(false);
        mockMvc.perform(
                        get("/api/v1/admin/dashboards/testKey")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboardOrderTest() throws Exception {
        when(transactionDefinitionSetOrderService.getTransactionDefinitionSetOrderAsString())
                .thenReturn(List.of("one", "two"));
        mockMvc.perform(
                        get("/api/v1/admin/dashboard-order")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is("one")))
                .andExpect(jsonPath("$[1]", is("two")));
    }

    @Test
    void updateDashboardOrderTest() throws Exception {
        List<String> order = List.of("one", "two");
        doNothing().when(transactionDefinitionSetOrderService).updateTransactionSetKeyOrder(any());

        mockMvc.perform(
                        put("/api/v1/admin/dashboard-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(order)))
                .andExpect(status().isAccepted());
    }

    @Test
    void getTransactionDefinitionSetCountsTest() throws Exception {

        Map<String, Long> counts = new HashMap<>();
        counts.put("one", 3L);
        counts.put("two", 1L);
        when(dashboardConfigurationService.countTabsForDashboard("transactionSetKey"))
                .thenReturn(counts);

        mockMvc.perform(
                        get("/api/v1/admin/dashboards/transactionSetKey/counts")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[?(@.tabLabel == 'one')].count").value(3))
                .andExpect(jsonPath("$[?(@.tabLabel == 'two')].count").value(1));
    }

    @Test
    void postTransactionSetTest_Successfully() throws Exception {
        String key = "key";
        String workflow = "workflow";
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder().key(key).workflow(workflow).build();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.empty());
        when(transactionDefinitionSetService.save(anyString(), any(TransactionDefinitionSet.class)))
                .thenReturn(transactionDefinitionSet);

        String requestBody = "{\"key\": \"" + key + "\", \"workflow\": \"" + workflow + "\"}";

        mockMvc.perform(
                        post("/api/v1/admin/transaction-sets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.workflow").value(workflow));

        ArgumentCaptor<TransactionDefinitionSet> transactionDefinitionSetCaptor =
                ArgumentCaptor.forClass(TransactionDefinitionSet.class);
        verify(transactionDefinitionSetService)
                .save(anyString(), transactionDefinitionSetCaptor.capture());
        TransactionDefinitionSet capturedSet = transactionDefinitionSetCaptor.getValue();
        assertEquals(key, capturedSet.getKey());
        assertEquals(workflow, capturedSet.getWorkflow());
    }

    @Test
    void postTransactionSetTestKeyUniquenessConstraint() throws Exception {

        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.empty());

        var exception = mock(DataIntegrityViolationException.class);
        when(exception.getRootCause())
                .thenReturn(new RuntimeException("key value violates unique constraint"));

        when(transactionDefinitionSetService.save(anyString(), any(TransactionDefinitionSet.class)))
                .thenThrow(exception);

        String requestBody = "{\"key\": \"key\", \"workflow\": \"workflow\"}";

        mockMvc.perform(
                        post("/api/v1/admin/transaction-sets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value("Case-insensitive key already exists for this type."));
    }

    @Test
    void postTransactionSet_Conflict() throws Exception {
        String key = "key";
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder().key(key).workflow("workflow").build();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.of(transactionDefinitionSet));
        String requestBody = "{\"key\": \"" + key + "\", \"workflow\": \"workflow\"}";

        mockMvc.perform(
                        post("/api/v1/admin/transaction-sets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void postTransactionSet_InvalidKeySpecialCharacter() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transaction-sets",
                KEY_VALIDATION_USER_MESSAGE,
                "{\"key\": \"" + "Invalid Key" + "\", \"workflow\": \"workflow\"}");
    }

    @Test
    void putTransactionSet_Successfully() throws Exception {
        String key = "key";
        String newWorkflow = "newWorkflow";

        TransactionDefinitionSetUpdateModel transactionDefinitionSetUpdateModel =
                new TransactionDefinitionSetUpdateModel();
        transactionDefinitionSetUpdateModel.setWorkflow(newWorkflow);

        TransactionDefinitionSet oldTransactionDefinitionSet =
                TransactionDefinitionSet.builder().key(key).workflow("oldWorkflow").build();

        when(transactionDefinitionSetService.getTransactionDefinitionSet(key))
                .thenReturn(Optional.of(oldTransactionDefinitionSet));

        TransactionDefinitionSet updatedTransactionDefinitionSet =
                TransactionDefinitionSetMapper.INSTANCE.updateModelToTransactionDefinitionSet(
                        transactionDefinitionSetUpdateModel);

        when(transactionDefinitionSetService.save(key, updatedTransactionDefinitionSet))
                .thenReturn(updatedTransactionDefinitionSet);

        String requestBody = "{\"workflow\": \"" + newWorkflow + "\"}";

        mockMvc.perform(
                        put("/api/v1/admin/transaction-sets/" + key)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.workflow").value(newWorkflow));

        verify(transactionDefinitionSetService).save(key, updatedTransactionDefinitionSet);
        assertEquals(newWorkflow, updatedTransactionDefinitionSet.getWorkflow());
    }

    @Test
    void putTransactionSet_NotFound() throws Exception {
        String key = "key";
        String newWorkflow = "newWorkflow";
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.empty());

        String requestBody = "{\"workflow\": \"" + newWorkflow + "\"}";
        mockMvc.perform(
                        put("/api/v1/admin/transaction-sets/" + key)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void putTransactionSet_InvalidKeySpecialCharacter() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/admin/transaction-sets/Invalid%20Key",
                KEY_VALIDATION_USER_MESSAGE_CONSTRAINT,
                "{\"workflow\": \"" + "newWorkflow" + "\"}");
    }

    @Test
    void getTransactionSetsTest() throws Exception {
        String key = "key";
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder().key(key).build();
        Page<TransactionDefinitionSet> transactionDefinitionSetPage =
                new PageImpl<>(List.of(transactionDefinitionSet));

        when(transactionDefinitionSetService.getAllTransactionDefinitionSets(any()))
                .thenReturn(transactionDefinitionSetPage);

        mockMvc.perform(
                        get("/api/v1/admin/transaction-sets")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTransactionDefinition_Success() throws Exception {
        // Arrange
        final String transactionDefinitionKey = "testKey";

        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/admin/transactions/{key}", transactionDefinitionKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(transactionDefinitionService).deleteTransactionDefinition(transactionDefinitionKey);
    }

    @Test
    void deleteTransactionDefinition_Forbidden() throws Exception {
        // Arrange
        final String transactionDefinitionKey = "testKey";

        when(authorizationHandler.isAllowed("delete", TransactionDefinition.class))
                .thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/admin/transactions/{key}", transactionDefinitionKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(transactionDefinitionService, never()).deleteTransactionDefinition(anyString());
    }

    @Test
    void deleteTransactionDefinition_InUse() throws Exception {
        // Arrange
        final String transactionDefinitionKey = "testKey";

        // Simulate the service throwing a RuntimeException, indicating the transaction is in use
        doThrow(new RuntimeException("Transaction in use"))
                .when(transactionDefinitionService)
                .deleteTransactionDefinition(transactionDefinitionKey);

        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/admin/transactions/{key}", transactionDefinitionKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        verify(transactionDefinitionService).deleteTransactionDefinition(transactionDefinitionKey);
    }

    @Test
    void deleteTransactionSet_Successful() throws Exception {
        String setKey = "setKey";
        when(transactionDefinitionSetService.getTransactionDefinitionSet(setKey))
                .thenReturn(Optional.of(TransactionDefinitionSet.builder().key(setKey).build()));

        doNothing().when(transactionDefinitionSetService).deleteTransactionDefinitionSet(any());

        mockMvc.perform(
                        delete("/api/v1/admin/transaction-sets/" + setKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTransactionSet_NotFound() throws Exception {
        String setKey = "setKey";
        when(transactionDefinitionSetService.getTransactionDefinitionSet(setKey))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        delete("/api/v1/admin/transaction-sets/" + setKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTransactionSet_Conflict() throws Exception {
        String setKey = "setKey";
        when(transactionDefinitionSetService.getTransactionDefinitionSet(setKey))
                .thenReturn(Optional.of(TransactionDefinitionSet.builder().key(setKey).build()));

        doThrow(
                        new RuntimeException(
                                "Transaction definition set is being used and cannot be deleted"))
                .when(transactionDefinitionSetService)
                .deleteTransactionDefinitionSet(any());

        mockMvc.perform(
                        delete("/api/v1/admin/transaction-sets/" + setKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void postRecordDefinitionFormConfiguration_Success() throws Exception {
        String formKey = "formKey";
        String recordDefinitionKey = "recordDefinitionKey";

        when(formConfigurationService.getFormConfigurationByKey(formKey))
                .thenReturn(Optional.empty());

        RecordDefinition recordDefinition =
                RecordDefinition.builder().key(recordDefinitionKey).build();

        when(recordDefRepository.findByKey(recordDefinitionKey))
                .thenReturn(Optional.of(recordDefinition));

        FormConfiguration formConfiguration = FormConfiguration.builder().key(formKey).build();

        when(formConfigurationService.saveFormConfiguration(any())).thenReturn(formConfiguration);

        FormConfigurationCreateModel formConfigurationCreateModel =
                new FormConfigurationCreateModel();
        formConfigurationCreateModel.setConfigurationSchema("formio");
        formConfigurationCreateModel.setName("name");
        formConfigurationCreateModel.schemaKey("schemaKey");
        formConfigurationCreateModel.setDescription("description");
        formConfigurationCreateModel.setKey(formKey);

        final String body = new ObjectMapper().writeValueAsString(formConfigurationCreateModel);

        mockMvc.perform(
                        post("/api/v1/admin/records/" + recordDefinitionKey + "/forms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(formKey));
    }

    @Test
    void postRecordDefinitionFormConfiguration_Conflict() throws Exception {
        String formKey = "formKey";
        String recordDefinitionKey = "recordDefinitionKey";

        when(formConfigurationService.getFormConfigurationByKey(formKey))
                .thenReturn(Optional.of(new FormConfiguration()));

        FormConfigurationCreateModel formConfigurationCreateModel =
                new FormConfigurationCreateModel();
        formConfigurationCreateModel.setKey(formKey);

        final String body = new ObjectMapper().writeValueAsString(formConfigurationCreateModel);

        mockMvc.perform(
                        post("/api/v1/admin/records/" + recordDefinitionKey + "/forms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void getListOfRecordDefinitionFormConfigurations_Success() throws Exception {
        String recordDefinitionKey = "recordDefinitionKey";

        FormConfiguration formConfigurationOne =
                FormConfiguration.builder()
                        .key("one")
                        .recordDefinitionKey(recordDefinitionKey)
                        .build();
        FormConfiguration formConfigurationTwo =
                FormConfiguration.builder()
                        .key("two")
                        .recordDefinitionKey(recordDefinitionKey)
                        .build();

        RecordDefinition recordDefinition =
                RecordDefinition.builder().key(recordDefinitionKey).build();
        when(recordDefRepository.findByKey(recordDefinitionKey))
                .thenReturn(Optional.of(recordDefinition));

        when(formConfigurationService.getFormConfigurationsByRecordDefinitionKey(
                        recordDefinitionKey))
                .thenReturn(List.of(formConfigurationOne, formConfigurationTwo));

        mockMvc.perform(
                        get("/api/v1/admin/records/" + recordDefinitionKey + "/forms")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].key").value("one"))
                .andExpect(jsonPath("$[1].key").value("two"));
    }

    @Test
    void putRecordDefinitionFormConfiguration_Success() throws Exception {
        String recordDefinitionKey = "recordDefinitionKey";
        String formKey = "formKey";

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .key(formKey)
                        .recordDefinitionKey(recordDefinitionKey)
                        .build();

        FormConfiguration formConfigurationUpdated =
                FormConfiguration.builder()
                        .key(formKey)
                        .recordDefinitionKey(recordDefinitionKey)
                        .description("description")
                        .build();

        when(formConfigurationService.getFormConfigurationByKeyAndRecordDefinitionKey(
                        recordDefinitionKey, formKey))
                .thenReturn(Optional.of(formConfiguration))
                .thenReturn(Optional.of(formConfigurationUpdated));

        RecordDefinition recordDefinition =
                RecordDefinition.builder().key(recordDefinitionKey).build();
        when(recordDefRepository.findByKey(recordDefinitionKey))
                .thenReturn(Optional.of(recordDefinition));

        when(formConfigurationService.saveFormConfiguration(any()))
                .thenReturn(formConfigurationUpdated);

        FormConfigurationUpdateModel formConfigurationUpdateModel =
                new FormConfigurationUpdateModel();
        formConfigurationUpdateModel.setDescription("description");

        final String body = new ObjectMapper().writeValueAsString(formConfigurationUpdateModel);

        mockMvc.perform(
                        put("/api/v1/admin/records/" + recordDefinitionKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("description"));
    }

    @Test
    void putRecordDefinitionFormConfiguration_NotFound() throws Exception {
        String recordDefinitionKey = "recordDefinitionKey";
        String formKey = "formKey";

        when(formConfigurationService.getFormConfigurationByKeyAndRecordDefinitionKey(
                        recordDefinitionKey, formKey))
                .thenReturn(Optional.empty());

        FormConfigurationUpdateModel formConfigurationUpdateModel =
                new FormConfigurationUpdateModel();

        final String body = new ObjectMapper().writeValueAsString(formConfigurationUpdateModel);

        mockMvc.perform(
                        put("/api/v1/admin/records/" + recordDefinitionKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecordDefinitionFormConfiguration_Success() throws Exception {
        String recordDefinitionKey = "recordDefinitionKey";
        String formKey = "formKey";

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .key(formKey)
                        .recordDefinitionKey(recordDefinitionKey)
                        .build();

        when(formConfigurationService.getFormConfigurationByKeyAndRecordDefinitionKey(
                        recordDefinitionKey, formKey))
                .thenReturn(Optional.of(formConfiguration));

        mockMvc.perform(
                        get("/api/v1/admin/records/" + recordDefinitionKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(formKey));
    }

    @Test
    void getRecordDefinitionFormConfiguration_NotFound() throws Exception {
        String recordDefinitionKey = "recordDefinitionKey";
        String formKey = "formKey";

        when(formConfigurationService.getFormConfigurationByKeyAndRecordDefinitionKey(
                        recordDefinitionKey, formKey))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/admin/records/" + recordDefinitionKey + "/forms/" + formKey)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private DashboardConfiguration createMockDashBoardConfiguration() {

        DashboardColumnConfiguration dashboardColumnConfiguration =
                DashboardColumnConfiguration.builder()
                        .id(UUID.randomUUID())
                        .columnLabel("label")
                        .attributePath("attributePath")
                        .sortable(true)
                        .displayFormat(DisplayFormat.DATE)
                        .build();

        DashboardTabConfiguration dashboardTabConfiguration =
                DashboardTabConfiguration.builder()
                        .id(UUID.randomUUID())
                        .tabLabel("label")
                        .filter(Map.of("key", "value"))
                        .build();

        return DashboardConfiguration.builder()
                .id(UUID.randomUUID())
                .dashboardLabel("label")
                .menuIcon("menuIcon")
                .columns(List.of(dashboardColumnConfiguration))
                .tabs(List.of(dashboardTabConfiguration))
                .build();
    }

    private Schema createMockSchema() {

        return Schema.builder()
                .id(UUID.randomUUID())
                .key("testschemaKey")
                .name("testschema")
                .description("test schema description")
                .property("attribute", String.class)
                .build();
    }

    private TransactionDefinition createTransactionDefinition() {

        return TransactionDefinition.builder()
                .id(UUID.randomUUID())
                .key("test")
                .name("test transaction")
                .category("test transaction")
                .processDefinitionKey("process-definition-key")
                .schemaKey("test-schema")
                .defaultStatus("new")
                .build();
    }

    private List<FormConfiguration> createFormConfigurations() {

        final TransactionDefinition transactionDefinition = createTransactionDefinition();

        final FormConfiguration formConfiguration1 =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinition(transactionDefinition)
                        .transactionDefinitionKey(transactionDefinition.getKey())
                        .key("testForm1")
                        .name("testForm1")
                        .schemaKey("schemaKey")
                        .description("test-description")
                        .createdBy("test-user")
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .lastUpdatedBy("test-user")
                        .configurationSchema("formio")
                        .build();

        final FormConfiguration formConfiguration2 =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinition(transactionDefinition)
                        .key("testForm2")
                        .name("testForm2")
                        .schemaKey("test-schema")
                        .configurationSchema("test-config")
                        .build();
        final FormConfiguration defaultFormConfiguration =
                FormConfiguration.builder()
                        .schemaKey("test-schema")
                        .configurationSchema("formio")
                        .name("Default Form")
                        .description("Default form for test transaction")
                        .configuration(new HashMap<>())
                        .key("testDefault")
                        .build();
        return List.of(formConfiguration1, formConfiguration2, defaultFormConfiguration);
    }

    private TransactionDefinitionSetDashboardRequestModel createDashboardConfiguration() {

        DashboardColumnModel dashboardColumnModel = new DashboardColumnModel();
        dashboardColumnModel.setColumnLabel(RandomStringUtils.random(10));
        dashboardColumnModel.setSortable(true);
        dashboardColumnModel.setAttributePath(RandomStringUtils.random(10));
        dashboardColumnModel.setDisplayFormat("PHONE");
        List<DashboardColumnModel> dashboardColumns = new ArrayList<>();
        dashboardColumns.add(dashboardColumnModel);

        DashboardTabModel dashboardTabModel = new DashboardTabModel();
        Map<String, Object> filter = new HashMap<>();
        filter.put("filter-test", "filter-value");
        dashboardTabModel.setTabLabel(RandomStringUtils.random(10));
        List<DashboardTabModel> dashboardTabs = new ArrayList<>();
        dashboardTabModel.filter(filter);
        dashboardTabs.add(dashboardTabModel);

        TransactionDefinitionSetDashboardRequestModel dashboard =
                new TransactionDefinitionSetDashboardRequestModel();
        dashboard.setDashboardLabel(RandomStringUtils.random(10));
        dashboard.setMenuIcon(RandomStringUtils.random(5));
        dashboard.setColumns(dashboardColumns);
        dashboard.setTabs(dashboardTabs);
        return dashboard;
    }

    private void getAndAssertBadRequestAndErrorString(String url, String errorString)
            throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(errorString)));
    }

    private void putAndAssertBadRequestAndErrorString(String url, String errorString, String body)
            throws Exception {
        mockMvc.perform(put(url).content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(errorString)));
    }

    private void postAndAssertBadRequestAndErrorString(String url, String errorString, String body)
            throws Exception {
        mockMvc.perform(post(url).content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(errorString)));
    }

    private void deleteAndAssertBadRequestAndErrorString(String url, String errorString)
            throws Exception {
        mockMvc.perform(delete(url).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(errorString)));
    }

    private String customAllowedLinkCreationRequest(String transactionDefinitionKey)
            throws JsonProcessingException {
        AllowedLinkCreationRequest request = new AllowedLinkCreationRequest();
        request.setTransactionDefinitionKey(transactionDefinitionKey);

        return bodyToString(request);
    }

    private String customTransactionLinkTypeModel(String name) throws JsonProcessingException {
        TransactionLinkTypeModel requestModel = new TransactionLinkTypeModel();
        requestModel.setName("");

        return bodyToString(requestModel);
    }

    private String customFormConfigurationCreateModel(String schemaKey, String formKey)
            throws JsonProcessingException {
        FormConfiguration formConfig = createFormConfigurations().get(0);
        FormConfigurationCreateModel formConfigurationCreateModel =
                new FormConfigurationCreateModel();
        formConfigurationCreateModel.setConfigurationSchema(formConfig.getConfigurationSchema());
        formConfigurationCreateModel.setName(formConfig.getName());
        formConfigurationCreateModel.setSchemaKey(schemaKey);
        formConfigurationCreateModel.setDescription(formConfig.getDescription());
        formConfigurationCreateModel.setConfiguration(formConfig.getConfiguration());
        formConfigurationCreateModel.setKey(formKey);

        return bodyToString(formConfigurationCreateModel);
    }

    private String customFormConfigurationUpdateModel(String schemaKey)
            throws JsonProcessingException {
        FormConfigurationUpdateModel formConfigurationUpdateModel =
                new FormConfigurationUpdateModel();
        formConfigurationUpdateModel.schemaKey(schemaKey);

        return bodyToString(formConfigurationUpdateModel);
    }

    private String customTransactionDefinitionUpdateModel(
            String schemaKey, String name, String transactionDefinitionSetKey)
            throws JsonProcessingException {

        final TransactionDefinitionUpdateModel updateModel =
                new TransactionDefinitionUpdateModel()
                        .name(name)
                        .category("category")
                        .processDefinitionKey("processDefinitionKey")
                        .schemaKey(schemaKey)
                        .defaultStatus("new")
                        .defaultFormConfigurationKey("defaultFormConfigurationKey")
                        .transactionDefinitionSetKey(transactionDefinitionSetKey);

        return bodyToString(updateModel);
    }

    private String customTransactionDefinitionCreateModel(
            String key, String schemaKey, String name, String transactionDefinitionSetKey)
            throws JsonProcessingException {

        final TransactionDefinitionCreateModel createModel =
                new TransactionDefinitionCreateModel()
                        .key(key)
                        .processDefinitionKey("processDefinitionKey")
                        .category("category")
                        .defaultStatus("Draft")
                        .name(name)
                        .schemaKey(schemaKey)
                        .subjectType("INDIVIDUAL")
                        .allowedRelatedPartyTypes(List.of("INDIVIDUAL"))
                        .transactionDefinitionSetKey(transactionDefinitionSetKey);

        return bodyToString(createModel);
    }

    private String customSchemaUpdateBodyToString(String name) throws JsonProcessingException {
        final Schema schema = Schema.builder().name(name).build();

        return bodyToString(this.dynamicSchemaMapper.schemaToSchemaUpdateModel(schema));
    }

    private String customSchemaCreateBodyToString(String name, String key)
            throws JsonProcessingException {
        final Schema schema = Schema.builder().name(name).key(key).build();

        return bodyToString(this.dynamicSchemaMapper.schemaToSchemaCreateModel(schema));
    }

    private String mockSchemaUpdateBodyToString() throws JsonProcessingException {
        final Schema schema = createMockSchema();

        return bodyToString(this.dynamicSchemaMapper.schemaToSchemaUpdateModel(schema));
    }

    private String bodyToString(Object body) throws JsonProcessingException {
        return objectMapper.writeValueAsString(body);
    }

    private TransactionDefinitionCreateModel getBaseTransactionDefinitionCreateModel() {
        return new TransactionDefinitionCreateModel()
                .name("test transaction")
                .description("test transaction")
                .category("test transaction")
                .processDefinitionKey("processDefinitionKey")
                .schemaKey("schemaKey")
                .defaultFormConfigurationKey("testDefault")
                .key("test")
                .defaultStatus("new")
                .subjectType("INDIVIDUAL")
                .allowedRelatedPartyTypes(List.of("INDIVIDUAL"));
    }
}
