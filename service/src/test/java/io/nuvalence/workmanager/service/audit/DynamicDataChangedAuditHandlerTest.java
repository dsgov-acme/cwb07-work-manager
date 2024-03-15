package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.generated.models.TransactionUpdateRequest;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.repository.SchemaRepository;
import io.nuvalence.workmanager.service.service.AuditEventService;
import io.nuvalence.workmanager.service.service.EmployerService;
import io.nuvalence.workmanager.service.service.IndividualService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.service.TransactionTaskService;
import io.nuvalence.workmanager.service.utils.JsonFileLoader;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DynamicDataChangedAuditHandlerTest {

    private final JsonFileLoader jsonLoader = new JsonFileLoader();
    private String nestedSchemaKey = "OfficeInfo";
    private final ObjectMapper objectMapper = SpringConfig.getMapper();
    private String userId = "2b394536-16a1-11ee-be56-0242ac120002";
    private OffsetDateTime eventTimestamp;
    private Transaction savedTransaction;
    private Map<String, Object> transactionData;

    @MockBean private AuthorizationHandler authorizationHandler;
    @MockBean private RequestContextTimestamp requestContextTimestamp;
    @MockBean private AuditEventService transactionAuditEventService;
    @MockBean private TransactionTaskService transactionTaskService;

    @MockBean private IndividualService individualService;
    @MockBean private EmployerService employerService;

    @Autowired private MockMvc mockMvc;
    @Autowired private DynamicSchemaMapper schemaMapper;
    @Autowired private SchemaService schemaService;
    @Autowired private TransactionDefinitionService transactionDefinitionService;
    @Autowired private TransactionService transactionService;
    @Autowired private EntityMapper entityMapper;
    @SpyBean private SchemaRepository schemaRepository;

    @BeforeEach
    void setup() throws IOException, MissingSchemaException, MissingTransactionException {

        SchemaRow schemaRow =
                SchemaRow.builder()
                        .id(UUID.fromString("ba8ef564-8947-11ee-b9d1-0242ac120002"))
                        .name("Parent Schema")
                        .key("ParentSchema")
                        .schemaJson("{\"key\": \"ParentSchema\"}")
                        .build();
        doReturn(List.of(schemaRow)).when(schemaRepository).getSchemaParents(anyString());

        // Set authenticated user
        Authentication authentication =
                UserToken.builder()
                        .applicationUserId(userId)
                        .providerUserId(userId)
                        .roles(List.of("wm:transaction-admin", "wm:transaction-config-admin"))
                        .build();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Ensure that all authorization checks pass.
        Mockito.when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        Mockito.when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);

        eventTimestamp = OffsetDateTime.now();
        Mockito.when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(eventTimestamp);

        Mockito.when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Collections.emptyList());

        Mockito.when(individualService.getIndividualById(any()))
                .thenReturn(Optional.of(Individual.builder().build()));
        Mockito.when(employerService.getEmployerById(any()))
                .thenReturn(Optional.of(Employer.builder().build()));

        setInitialDataState();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setInitialDataState()
            throws IOException, MissingSchemaException, MissingTransactionException {
        Schema schema = createSchema();
        TransactionDefinition transactionDefinition = createTransactionDefinition(schema);
        Transaction transaction = transactionService.createTransaction(transactionDefinition);

        transactionData = jsonLoader.loadConfigMap("/basicTransactionData.json");
        DynamicEntity dynaEntity = entityMapper.convertGenericMapToEntity(schema, transactionData);

        transaction.setData(dynaEntity);

        transaction.setSubjectProfileType(ProfileType.INDIVIDUAL);
        transaction.setSubjectProfileId(UUID.randomUUID());
        savedTransaction =
                transactionService.updateTransactionFromPartialUpdate(
                        transaction, schema.getAttributeConfigurations());
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_firstNameAndOfficeCityChanged_StringData() throws Exception {
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("firstName", "Thomas");
        Map<String, Object> officeInfo =
                (Map<String, Object>) transactionRequestData.get("officeInfo");
        officeInfo.put("city", "NY");
        transactionRequestData.put("officeInfo", officeInfo);

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setData(transactionRequestData);
        final String postBody =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                        .writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.firstName").value("Thomas"))
                .andExpect(jsonPath("$.data.officeInfo.city").value("NY"));

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> oldStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getOldState(),
                        new TypeReference<>() {});
        Map<String, Object> newStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getNewState(),
                        new TypeReference<>() {});

        Assertions.assertEquals("myFirstName", oldStateMap.get("firstName"));
        Assertions.assertEquals("Thomas", newStateMap.get("firstName"));
        Assertions.assertEquals("myCity", oldStateMap.get("officeInfo.city"));
        Assertions.assertEquals("NY", newStateMap.get("officeInfo.city"));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_documentChanged_DocumentData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        Document document =
                Document.builder()
                        .documentId(UUID.fromString("dff856ee-15dc-11ee-be56-0242ac120002"))
                        .build();
        transactionRequestData.put("document", document);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(
                        jsonPath("$.data.document.documentId")
                                .value("dff856ee-15dc-11ee-be56-0242ac120002"));

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> oldStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getOldState(),
                        new TypeReference<Map<String, Object>>() {});
        Map<String, Object> newStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getNewState(),
                        new TypeReference<Map<String, Object>>() {});

        Assertions.assertEquals(
                "f84b20e8-7a64-431f-ad94-440ca0c4b7c1", oldStateMap.get("document"));
        Assertions.assertEquals(
                "dff856ee-15dc-11ee-be56-0242ac120002", newStateMap.get("document"));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_ageChanged_numericData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("age", 50);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.age").value("50"));

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> oldStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getOldState(),
                        new TypeReference<Map<String, Object>>() {});
        Map<String, Object> newStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getNewState(),
                        new TypeReference<Map<String, Object>>() {});

        Assertions.assertEquals("30", oldStateMap.get("age"));
        Assertions.assertEquals("50", newStateMap.get("age"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DirtiesContext
    void testPublishAuditEvent_Exception(CapturedOutput output) throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("age", 50);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);
        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .sendAuditEvent(any(AuditEventRequestObjectDto.class));

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.age").value("50"));

        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " dynamic data change in transaction "
                                        + savedTransaction.getId()));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_dateOfBirthChanged_dateData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("dateOfBirth", "1963-12-21");

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1963-12-21"));

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> oldStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getOldState(),
                        new TypeReference<Map<String, Object>>() {});
        Map<String, Object> newStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getNewState(),
                        new TypeReference<Map<String, Object>>() {});

        Assertions.assertEquals("1993-12-21", oldStateMap.get("dateOfBirth"));
        Assertions.assertEquals("1963-12-21", newStateMap.get("dateOfBirth"));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_isMailingAddressNeededChanged_booleanData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("isMailingAddressNeeded", false);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("complete", "true")
                                .param("taskId", "taskId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.isMailingAddressNeeded").value("false"));

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(userId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(savedTransaction.getId(), capturedEvent.getBusinessObjectId());
        StateChangeEventData eventData = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals("{\"isMailingAddressNeeded\":\"true\"}", eventData.getOldState());
        Assertions.assertEquals("{\"isMailingAddressNeeded\":\"false\"}", eventData.getNewState());
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_dynamicDataDidNotChange() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();

        request.setData(transactionData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()));

        verify(transactionAuditEventService, never())
                .sendAuditEvent(any(AuditEventRequestObjectDto.class));
    }

    @Test
    @DirtiesContext
    void testRemoveComputedFields() throws Exception {
        Map<String, Object> transactionRequestData = new HashMap<>(transactionData);

        // modify dynamic data
        transactionRequestData.put("firstName", "Thomas");
        Map<String, Object> officeInfo =
                (Map<String, Object>) transactionRequestData.get("officeInfo");
        officeInfo.put("city", "NY");
        transactionRequestData.put("officeInfo", officeInfo);

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.firstName").value("Thomas"))
                .andExpect(jsonPath("$.data.officeInfo.city").value("NY"))
                .andExpect(jsonPath("$.data.officeInfo.fullAddress").value("NY myOfficeAddress"));

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(transactionAuditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(userId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(savedTransaction.getId(), capturedEvent.getBusinessObjectId());

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> oldStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getOldState(),
                        new TypeReference<>() {});
        Map<String, Object> newStateMap =
                objectMapper.readValue(
                        ((StateChangeEventData) capturedEvent.getData()).getNewState(),
                        new TypeReference<>() {});

        Assertions.assertFalse(oldStateMap.containsKey("officeInfo.fullAddress"));
        Assertions.assertFalse(newStateMap.containsKey("officeInfo.fullAddress"));
        Assertions.assertEquals("myFirstName", oldStateMap.get("firstName"));
        Assertions.assertEquals("Thomas", newStateMap.get("firstName"));
        Assertions.assertEquals("myCity", oldStateMap.get("officeInfo.city"));
        Assertions.assertEquals("NY", newStateMap.get("officeInfo.city"));
    }

    private TransactionDefinition createTransactionDefinition(Schema schema) {

        String stepKeyOne = "keyOne";
        Map<String, Object> componentOne = new HashMap<>();
        componentOne.put("key", stepKeyOne);
        String stepKeyTwo = "keyTwo";
        Map<String, Object> componentTwo = new HashMap<>();
        componentTwo.put("key", stepKeyTwo);
        List<Map<String, Object>> components =
                new ArrayList<>(Arrays.asList(componentOne, componentTwo));
        Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put("components", components);

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .key("defaultFormConfigurationKey")
                        .name("defaultFormConfigurationName")
                        .schemaKey(schema.getKey())
                        .configurationSchema("formio")
                        .configuration(configurationMap)
                        .build();

        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .key("BasicTransactionDefinition")
                        .name("Basic transaction definition")
                        .processDefinitionKey("test_process")
                        .schemaKey(schema.getKey())
                        .defaultStatus("Draft")
                        .category("application")
                        .subjectType(ProfileType.INDIVIDUAL)
                        .allowedRelatedPartyTypes(Set.of(ProfileType.INDIVIDUAL))
                        .build();
        transactionDefinition.addFormConfiguration(formConfiguration);

        return transactionDefinitionService.saveTransactionDefinition(transactionDefinition);
    }

    private Schema createSchema() throws IOException {
        List<DynaProperty> properties = new ArrayList<>();
        properties.add(new DynaProperty("city", String.class));
        properties.add(new DynaProperty("address", String.class));

        Schema nestedSchema =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .key(nestedSchemaKey)
                        .name(nestedSchemaKey)
                        .properties(properties)
                        .computedProperty(
                                "fullAddress", String.class, "#concat(\" \", city, address)")
                        .build();
        schemaService.saveSchema(nestedSchema);

        String schemaString = jsonLoader.loadConfigString("/basicSchema.json");

        SchemaJson schemaJson = objectMapper.readValue(schemaString, SchemaJson.class);

        Schema schema = schemaMapper.schemaJsonToSchema(schemaJson, UUID.randomUUID());
        return schemaService.saveSchema(schema);
    }
}
