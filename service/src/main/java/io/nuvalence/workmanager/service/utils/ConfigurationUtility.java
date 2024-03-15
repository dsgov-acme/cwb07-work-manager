package io.nuvalence.workmanager.service.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionSetMapper;
import io.nuvalence.workmanager.service.repository.FormConfigurationRepository;
import io.nuvalence.workmanager.service.repository.SchemaRepository;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionRepository;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A utility to be used for configuration.
 */
@SuppressWarnings({"ClassFanOutComplexity", "CyclomaticComplexity", "ClassDataAbstractionCoupling"})
@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ConfigurationUtility {
    public static final String SCHEMAS_DIR = "schema";
    public static final String TRANSACTIONS_DIR = "transaction";
    public static final String TRANSACTIONS_SET_DIR = "transaction-set";
    public static final String WORKFLOWS_DIR = "workflow";
    private static String exportTempDir = "/data/export_tmp";

    private final TransactionDefinitionRepository transactionDefinitionRepository;
    private final TransactionDefinitionSetRepository transactionDefinitionSetRepository;
    private final SchemaRepository schemaRepository;
    private final FormConfigurationRepository formConfigurationRepository;
    private final ProcessEngine processEngine;
    private final DynamicSchemaMapper dynamicSchemaMapper;
    private final TransactionDefinitionMapper transactionDefinitionMapper;
    private final TransactionDefinitionSetMapper transactionDefinitionSetMapper;
    private final FormConfigurationMapper formConfigurationMapper;
    private final ObjectMapper objectMapper;

    /**
     * Gets the import timestamp string.
     *
     * @return the import timestamp string.
     */
    public static String getImportTimestampString() {
        String nowString =
                new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
                        .format(Calendar.getInstance().getTime());
        return String.format("%s-%s", nowString, UUID.randomUUID());
    }

    /**
     * Converts a ZipEntry to the passed in type.
     *
     * @param yamlFile        the ZipEntry.
     * @param objectMapper ObjectMapper, used to convert.
     * @param type         the type.
     * @param <T>          the type to convert the ZipEntry to.
     * @return an object of the type.
     * @throws IOException on read/write errors
     */
    public static <T> T yamlToTypedClass(String yamlFile, ObjectMapper objectMapper, Class<T> type)
            throws IOException {
        return objectMapper.readValue(yamlFile, type);
    }

    /**
     * Converts the ZipEntry to a ByteArrayInputStream.
     *
     * @param zipIn the ZipEntry.
     * @return a ByteArrayInputStream.
     * @throws IOException on read/write errors.
     */
    public static ByteArrayInputStream readZipEntryToInputStream(ZipInputStream zipIn)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read = zipIn.read(buffer, 0, buffer.length);
        while (read >= 0) {
            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            read = zipIn.read(buffer, 0, buffer.length);
        }

        return new ByteArrayInputStream(
                stripEmptyLinesFromString(sb.toString()).getBytes(StandardCharsets.UTF_8));
    }

    private static String stripEmptyLinesFromString(String str) {
        return String.join(
                System.lineSeparator(),
                Arrays.stream(str.split("\\r?\\n|\\r"))
                        .filter(StringUtils::isNotBlank)
                        .toArray(String[]::new));
    }

    /**
     * Gets the current configuration.
     *
     * @return the current configuration.
     * @throws IOException on read/write errors.
     * @throws UnexpectedException unexpected errors.
     */
    public FileSystemResource getConfiguration() throws IOException {
        // setting up temporary file to write to
        File tempZipFile = createTempZipFile();

        try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            writeDirectoriesToZip(zipOut);
            writeSchemasToZip(zipOut);
            writeWorkflowsToZip(zipOut);
            writeTransactionFiles(zipOut);
            writeTransactionDefinitionSetFiles(zipOut);

            return new FileSystemResource(tempZipFile);
        } catch (IOException e) {
            log.error(
                    "Exception occurred in {}.getConfiguration: {}",
                    this.getClass().getSimpleName(),
                    e.getMessage());
        }

        throw new UnexpectedException(
                "An error occurred while attempting to retrieve the configuration.");
    }

    private static File createTempZipFile() throws IOException {
        Path parentDir = Paths.get(exportTempDir);
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Path tempDir = Files.createTempDirectory(Paths.get(exportTempDir), "temp");

        if (tempDir == null) {
            throw new IOException("Failed to create temp directory");
        }

        if (SystemUtils.IS_OS_UNIX) {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
            Files.setPosixFilePermissions(tempDir, permissions);
        }

        Path tempFile = Files.createTempFile(tempDir, "", ".zip");
        setFilePermissions(tempFile.toFile());
        return tempFile.toFile();
    }

    private static void setFilePermissions(File file) throws IOException {
        if (!file.setReadable(true, true)
                || !file.setWritable(true, true)
                || !file.setExecutable(true, true)) {
            throw new IOException(
                    "Failed to set permissions for the file: " + file.getAbsolutePath());
        }
    }

    private void writeDirectoriesToZip(ZipOutputStream zipOut) throws IOException {
        List<String> directories = Arrays.asList(SCHEMAS_DIR, WORKFLOWS_DIR, TRANSACTIONS_DIR);
        for (String directory : directories) {
            zipOut.putNextEntry(new ZipEntry(directory + "/"));
            zipOut.closeEntry();
        }
    }

    private void writeSchemasToZip(ZipOutputStream zipOut) {
        schemaRepository
                .findAll()
                .forEach(
                        schemaRow -> {
                            try {
                                writeObjectToYamlFile(
                                        zipOut,
                                        SCHEMAS_DIR,
                                        dynamicSchemaMapper.schemaRowToSchemaExportModel(schemaRow),
                                        schemaRow.getKey());
                            } catch (IOException e) {
                                throw new UnexpectedException(e);
                            }
                        });
    }

    private void writeWorkflowsToZip(ZipOutputStream zipOut) {
        processEngine
                .getRepositoryService()
                .createProcessDefinitionQuery()
                .latestVersion()
                .list()
                .forEach(
                        processDefinition -> {
                            BpmnModelInstance bpmnModelInstance =
                                    processEngine
                                            .getRepositoryService()
                                            .getBpmnModelInstance(processDefinition.getId());
                            String fileName =
                                    String.format(
                                            "%s/%s_%s.bpmn",
                                            WORKFLOWS_DIR,
                                            processDefinition.getKey(),
                                            UUID.randomUUID());
                            try {
                                writeStringToZip(
                                        zipOut, fileName, Bpmn.convertToString(bpmnModelInstance));
                            } catch (IOException e) {
                                throw new UnexpectedException(e);
                            }
                        });

        processEngine
                .getRepositoryService()
                .createDecisionDefinitionQuery()
                .latestVersion()
                .list()
                .forEach(
                        decisionDefinition -> {
                            DmnModelInstance dmnModelInstance =
                                    processEngine
                                            .getRepositoryService()
                                            .getDmnModelInstance(decisionDefinition.getId());
                            String fileName =
                                    String.format(
                                            "%s/%s_%s.dmn",
                                            WORKFLOWS_DIR,
                                            decisionDefinition.getKey(),
                                            UUID.randomUUID());
                            try {
                                writeStringToZip(
                                        zipOut, fileName, Dmn.convertToString(dmnModelInstance));
                            } catch (IOException e) {
                                throw new UnexpectedException(e);
                            }
                        });
    }

    private void writeTransactionFiles(ZipOutputStream zipOut) {

        transactionDefinitionRepository
                .findAll()
                .forEach(
                        transactionDefinition -> {
                            try {
                                writeObjectToYamlFile(
                                        zipOut,
                                        TRANSACTIONS_DIR,
                                        transactionDefinitionMapper
                                                .transactionDefinitionToTransactionDefinitionExportModel(
                                                        transactionDefinition),
                                        transactionDefinition.getKey());
                            } catch (IOException e) {
                                throw new UnexpectedException(e);
                            }

                            formConfigurationRepository
                                    .findByTransactionDefinitionKey(transactionDefinition.getKey())
                                    .forEach(
                                            formConfiguration -> {
                                                try {
                                                    writeObjectToYamlFile(
                                                            zipOut,
                                                            TRANSACTIONS_DIR,
                                                            formConfigurationMapper
                                                                    .formConfigurationToFormConfigurationExportModel(
                                                                            formConfiguration),
                                                            transactionDefinition
                                                                    .getKey()
                                                                    .concat("-form-")
                                                                    .concat(
                                                                            formConfiguration
                                                                                    .getKey()));
                                                } catch (IOException e) {
                                                    throw new UnexpectedException(e);
                                                }
                                            });
                        });
    }

    private void writeTransactionDefinitionSetFiles(ZipOutputStream zipOut) {
        transactionDefinitionSetRepository
                .findAll()
                .forEach(
                        transactionDefinitionSet -> {
                            try {
                                writeObjectToYamlFile(
                                        zipOut,
                                        TRANSACTIONS_SET_DIR,
                                        transactionDefinitionSetMapper
                                                .transactionDefinitionSetToExportModel(
                                                        transactionDefinitionSet),
                                        transactionDefinitionSet.getKey());
                            } catch (IOException e) {
                                throw new UnexpectedException(e);
                            }
                        });
    }

    private void writeObjectToYamlFile(
            ZipOutputStream zipOut, String directoryName, Object object, String schemaKey)
            throws IOException {
        zipOut.putNextEntry(new ZipEntry(String.format("%s/%s.yaml", directoryName, schemaKey)));

        ObjectMapper yamlMapper =
                new ObjectMapper(
                        new YAMLFactory()
                                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
                                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                .enable(YAMLGenerator.Feature.INDENT_ARRAYS));

        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        String yamlString = yamlMapper.writeValueAsString(object);
        zipOut.write(yamlString.getBytes(StandardCharsets.UTF_8));
    }

    static String convertYamlToJson(String yaml) throws JsonProcessingException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    private void writeStringToZip(ZipOutputStream zipOut, String filePath, String content)
            throws IOException {
        zipOut.putNextEntry(new ZipEntry(filePath));
        zipOut.write(content.getBytes(StandardCharsets.UTF_8));
        zipOut.closeEntry();
    }
}
