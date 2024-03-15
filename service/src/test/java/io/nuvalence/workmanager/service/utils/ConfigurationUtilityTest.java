package io.nuvalence.workmanager.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.DashboardColumnConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.DashboardTabConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.FormConfigurationSelectionRule;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetDataRequirement;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationExportModel;
import io.nuvalence.workmanager.service.generated.models.SchemaExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetExportModel;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionSetMapper;
import io.nuvalence.workmanager.service.repository.FormConfigurationRepository;
import io.nuvalence.workmanager.service.repository.SchemaRepository;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionRepository;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionSetRepository;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.repository.DeploymentBuilderImpl;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.community.mockito.QueryMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ExtendWith(MockitoExtension.class)
class ConfigurationUtilityTest {
    @Mock private TransactionDefinitionRepository transactionDefinitionRepository;

    @Mock private TransactionDefinitionSetRepository transactionDefinitionSetRepository;

    @Mock private FormConfigurationRepository formConfigurationRepository;

    @Mock private SchemaRepository schemaRepository;

    @Mock private ProcessEngine processEngine;

    @Mock private ProcessDefinition processDefinition;

    @Mock private DecisionDefinition decisionDefinition;

    @Mock private RepositoryService repositoryService;

    @Mock private DeploymentBuilderImpl deploymentBuilder;

    private TransactionDefinitionMapper transactionDefinitionMapper;

    private TransactionDefinitionSetMapper transactionDefinitionSetMapper;

    private FormConfigurationMapper formConfigurationMapper;

    private DynamicSchemaMapper dynamicSchemaMapper;

    private ObjectMapper objectMapper;

    private ConfigurationUtility configurationUtility;
    private List<TransactionDefinition> transactionDefinitions;
    private List<TransactionDefinitionSet> transactionDefinitionSets;
    private List<SchemaRow> schemas;
    private List<FormConfiguration> formConfigurations;

    @BeforeEach
    void setup() {
        this.objectMapper = SpringConfig.getMapper();
        this.dynamicSchemaMapper = Mappers.getMapper(DynamicSchemaMapper.class);
        this.dynamicSchemaMapper.setObjectMapper(this.objectMapper);
        this.transactionDefinitionMapper = Mappers.getMapper(TransactionDefinitionMapper.class);
        this.transactionDefinitionSetMapper =
                Mappers.getMapper(TransactionDefinitionSetMapper.class);
        this.formConfigurationMapper = Mappers.getMapper(FormConfigurationMapper.class);
        this.configurationUtility =
                new ConfigurationUtility(
                        transactionDefinitionRepository,
                        transactionDefinitionSetRepository,
                        schemaRepository,
                        formConfigurationRepository,
                        processEngine,
                        dynamicSchemaMapper,
                        transactionDefinitionMapper,
                        transactionDefinitionSetMapper,
                        formConfigurationMapper,
                        objectMapper);
        this.transactionDefinitions = getTransactionDefinitions();
        this.transactionDefinitionSets = getTransactionDefinitionSets();
        this.schemas = getSchemas();
        this.formConfigurations = getFormConfigurations();

        ReflectionTestUtils.setField(configurationUtility, "exportTempDir", "");
    }

    @Test
    void getConfiguration() throws Exception {
        setupStubs();
        String yamlContent;
        String jsonYaml;
        FileSystemResource resource = this.configurationUtility.getConfiguration();
        // validates the writeDirectoriesToZip metod
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isFile());
        assertEquals(
                ".zip",
                Objects.requireNonNull(resource.getFilename())
                        .substring(resource.getFilename().lastIndexOf(".")));
        File file = resource.getFile();
        InputStream is = new FileInputStream(file);
        ZipInputStream zipIn = new ZipInputStream(is);
        ZipEntry zipEntry = zipIn.getNextEntry();
        try (is;
                zipIn) {
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    String[] parts = zipEntry.getName().split("/", 2);
                    switch (parts[0]) {
                        case ConfigurationUtility.SCHEMAS_DIR:
                            yamlContent = IOUtils.toString(zipIn, StandardCharsets.UTF_8);
                            jsonYaml = ConfigurationUtility.convertYamlToJson(yamlContent);
                            SchemaExportModel schemaExportModel =
                                    ConfigurationUtility.yamlToTypedClass(
                                            jsonYaml, this.objectMapper, SchemaExportModel.class);
                            assertEquals(
                                    schemaExportModel,
                                    this.dynamicSchemaMapper.schemaRowToSchemaExportModel(
                                            this.schemas.get(0)));
                            break;
                        case ConfigurationUtility.TRANSACTIONS_DIR:
                            yamlContent = IOUtils.toString(zipIn, StandardCharsets.UTF_8);
                            jsonYaml = ConfigurationUtility.convertYamlToJson(yamlContent);
                            TransactionDefinitionExportModel transactionDefinitionExport =
                                    ConfigurationUtility.yamlToTypedClass(
                                            jsonYaml,
                                            this.objectMapper,
                                            TransactionDefinitionExportModel.class);
                            if (!parts[1].contains("form")) {
                                assertEquals(
                                        transactionDefinitionExport,
                                        this.transactionDefinitionMapper
                                                .transactionDefinitionToTransactionDefinitionExportModel(
                                                        this.transactionDefinitions.get(0)));
                            } else {
                                FormConfigurationExportModel formConfigurationExportModel =
                                        ConfigurationUtility.yamlToTypedClass(
                                                jsonYaml,
                                                this.objectMapper,
                                                FormConfigurationExportModel.class);
                                assertEquals(
                                        formConfigurationExportModel,
                                        this.formConfigurationMapper
                                                .formConfigurationToFormConfigurationExportModel(
                                                        this.formConfigurations.get(0)));
                            }
                            break;
                        case ConfigurationUtility.TRANSACTIONS_SET_DIR:
                            yamlContent = IOUtils.toString(zipIn, StandardCharsets.UTF_8);
                            jsonYaml = ConfigurationUtility.convertYamlToJson(yamlContent);
                            TransactionDefinitionSetExportModel
                                    transactionDefinitionSetExportModel =
                                            ConfigurationUtility.yamlToTypedClass(
                                                    jsonYaml,
                                                    this.objectMapper,
                                                    TransactionDefinitionSetExportModel.class);
                            assertEquals(
                                    transactionDefinitionSetExportModel,
                                    this.transactionDefinitionSetMapper
                                            .transactionDefinitionSetToExportModel(
                                                    this.transactionDefinitionSets.get(0)));
                            break;
                        case ConfigurationUtility.WORKFLOWS_DIR:
                            if (parts[1].contains("bpmn")) {
                                BpmnModelInstance bpmnModelInstance =
                                        Bpmn.readModelFromStream(
                                                ConfigurationUtility.readZipEntryToInputStream(
                                                        zipIn));
                                assertEquals(
                                        "PROCESS_DEFINITION_DIAGRAM_1",
                                        bpmnModelInstance.getDefinitions().getId());
                            } else {
                                DmnModelInstance dmnModelInstance =
                                        Dmn.readModelFromStream(
                                                ConfigurationUtility.readZipEntryToInputStream(
                                                        zipIn));
                                assertEquals(
                                        "DECISION_DEFINITION_DIAGRAM_1",
                                        dmnModelInstance.getDefinitions().getId());
                            }
                            break;
                        default:
                            break;
                    }
                }
                zipEntry = zipIn.getNextEntry();
            }
        }
        File tempFile = resource.getFile();
        assertTrue(tempFile.delete());
        deleteDirectory(resource.getFile().getParentFile());
    }

    private void setupStubs() {
        lenient()
                .when(transactionDefinitionRepository.findAll())
                .thenReturn(this.transactionDefinitions);
        lenient()
                .when(transactionDefinitionSetRepository.findAll())
                .thenReturn(this.transactionDefinitionSets);
        lenient()
                .when(
                        formConfigurationRepository.findByTransactionDefinitionKey(
                                "TRANSACTION_DEFINITION_KEY"))
                .thenReturn(this.formConfigurations);

        lenient().when(schemaRepository.findAll()).thenReturn(this.schemas);

        lenient().when(processEngine.getRepositoryService()).thenReturn(repositoryService);
        lenient().when(processDefinition.getId()).thenReturn("PROCESS_INSTANCE_ID");
        lenient().when(processDefinition.getKey()).thenReturn("PROCESS_DEFINITION_KEY");
        lenient().when(decisionDefinition.getId()).thenReturn("DECISION_DEFINITION_ID");
        lenient().when(decisionDefinition.getKey()).thenReturn("DECISION_DEFINITION_KEY");
        QueryMocks.mockProcessDefinitionQuery(processEngine.getRepositoryService())
                .list(List.of(processDefinition));
        QueryMocks.mockDecisionDefinitionQuery(processEngine.getRepositoryService())
                .list(List.of(decisionDefinition));
        lenient()
                .when(repositoryService.getBpmnModelInstance(anyString()))
                .thenReturn(Bpmn.readModelFromStream(getValidBpmnModelString()));
        lenient()
                .when(repositoryService.getDmnModelInstance(anyString()))
                .thenReturn(Dmn.readModelFromStream(getValidDmnModelFromString()));
    }

    private List<TransactionDefinition> getTransactionDefinitions() {

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .key("FORM_CONFIGURATION_KEY")
                        .build();

        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("TRANSACTION_DEFINITION_KEY")
                        .name("Transaction Definition 1")
                        .category("category")
                        .processDefinitionKey("PROCESS_DEFINITION_KEY")
                        .schemaKey("SCHEMA_1")
                        .defaultStatus("draft")
                        .subjectType(ProfileType.INDIVIDUAL)
                        .allowedRelatedPartyTypes(Set.copyOf(List.of(ProfileType.INDIVIDUAL)))
                        .isPublicVisible(true)
                        .formConfigurationSelectionRules(
                                List.of(
                                        FormConfigurationSelectionRule.builder()
                                                .task("task")
                                                .viewer("viewer")
                                                .context("context")
                                                .build()))
                        .build();
        transactionDefinition.addFormConfiguration(formConfiguration);

        return List.of(transactionDefinition);
    }

    private List<TransactionDefinitionSet> getTransactionDefinitionSets() {
        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .dashboardConfiguration(
                                DashboardConfiguration.builder()
                                        .dashboardLabel("label")
                                        .menuIcon("icon")
                                        .columns(
                                                List.of(
                                                        DashboardColumnConfiguration.builder()
                                                                .columnLabel("column")
                                                                .attributePath("path")
                                                                .sortable(true)
                                                                .build()))
                                        .tabs(
                                                List.of(
                                                        DashboardTabConfiguration.builder()
                                                                .tabLabel("tab")
                                                                .filter(Map.of("key", "value"))
                                                                .build()))
                                        .transactionDefinitionSet(
                                                TransactionDefinitionSet.builder()
                                                        .key("key")
                                                        .build())
                                        .build())
                        .constraints(
                                List.of(
                                        TransactionDefinitionSetDataRequirement.builder()
                                                .path("path")
                                                .contentType("contentType")
                                                .type("type")
                                                .build()))
                        .workflow("workflow")
                        .key("key")
                        .build();

        return List.of(transactionDefinitionSet);
    }

    private List<SchemaRow> getSchemas() {

        String schemaJson =
                "{\"id\": \"429d488e-d219-4cd6-b015-f71fdfeb86c7\", "
                        + "\"key\": \"314b75dc-e2e1-47b4-81ca-28eff5c8b1eb\","
                        + "\"name\": \"Example_Schema_1\", \"attributes\": [], "
                        + "\"description\": \"An example schema\\nfor testing purposes\"}";

        SchemaRow schemaRow =
                SchemaRow.builder()
                        .name("SCHEMA_1")
                        .id(UUID.fromString("30d2c43a-ef5b-11ed-a05b-0242ac120003"))
                        .schemaJson(schemaJson)
                        .build();

        return List.of(schemaRow);
    }

    private List<FormConfiguration> getFormConfigurations() {

        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("TRANSACTION_DEFINITION_KEY")
                        .build();

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinition(transactionDefinition)
                        .key("DEFAULT CONFIGURATION KEY")
                        .name("Form Configuration 1")
                        .schemaKey("Schema Key 1")
                        .configurationSchema("test schema")
                        .configuration(new HashMap<>())
                        .createdBy("test user")
                        .lastUpdatedBy("test user")
                        .createdTimestamp(OffsetDateTime.parse("2023-05-11T22:22:24Z"))
                        .lastUpdatedTimestamp(OffsetDateTime.parse("2023-05-11T22:22:24Z"))
                        .build();

        return List.of(formConfiguration);
    }

    private String getBpmnModelString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
                + " xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\""
                + " xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\""
                + " xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\""
                + " xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns:bioc=\"http://bpmn.io/schema/bpmn/biocolor/1.0\""
                + " xmlns:color=\"http://www.omg.org/spec/BPMN/non-normative/color/1.0\""
                + " xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\""
                + " id=\"PROCESS_DEFINITION_DIAGRAM_1\""
                + " targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\""
                + " exporterVersion=\"5.0.0\" modeler:executionPlatform=\"Camunda Platform\""
                + " modeler:executionPlatformVersion=\"7.15.0\">\n"
                + "  <bpmn:collaboration id=\"Collaboration_0slndn1\">\n"
                + "    <bpmn:participant id=\"Participant_07k0cyw\" name=\"Test\" processRef=\""
                + "PROCESS_DEFINITION_KEY"
                + "\" />\n"
                + "  </bpmn:collaboration>\n"
                + "  <bpmn:process id=\""
                + "PROCESS_DEFINITION_KEY"
                + "\" name=\""
                + "PROCESS_DEFINITION_KEY"
                + "\" isExecutable=\"true\">\n"
                + "    <bpmn:sequenceFlow id=\"Flow_0y7ki00\" sourceRef=\"StartEvent_1\""
                + " targetRef=\""
                + "TASK_1"
                + "\" />\n"
                + "    <bpmn:endEvent id=\"EndEvent_1\">\n"
                + "      <bpmn:extensionElements />\n"
                + "      <bpmn:incoming>Flow_1rerpnm</bpmn:incoming>\n"
                + "    </bpmn:endEvent>\n"
                + "    <bpmn:sequenceFlow id=\"Flow_0pk29ve\" sourceRef=\""
                + "TASK_1"
                + "\" targetRef=\"Activity_1hdaye6\">\n"
                + "      <bpmn:extensionElements />\n"
                + "    </bpmn:sequenceFlow>\n"
                + "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\">\n"
                + "      <bpmn:outgoing>Flow_0y7ki00</bpmn:outgoing>\n"
                + "    </bpmn:startEvent>\n"
                + "    <bpmn:userTask id=\""
                + "TASK_1"
                + "\" name=\"Task\">\n"
                + "      <bpmn:extensionElements />\n"
                + "      <bpmn:incoming>Flow_0y7ki00</bpmn:incoming>\n"
                + "      <bpmn:outgoing>Flow_0pk29ve</bpmn:outgoing>\n"
                + "    </bpmn:userTask>\n"
                + "    <bpmn:sequenceFlow id=\"Flow_1rerpnm\" sourceRef=\"Activity_1hdaye6\" "
                + "targetRef=\"EndEvent_1\" />\n"
                + "    <bpmn:serviceTask id=\"Activity_1hdaye6\" name=\"Test Delegate\" "
                + "camunda:delegateExpression=\"#{"
                + "TestDelegate"
                + "}\">\n"
                + "      <bpmn:extensionElements />\n"
                + "      <bpmn:incoming>Flow_0pk29ve</bpmn:incoming>\n"
                + "      <bpmn:outgoing>Flow_1rerpnm</bpmn:outgoing>\n"
                + "    </bpmn:serviceTask>\n"
                + "  </bpmn:process>\n"
                + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\""
                + " bpmnElement=\"Collaboration_0slndn1\">\n"
                + "      <bpmndi:BPMNShape id=\"Participant_07k0cyw_di\""
                + " bpmnElement=\"Participant_07k0cyw\" isHorizontal=\"true\">\n"
                + "        <dc:Bounds x=\"129\" y=\"80\" width=\"751\" height=\"250\" />\n"
                + "        <bpmndi:BPMNLabel />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNEdge id=\"Flow_1rerpnm_di\" bpmnElement=\"Flow_1rerpnm\">\n"
                + "        <di:waypoint x=\"730\" y=\"197\" />\n"
                + "        <di:waypoint x=\"772\" y=\"197\" />\n"
                + "      </bpmndi:BPMNEdge>\n"
                + "      <bpmndi:BPMNEdge id=\"Flow_0pk29ve_di\" bpmnElement=\"Flow_0pk29ve\""
                + " bioc:stroke=\"#1e88e5\" color:border-color=\"#1e88e5\">\n"
                + "        <di:waypoint x=\"410\" y=\"197\" />\n"
                + "        <di:waypoint x=\"630\" y=\"197\" />\n"
                + "        <bpmndi:BPMNLabel>\n"
                + "          <dc:Bounds x=\"498\" y=\"206\" width=\"50\" height=\"27\" />\n"
                + "        </bpmndi:BPMNLabel>\n"
                + "      </bpmndi:BPMNEdge>\n"
                + "      <bpmndi:BPMNEdge id=\"Flow_0y7ki00_di\" bpmnElement=\"Flow_0y7ki00\">\n"
                + "        <di:waypoint x=\"258\" y=\"197\" />\n"
                + "        <di:waypoint x=\"310\" y=\"197\" />\n"
                + "      </bpmndi:BPMNEdge>\n"
                + "      <bpmndi:BPMNShape id=\"Event_0snp829_di\" bpmnElement=\"EndEvent_1\">\n"
                + "        <dc:Bounds x=\"772\" y=\"179\" width=\"36\" height=\"36\" />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\""
                + " bpmnElement=\"StartEvent_1\">\n"
                + "        <dc:Bounds x=\"222\" y=\"179\" width=\"36\" height=\"36\" />\n"
                + "        <bpmndi:BPMNLabel>\n"
                + "          <dc:Bounds x=\"229\" y=\"222\" width=\"25\" height=\"14\" />\n"
                + "        </bpmndi:BPMNLabel>\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"Activity_1m34esl_di\" bpmnElement=\""
                + "TASK_1"
                + "\">\n"
                + "        <dc:Bounds x=\"310\" y=\"157\" width=\"100\" height=\"80\" />\n"
                + "        <bpmndi:BPMNLabel />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "      <bpmndi:BPMNShape id=\"Activity_1fygaiv_di\""
                + " bpmnElement=\"Activity_1hdaye6\">\n"
                + "        <dc:Bounds x=\"630\" y=\"157\" width=\"100\" height=\"80\" />\n"
                + "        <bpmndi:BPMNLabel />\n"
                + "      </bpmndi:BPMNShape>\n"
                + "    </bpmndi:BPMNPlane>\n"
                + "  </bpmndi:BPMNDiagram>\n"
                + "</bpmn:definitions>\n";
    }

    private String getDmnModelString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\""
                + " xmlns:dmndi=\"https://www.omg.org/spec/DMN/20191111/DMNDI/\""
                + " xmlns:dc=\"http://www.omg.org/spec/DMN/20180521/DC/\""
                + " xmlns:biodi=\"http://bpmn.io/schema/dmn/biodi/2.0\""
                + " id=\"DECISION_DEFINITION_DIAGRAM_1\" name=\"DRD\""
                + " namespace=\"http://camunda.org/schema/1.0/dmn\" exporter=\"Camunda"
                + " Modeler\" exporterVersion=\"5.0.0\">\n"
                + "  <decision id=\"decision\" name=\"Determination\">\n"
                + "    <decisionTable id=\"DecisionTable_1l6n2mr\" hitPolicy=\"RULE ORDER\">\n"
                + "      <input id=\"Input_1\" label=\"Complaint Type\" biodi:width=\"192\">\n"
                + "        <inputExpression id=\"InputExpression_1\" typeRef=\"boolean\">\n"
                + "          <text></text>\n"
                + "        </inputExpression>\n"
                + "      </input>\n"
                + "      <output id=\"Output_1\" label=\"Complaint\" typeRef=\"string\""
                + " biodi:width=\"192\" />\n"
                + "      <rule id=\"DecisionRule_1bwplsd\">\n"
                + "        <inputEntry id=\"UnaryTests_11vxdb9\">\n"
                + "          <text>testValue1</text>\n"
                + "        </inputEntry>\n"
                + "        <outputEntry id=\"LiteralExpression_1j26rot\">\n"
                + "          <text>\"Test Result 1\"</text>\n"
                + "        </outputEntry>\n"
                + "      </rule>\n"
                + "      <rule id=\"DecisionRule_0f19a8i\">\n"
                + "        <inputEntry id=\"UnaryTests_0yiyhje\">\n"
                + "          <text>testValue5</text>\n"
                + "        </inputEntry>\n"
                + "        <outputEntry id=\"LiteralExpression_1i4iey6\">\n"
                + "          <text>\"Test Result 2\"</text>\n"
                + "        </outputEntry>\n"
                + "      </rule>\n"
                + "    </decisionTable>\n"
                + "  </decision>\n"
                + "  <dmndi:DMNDI>\n"
                + "    <dmndi:DMNDiagram>\n"
                + "      <dmndi:DMNShape dmnElementRef=\"decision\">\n"
                + "        <dc:Bounds height=\"80\" width=\"180\" x=\"160\" y=\"100\" />\n"
                + "      </dmndi:DMNShape>\n"
                + "    </dmndi:DMNDiagram>\n"
                + "  </dmndi:DMNDI>\n"
                + "</definitions>\n";
    }

    private InputStream getValidBpmnModelString() {
        return new ByteArrayInputStream(getBpmnModelString().getBytes(StandardCharsets.UTF_8));
    }

    private InputStream getValidDmnModelFromString() {
        return new ByteArrayInputStream(getDmnModelString().getBytes(StandardCharsets.UTF_8));
    }

    private static void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.deleteIfExists(file.toPath());
                    }
                }
            }
            Files.deleteIfExists(directory.toPath());
        }
    }
}
